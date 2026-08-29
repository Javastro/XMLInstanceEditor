package org.javastro.xmlinstanceeditor.api.xmlimport;

import java.util.List;
import java.util.Map;

public record XmlImportResult(
    String rootName,
    String rootTypeName,
    String rootTypeNamespaceUri,
    Map<String, Object> values,
    Map<String, Integer> repeatCounts,
    List<UnknownXmlContent> unknownContent
) {
    public XmlImportResult {
        values = values == null ? Map.of() : Map.copyOf(values);
        repeatCounts = repeatCounts == null ? Map.of() : Map.copyOf(repeatCounts);
        unknownContent = unknownContent == null ? List.of() : List.copyOf(unknownContent);
    }
}
