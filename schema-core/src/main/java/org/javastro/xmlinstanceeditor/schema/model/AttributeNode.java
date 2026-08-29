package org.javastro.xmlinstanceeditor.schema.model;

public record AttributeNode(
    String name,
    SimpleType type,
    boolean required,
    String defaultValue,
    String fixedValue
) {
}
