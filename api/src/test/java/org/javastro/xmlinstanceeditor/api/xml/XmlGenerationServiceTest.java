package org.javastro.xmlinstanceeditor.api.xml;

import org.javastro.xmlinstanceeditor.xml.XmlElement;
import org.javastro.xmlinstanceeditor.xml.XmlRawFragment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlGenerationServiceTest {
    @Test
    void generatesXmlFromElementTree() {
        var service = new XmlGenerationService();
        var xml = service.generate(new XmlElement(
            "root",
            null,
            java.util.Map.of(),
            java.util.List.of(new XmlRawFragment("<child/>"))
        ));

        assertTrue(xml.contains("<root"));
        assertTrue(xml.contains("<child/>"));
    }
}
