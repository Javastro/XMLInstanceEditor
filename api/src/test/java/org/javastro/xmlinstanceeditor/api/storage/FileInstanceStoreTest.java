package org.javastro.xmlinstanceeditor.api.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileInstanceStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsInstances() {
        var store = new FileInstanceStore(new ObjectMapper(), tempDir);

        var saved = store.save(new EditedInstance<>(
            "abc",
            "schema-1",
            "<root/>",
            Map.of("field", "value"),
            "2026-08-29T09:00:00Z",
            null
        ));

        assertTrue(store.findById("abc").isPresent());
        assertEquals("schema-1", saved.schemaId());
        assertEquals("value", store.findById("abc").orElseThrow().state().get("field"));
        assertTrue(store.findAll().size() == 1);
    }
}
