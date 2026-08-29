package org.javastro.xmlinstanceeditor.api.xml;

import org.javastro.xmlinstanceeditor.xml.XmlElement;
import org.javastro.xmlinstanceeditor.xml.XmlSerializer;

import java.io.OutputStream;

public final class XmlGenerationService {
    private final XmlSerializer serializer = new XmlSerializer();

    public String generate(XmlElement root) {
        return serializer.toXml(root);
    }

    public void write(XmlElement root, OutputStream outputStream) {
        serializer.write(root, outputStream);
    }
}
