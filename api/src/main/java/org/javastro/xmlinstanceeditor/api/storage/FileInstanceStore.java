package org.javastro.xmlinstanceeditor.api.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FileInstanceStore implements InstanceStore<Map<String, Object>> {
    private static final TypeReference<EditedInstance<Map<String, Object>>> INSTANCE_TYPE =
        new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path rootDir;

    public FileInstanceStore(ObjectMapper objectMapper, Path rootDir) {
        this.objectMapper = objectMapper;
        this.rootDir = rootDir;
    }

    @Override
    public EditedInstance<Map<String, Object>> save(EditedInstance<Map<String, Object>> instance) {
        EditedInstance<Map<String, Object>> normalized = new EditedInstance<>(
            instance.id(),
            instance.schemaId(),
            instance.xml(),
            instance.state(),
            instance.createdAt(),
            Instant.now().toString()
        );
        write(normalized);
        return normalized;
    }

    @Override
    public Optional<EditedInstance<Map<String, Object>>> findById(String id) {
        Path file = fileFor(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(Files.readString(file), INSTANCE_TYPE));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read instance " + id, ex);
        }
    }

    @Override
    public List<EditedInstance<Map<String, Object>>> findAll() {
        try {
            if (!Files.exists(rootDir)) {
                return List.of();
            }
            List<EditedInstance<Map<String, Object>>> records = new ArrayList<>();
            try (var stream = Files.list(rootDir)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> {
                        try {
                            records.add(objectMapper.readValue(Files.readString(path), INSTANCE_TYPE));
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to read instance file " + path, ex);
                        }
                    });
            }
            return List.copyOf(records);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list instances", ex);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            return Files.deleteIfExists(fileFor(id));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete instance " + id, ex);
        }
    }

    private void write(EditedInstance<Map<String, Object>> instance) {
        try {
            Files.createDirectories(rootDir);
            Files.writeString(fileFor(instance.id()), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store instance " + instance.id(), ex);
        }
    }

    private Path fileFor(String id) {
        return rootDir.resolve(id + ".json");
    }
}
