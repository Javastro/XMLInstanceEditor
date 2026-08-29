package org.javastro.xmlinstanceeditor.backend.validation;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class RegistryInterfaceImportResourceTest {
    private static final String REGISTRY_INTERFACE_NS = "http://www.ivoa.net/xml/RegistryInterface/v1.0";

    @Test
    void importsVoResourceXmlUsingPreloadedRegistrySchema() throws Exception {
        var fetchResponse = given()
            .contentType(ContentType.JSON)
            .body(Map.of("namespaceUri", REGISTRY_INTERFACE_NS))
            .when()
            .post("/api/schema/preloaded/fetch")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

        String xsd = fetchResponse.getString("xsd");
        String systemId = fetchResponse.getString("systemId");
        String xml = readResource("/VOResource.xml");
        Map<String, Object> request = new HashMap<>();
        request.put("xsd", xsd);
        request.put("systemId", systemId);
        request.put("xml", xml);
        request.put("rootName", "");
        request.put("rootTypeName", "");
        request.put("rootTypeNamespaceUri", "");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/instance/import")
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("rootName", equalTo("VOResources"))
            .body("values", not(nullValue()));
    }

    private String readResource(String resourcePath) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
