package org.javastro.xmlinstanceeditor.api.xmlimport;

import org.javastro.xmlinstanceeditor.schema.translate.XsomSchemaTranslator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlInstanceImportServiceTest {
    @Test
    void importsMappedValuesAndRepeatCounts() {
        var xsd = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="root">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="item" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                  <xs:attribute name="id" type="xs:string"/>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;
        var xml = "<root id=\"a1\"><item>one</item><item>two</item></root>";
        var schema = new XsomSchemaTranslator().translate(
            new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)),
            null
        );

        var result = new XmlInstanceImportService().importInstance(schema, xml, null, null, null);

        assertEquals("root", result.rootName());
        assertEquals("a1", result.values().get("root.@id"));
        assertEquals("one", result.values().get("root.sequence.item[0]"));
        assertEquals("two", result.values().get("root.sequence.item[1]"));
        assertEquals(2, result.repeatCounts().get("root.sequence.item.0"));
        assertTrue(result.unknownContent().isEmpty());
    }

    @Test
    void importsSyntheticRootTypeAndCapturesUnknownFragments() {
        var xsd = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:complexType name="EnvelopeType">
                <xs:sequence>
                  <xs:element name="name" type="xs:string"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """;
        var xml = """
            <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="EnvelopeType">
              <name>Alpha</name>
              <extra>ignored</extra>
            </doc>
            """;
        var schema = new XsomSchemaTranslator().translate(
            new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)),
            null
        );

        var result = new XmlInstanceImportService().importInstance(schema, xml, "doc", null, null);

        assertEquals("doc", result.rootName());
        assertEquals("EnvelopeType", result.rootTypeName());
        assertEquals("Alpha", result.values().get("doc.sequence.name"));
        assertFalse(result.unknownContent().isEmpty());
        assertEquals("doc.sequence.extra", result.unknownContent().get(0).path());
    }
}
