package org.javastro.xmlinstanceeditor.backend.validation;

public record XmlImportRequest(
    String xsd,
    String systemId,
    String xml,
    String rootName,
    String rootTypeName,
    String rootTypeNamespaceUri
) {
}
