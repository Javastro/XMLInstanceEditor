package org.javastro.xmlinstanceeditor.backend.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.javastro.ivoa.schema.SchemaMap;
import org.javastro.ivoa.schema.XMLValidator;
import org.jboss.logging.Logger;
import org.javastro.xmlinstanceeditor.api.validation.ValidationMessage;
import org.javastro.xmlinstanceeditor.api.validation.ValidationResult;
import org.javastro.xmlinstanceeditor.api.validation.XsdValidationService;
import org.javastro.xmlinstanceeditor.api.xmlimport.XmlImportResult;
import org.javastro.xmlinstanceeditor.api.xmlimport.XmlInstanceImportService;
import org.javastro.xmlinstanceeditor.schema.translate.XsomSchemaTranslator;
import org.xmlresolver.XMLResolver;
import org.xml.sax.EntityResolver;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Path("/instance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ValidationResource {
    private static final Logger LOG = Logger.getLogger(ValidationResource.class);
    private final XsdValidationService validationService = new XsdValidationService();
    private final XsomSchemaTranslator translator = new XsomSchemaTranslator();
    private final XmlInstanceImportService importService = new XmlInstanceImportService();
    private final XMLResolver xmlResolver = XMLValidator.makeXMLResolver();
    private final EntityResolver entityResolver = xmlResolver.getEntityResolver();
    private final LSResourceResolver lsResourceResolver = xmlResolver.getLSResourceResolver();

    @POST
    @Path("/validate")
    public ValidationResult validate(XmlValidationRequest request) {
        if (request == null || request.xsd() == null || request.xml() == null) {
            throw new IllegalArgumentException("xsd and xml must not be null");
        }

        try (var xsd = new ByteArrayInputStream(request.xsd().getBytes(StandardCharsets.UTF_8))) {
            return validationService.validate(xsd, request.xml(), null, lsResourceResolver);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to validate XML", ex);
        }
    }

    @POST
    @Path("/import")
    public XmlImportResult importXml(XmlImportRequest request) {
        if (request == null || request.xsd() == null || request.xsd().isBlank() || request.xml() == null || request.xml().isBlank()) {
            throw new IllegalArgumentException("xsd and xml must not be blank");
        }

        try (
            var xsdForValidation = new ByteArrayInputStream(request.xsd().getBytes(StandardCharsets.UTF_8));
            var xsdForTranslation = new ByteArrayInputStream(request.xsd().getBytes(StandardCharsets.UTF_8))
        ) {
            var schema = translator.translate(xsdForTranslation, request.systemId(), entityResolver);
            if (!schema.roots().isEmpty()) {
                boolean preloadedRegistrySchema = isRegistryPreloadedSchema(request.systemId());
                var validation = preloadedRegistrySchema
                    ? validationService.validate(SchemaMap.getAllSchemaAsSources(), request.xml(), lsResourceResolver)
                    : validationService.validate(xsdForValidation, request.xml(), request.systemId(), lsResourceResolver);
                if (!validation.valid()) {
                    String messages = validation.messages().stream()
                        .map(message -> message.severity() + ":" + message.message())
                        .collect(java.util.stream.Collectors.joining(" | "));
                    if (preloadedRegistrySchema) {
                        LOG.warnf("Continuing import for preloaded registry schema despite validation issues: %s", messages);
                    } else {
                        LOG.infof("XML import validation failed: messages=%s", messages);
                        throw new BadRequestException(formatValidationErrors(validation.messages()));
                    }
                }
            }
            return importService.importInstance(
                schema,
                request.xml(),
                request.rootName(),
                request.rootTypeName(),
                request.rootTypeNamespaceUri()
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to import XML instance", ex);
        }
    }

    private String formatValidationErrors(java.util.List<ValidationMessage> messages) {
        return messages.stream()
            .map(message -> message.severity() + ": " + message.message())
            .collect(java.util.stream.Collectors.joining("; "));
    }

    private boolean isRegistryPreloadedSchema(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return false;
        }
        return Arrays.stream(SchemaMap.getRegistrySchemaAsSources())
            .map(javax.xml.transform.stream.StreamSource::getSystemId)
            .anyMatch(systemId::equals);
    }
}
