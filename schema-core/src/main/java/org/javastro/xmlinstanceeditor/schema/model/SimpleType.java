package org.javastro.xmlinstanceeditor.schema.model;

public record SimpleType(String baseType, FacetConstraints constraints) implements TypeDefinition {
    public SimpleType {
        constraints = constraints == null ? FacetConstraints.empty() : constraints;
    }

    @Override
    public boolean isComplex() {
        return false;
    }
}
