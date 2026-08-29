package org.javastro.xmlinstanceeditor.schema.translate;

import org.javastro.xmlinstanceeditor.schema.model.SchemaDocument;

import java.io.InputStream;

public interface SchemaTranslator {
    SchemaDocument translate(InputStream xsdStream, String systemId);
}
