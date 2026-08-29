package org.javastro.xmlinstanceeditor.backend.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationResourceTest {
    @Test
    void importsXmlWithSyntheticRootWithoutPreValidationFailure() {
        var resource = new ValidationResource();
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
            </doc>
            """;

        var result = resource.importXml(new XmlImportRequest(xsd, null, xml, "doc", "EnvelopeType", null));

        assertEquals("doc", result.rootName());
        assertEquals("EnvelopeType", result.rootTypeName());
        assertEquals("Alpha", result.values().get("doc.sequence.name"));
    }
}
