package org.javastro.xmlinstanceeditor.backend.schema;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.javastro.ivoa.schema.SchemaMap;
import org.javastro.ivoa.schema.XMLValidator;
import org.jboss.logging.Logger;
import org.javastro.xmlinstanceeditor.schema.model.SchemaDocument;
import org.javastro.xmlinstanceeditor.schema.translate.XsomSchemaTranslator;
import org.xmlresolver.XMLResolver;
import org.xml.sax.EntityResolver;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/schema")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SchemaResource {
    private static final Logger LOG = Logger.getLogger(SchemaResource.class);
    private final XsomSchemaTranslator translator = new XsomSchemaTranslator();
    private final XMLResolver xmlResolver = XMLValidator.makeXMLResolver();
    private final EntityResolver entityResolver = xmlResolver.getEntityResolver();

    /** Namespace-URI to classpath-path entries that appear in getRegistrySchemaAsSources(). Computed lazily. */
    private volatile List<PreloadedSchemaInfo> cachedRegistrySchemas;

    @GET
    @Path("/preloaded")
    public List<PreloadedSchemaInfo> listPreloaded() {
        if (cachedRegistrySchemas == null) {
            cachedRegistrySchemas = buildRegistrySchemaList();
        }
        return cachedRegistrySchemas;
    }

    @POST
    @Path("/preloaded/fetch")
    public PreloadedSchemaFetchResponse fetchPreloaded(PreloadedSchemaFetchRequest request) {
        if (request == null || request.namespaceUri() == null || request.namespaceUri().isBlank()) {
            throw new IllegalArgumentException("namespaceUri must not be blank");
        }
        String systemId = SchemaMap.getSchemaURL(request.namespaceUri()) == null
            ? null
            : SchemaMap.getSchemaURL(request.namespaceUri()).toExternalForm();
        InputStream stream = SchemaMap.getSchemaAsStream(request.namespaceUri());
        if (stream == null) {
            throw new NotFoundException("Preloaded schema not found for namespace: " + request.namespaceUri());
        }
        try (stream) {
            String xsd = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new PreloadedSchemaFetchResponse(xsd, systemId);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read preloaded schema", ex);
        }
    }

    private List<PreloadedSchemaInfo> buildRegistrySchemaList() {
        // Derive the set of classpath paths used by getRegistrySchemaAsSources()
        Set<String> registryPaths = Arrays.stream(SchemaMap.getRegistrySchemaAsSources())
            .map(StreamSource::getSystemId)
            .map(id -> {
                int bang = id.lastIndexOf('!');
                return bang >= 0 ? id.substring(bang + 1) : id;
            })
            .collect(Collectors.toSet());

        // Match against ALL map (namespace URI -> classpath path) and keep only registry entries
        return SchemaMap.ALL.entrySet().stream()
            .filter(e -> registryPaths.contains(e.getValue()))
            .map(e -> new PreloadedSchemaInfo(
                e.getKey(),
                labelFromPath(e.getValue())
            ))
            .sorted(Comparator.comparing(PreloadedSchemaInfo::label))
            .toList();
    }

    private static String labelFromPath(String path) {
        // /schema/VOTable-1.4.xsd -> VOTable-1.4
        String filename = path.replaceAll(".*/", "");
        return filename.endsWith(".xsd") ? filename.substring(0, filename.length() - 4) : filename;
    }

    @POST
    @Path("/parse")
    public SchemaDocument parse(SchemaParseRequest request) {
        if (request == null || request.xsd() == null || request.xsd().isBlank()) {
            LOG.warn("Schema parse request rejected: empty XSD payload");
            throw new IllegalArgumentException("xsd must not be blank");
        }

        try (var input = new ByteArrayInputStream(request.xsd().getBytes(StandardCharsets.UTF_8))) {
            LOG.infof("Parsing schema request: systemId=%s, xsdChars=%d",
                request.systemId(),
                request.xsd().length());
            SchemaDocument parsed = translator.translate(input, request.systemId(), entityResolver);
            LOG.infof("Schema parsed successfully: roots=%d, selectableTypes=%d, targetNamespace=%s, source=%s",
                parsed.roots().size(),
                parsed.availableRootTypes().size(),
                parsed.targetNamespace(),
                parsed.sourceSystemId());
            return parsed;
        } catch (Exception ex) {
            LOG.error("Schema parse failed", ex);
            throw new IllegalStateException("Failed to parse schema", ex);
        }
    }
}
