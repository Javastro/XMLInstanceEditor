package org.javastro.xmlinstanceeditor.xml;

import java.util.List;
import java.util.Map;

public record XmlElement(
    String name,
    String namespaceUri,
    Map<String, String> attributes,
    List<XmlNode> children
) implements XmlNode {
    public XmlElement {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
