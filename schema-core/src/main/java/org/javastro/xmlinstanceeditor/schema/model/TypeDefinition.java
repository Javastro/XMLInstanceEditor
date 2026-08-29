package org.javastro.xmlinstanceeditor.schema.model;

public sealed interface TypeDefinition permits ComplexType, SimpleType {
    boolean isComplex();
}
