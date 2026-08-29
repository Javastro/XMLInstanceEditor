package org.javastro.xmlinstanceeditor.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlSerializerTest {
    @Test
    void serializesSimpleTree() {
        var serializer = new XmlSerializer();
        var xml = serializer.toXml(new XmlElement(
            "root",
            null,
            java.util.Map.of("xmlns:x", "urn:test"),
            java.util.List.of(
                new XmlElement("child", null, java.util.Map.of("id", "1"), java.util.List.of(new XmlText("value")))
            )
        ));

        assertTrue(xml.contains("<root"));
        assertTrue(xml.contains("<child id=\"1\">"));
        assertTrue(xml.contains("value"));
    }

    @Test
    void embedsRawXmlFragments() {
        var serializer = new XmlSerializer();
        var xml = serializer.toXml(new XmlElement(
            "root",
            null,
            java.util.Map.of(),
            java.util.List.of(new XmlRawFragment("<child><nested/></child>"))
        ));

        assertTrue(xml.contains("<child>"));
        assertTrue(xml.contains("<nested/>"));
    }
}
