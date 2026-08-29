package org.javastro.xmlinstanceeditor.schema.model;

import java.util.List;
import java.util.Map;

public record SchemaDocument(
    String targetNamespace,
    Map<String, String> namespaces,
    List<SchemaNode> roots,
    List<SchemaTypeOption> availableRootTypes,
    String sourceSystemId
) {
    public SchemaDocument {
        namespaces = namespaces == null ? Map.of() : Map.copyOf(namespaces);
        roots = roots == null ? List.of() : List.copyOf(roots);
        availableRootTypes = availableRootTypes == null ? List.of() : List.copyOf(availableRootTypes);
    }
}
