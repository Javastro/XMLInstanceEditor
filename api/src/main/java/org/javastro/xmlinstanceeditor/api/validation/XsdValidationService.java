package org.javastro.xmlinstanceeditor.api.validation;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class XsdValidationService {
    public ValidationResult validate(InputStream xsd, String xml) {
        return validate(xsd, xml, null, null);
    }

    public ValidationResult validate(InputStream xsd, String xml, String systemId, LSResourceResolver resourceResolver) {
        var source = new StreamSource(xsd);
        source.setSystemId(systemId);
        return validate(new StreamSource[] { source }, xml, resourceResolver);
    }

    public ValidationResult validate(StreamSource[] schemaSources, String xml, LSResourceResolver resourceResolver) {
        try {
            var messages = new ArrayList<ValidationMessage>();
            var invalid = new boolean[] { false };

            var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (resourceResolver != null) {
                schemaFactory.setResourceResolver(resourceResolver);
            }
            var schema = schemaFactory.newSchema(schemaSources);
            var validator = schema.newValidator();
            if (resourceResolver != null) {
                validator.setResourceResolver(resourceResolver);
            }
            validator.setErrorHandler(new CollectingErrorHandler(messages, invalid));
            validator.validate(new StreamSource(new StringReader(xml)));

            return new ValidationResult(!invalid[0], messages);
        } catch (SAXException ex) {
            return new ValidationResult(false, List.of(new ValidationMessage(ValidationSeverity.FATAL, ex.getMessage(), -1, -1)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to validate XML", ex);
        }
    }

    private static final class CollectingErrorHandler implements ErrorHandler {
        private final List<ValidationMessage> messages;
        private final boolean[] invalid;

        private CollectingErrorHandler(List<ValidationMessage> messages, boolean[] invalid) {
            this.messages = messages;
            this.invalid = invalid;
        }

        @Override
        public void warning(SAXParseException exception) {
            messages.add(toMessage(ValidationSeverity.WARNING, exception));
        }

        @Override
        public void error(SAXParseException exception) {
            messages.add(toMessage(ValidationSeverity.ERROR, exception));
            invalid[0] = true;
        }

        @Override
        public void fatalError(SAXParseException exception) {
            messages.add(toMessage(ValidationSeverity.FATAL, exception));
            invalid[0] = true;
        }

        private ValidationMessage toMessage(ValidationSeverity severity, SAXParseException exception) {
            return new ValidationMessage(severity, exception.getMessage(), exception.getLineNumber(), exception.getColumnNumber());
        }
    }
}
