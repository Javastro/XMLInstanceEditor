package org.javastro.xmlinstanceeditor.schema.translate;

import org.javastro.xmlinstanceeditor.schema.model.Compositor;
import org.javastro.xmlinstanceeditor.schema.model.ComplexType;
import org.javastro.xmlinstanceeditor.schema.model.NodeKind;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XsomSchemaTranslatorTest {
    @Test
    void translatesSequenceAttributesAndFacets() {
        var translator = new XsomSchemaTranslator();
        var xsd = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:test" elementFormDefault="qualified">
              <xs:element name="root">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="child" type="xs:string"/>
                    <xs:element name="status">
                      <xs:simpleType>
                        <xs:restriction base="xs:string">
                          <xs:enumeration value="A"/>
                          <xs:enumeration value="B"/>
                        </xs:restriction>
                      </xs:simpleType>
                    </xs:element>
                  </xs:sequence>
                  <xs:attribute name="code" type="xs:int" use="required"/>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

        var document = translator.translate(new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)), "memory:xsd");

        assertEquals("urn:test", document.targetNamespace());
        assertEquals(1, document.roots().size());
        assertEquals(0, document.availableRootTypes().size());

        var root = document.roots().get(0);
        assertEquals("root", root.name());
        assertEquals(NodeKind.ELEMENT, root.kind());

        var rootType = (ComplexType) root.typeDefinition();
        assertEquals(1, rootType.attributes().size());
        var group = rootType.children().get(0);
        assertEquals(NodeKind.GROUP, group.kind());
        assertEquals(Compositor.SEQUENCE, ((ComplexType) group.typeDefinition()).compositor());

        var groupType = (ComplexType) group.typeDefinition();
        assertEquals(2, groupType.children().size());
        assertFalse(groupType.recursive());
        var simpleType = (org.javastro.xmlinstanceeditor.schema.model.SimpleType) groupType.children().get(1).typeDefinition();
        assertEquals(2, simpleType.constraints().enumeration().size());
    }

    @Test
    void exposesNamedTypesWhenNoGlobalElementsExist() {
        var translator = new XsomSchemaTranslator();
        var xsd = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:types-only">
              <xs:complexType name="EnvelopeType">
                <xs:sequence>
                  <xs:element name="message" type="xs:string"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """;

        var document = translator.translate(new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)), "memory:types-only");
        assertEquals(0, document.roots().size());
        assertEquals(1, document.availableRootTypes().size());
        assertEquals("EnvelopeType", document.availableRootTypes().get(0).name());
    }
}
