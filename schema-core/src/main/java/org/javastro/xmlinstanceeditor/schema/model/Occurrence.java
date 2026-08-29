package org.javastro.xmlinstanceeditor.schema.model;

public record Occurrence(int minOccurs, Integer maxOccurs) {
    public static Occurrence required() {
        return new Occurrence(1, 1);
    }

    public static Occurrence optional() {
        return new Occurrence(0, 1);
    }

    public static Occurrence unbounded(int minOccurs) {
        return new Occurrence(minOccurs, null);
    }

    public Occurrence {
        if (minOccurs < 0) {
            throw new IllegalArgumentException("minOccurs must be >= 0");
        }
        if (maxOccurs != null && maxOccurs < minOccurs) {
            throw new IllegalArgumentException("maxOccurs must be >= minOccurs");
        }
    }

    public boolean isOptional() {
        return minOccurs == 0;
    }

    public boolean isRepeatable() {
        return maxOccurs == null || maxOccurs > 1 || minOccurs > 1;
    }
}
