package org.javastro.xmlinstanceeditor.schema.model;

public record SchemaTypeOption(
    String name,
    String namespaceUri,
    TypeDefinition typeDefinition
) {
    public SchemaTypeOption {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (typeDefinition == null) {
            throw new IllegalArgumentException("typeDefinition must not be null");
        }
    }
}
