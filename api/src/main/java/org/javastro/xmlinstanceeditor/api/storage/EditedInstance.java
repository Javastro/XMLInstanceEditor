package org.javastro.xmlinstanceeditor.api.storage;

public record EditedInstance<TState>(
    String id,
    String schemaId,
    String xml,
    TState state,
    String createdAt,
    String updatedAt
) {
    public EditedInstance {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (schemaId == null || schemaId.isBlank()) {
            throw new IllegalArgumentException("schemaId must not be blank");
        }
        if (xml == null) {
            throw new IllegalArgumentException("xml must not be null");
        }
        if (createdAt == null || createdAt.isBlank()) {
            createdAt = java.time.Instant.now().toString();
        }
        if (updatedAt == null || updatedAt.isBlank()) {
            updatedAt = createdAt;
        }
    }
}
