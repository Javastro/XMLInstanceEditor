package org.javastro.xmlinstanceeditor.api.validation;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XsdValidationServiceTest {
    @Test
    void validatesXmlAgainstSchema() {
        var service = new XsdValidationService();
        var xsd = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="root">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="child" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """.getBytes(StandardCharsets.UTF_8);

        var valid = service.validate(new java.io.ByteArrayInputStream(xsd), "<root><child>ok</child></root>");
        var invalid = service.validate(new java.io.ByteArrayInputStream(xsd), "<root/>");

        assertTrue(valid.valid());
        assertFalse(invalid.valid());
    }
}
