package org.javastro.xmlinstanceeditor.xml;

public record XmlRawFragment(String xml) implements XmlNode {
    public XmlRawFragment {
        xml = xml == null ? "" : xml;
    }
}
