package org.javastro.xmlinstanceeditor.schema.model;

import java.util.List;
import java.util.Optional;

public record ComplexType(
    Compositor compositor,
    List<SchemaNode> children,
    List<AttributeNode> attributes,
    Optional<SimpleType> textContent,
    boolean recursive
) implements TypeDefinition {
    public ComplexType {
        children = children == null ? List.of() : List.copyOf(children);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        textContent = textContent == null ? Optional.empty() : textContent;
        if (compositor == null) {
            throw new IllegalArgumentException("compositor must not be null");
        }
    }

    @Override
    public boolean isComplex() {
        return true;
    }
}
