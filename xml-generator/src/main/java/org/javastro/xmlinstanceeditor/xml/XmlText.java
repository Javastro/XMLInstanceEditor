package org.javastro.xmlinstanceeditor.xml;

public record XmlText(String value) implements XmlNode {
    public XmlText {
        value = value == null ? "" : value;
    }
}
