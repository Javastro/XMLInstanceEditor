package org.javastro.xmlinstanceeditor.schema.model;

import java.util.List;

public record FacetConstraints(
    List<String> enumeration,
    String pattern,
    Integer minLength,
    Integer maxLength
) {
    public static FacetConstraints empty() {
        return new FacetConstraints(List.of(), null, null, null);
    }

    public FacetConstraints {
        enumeration = enumeration == null ? List.of() : List.copyOf(enumeration);
    }

    public boolean isRestricted() {
        return !enumeration.isEmpty()
            || pattern != null
            || minLength != null
            || maxLength != null;
    }
}
