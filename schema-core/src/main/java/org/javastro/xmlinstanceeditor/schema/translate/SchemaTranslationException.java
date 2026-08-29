package org.javastro.xmlinstanceeditor.schema.translate;

public class SchemaTranslationException extends RuntimeException {
    public SchemaTranslationException(String message) {
        super(message);
    }

    public SchemaTranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
