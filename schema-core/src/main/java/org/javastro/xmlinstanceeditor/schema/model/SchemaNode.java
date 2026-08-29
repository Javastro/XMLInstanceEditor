package org.javastro.xmlinstanceeditor.schema.model;

import java.util.List;

public record SchemaNode(
    String name,
    NodeKind kind,
    Occurrence occurrence,
    boolean nillable,
    String defaultValue,
    String fixedValue,
    String namespaceUri,
    boolean abstractElement,
    List<SchemaNode> substitutionOptions,
    TypeDefinition typeDefinition
) {
    public SchemaNode {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence must not be null");
        }
        substitutionOptions = substitutionOptions == null ? List.of() : List.copyOf(substitutionOptions);
        if (typeDefinition == null) {
            throw new IllegalArgumentException("typeDefinition must not be null");
        }
    }
}
