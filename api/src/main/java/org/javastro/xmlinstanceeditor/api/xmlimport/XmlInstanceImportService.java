package org.javastro.xmlinstanceeditor.api.xmlimport;

import org.javastro.xmlinstanceeditor.schema.model.AttributeNode;
import org.javastro.xmlinstanceeditor.schema.model.ComplexType;
import org.javastro.xmlinstanceeditor.schema.model.NodeKind;
import org.javastro.xmlinstanceeditor.schema.model.Occurrence;
import org.javastro.xmlinstanceeditor.schema.model.SchemaDocument;
import org.javastro.xmlinstanceeditor.schema.model.SchemaNode;
import org.javastro.xmlinstanceeditor.schema.model.SchemaTypeOption;
import org.javastro.xmlinstanceeditor.schema.model.TypeDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class XmlInstanceImportService {
    public XmlImportResult importInstance(
        SchemaDocument schema,
        String xml,
        String requestedRootName,
        String requestedRootTypeName,
        String requestedRootTypeNamespaceUri
    ) {
        if (schema == null) {
            throw new IllegalArgumentException("schema must not be null");
        }
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("xml must not be blank");
        }

        Document document = parseXml(xml);
        Element rootElement = document.getDocumentElement();
        if (rootElement == null) {
            throw new IllegalArgumentException("xml has no document element");
        }

        RootSelection selection = selectRoot(
            schema,
            rootElement,
            requestedRootName,
            requestedRootTypeName,
            requestedRootTypeNamespaceUri
        );
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, Integer> repeatCounts = new LinkedHashMap<>();
        List<UnknownXmlContent> unknown = new ArrayList<>();

        mapElement(selection.rootNode(), rootElement, selection.rootName(), values, repeatCounts, unknown);
        return new XmlImportResult(
            selection.rootName(),
            selection.rootTypeName(),
            selection.rootTypeNamespaceUri(),
            values,
            repeatCounts,
            unknown
        );
    }

    private RootSelection selectRoot(
        SchemaDocument schema,
        Element rootElement,
        String requestedRootName,
        String requestedRootTypeName,
        String requestedRootTypeNamespaceUri
    ) {
        if (!schema.roots().isEmpty()) {
            SchemaNode root = schema.roots().stream()
                .filter(candidate -> matchesElement(candidate, rootElement))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "XML root element does not match any schema root declaration: " + localNameOf(rootElement)
                ));
            return new RootSelection(root, root.name(), null, null);
        }

        String resolvedRootName = isBlank(requestedRootName) ? localNameOf(rootElement) : requestedRootName.trim();
        SchemaTypeOption typeOption = resolveRootTypeOption(
            schema.availableRootTypes(),
            rootElement,
            requestedRootTypeName,
            requestedRootTypeNamespaceUri
        );
        SchemaNode syntheticRoot = new SchemaNode(
            resolvedRootName,
            NodeKind.ELEMENT,
            Occurrence.required(),
            false,
            null,
            null,
            typeOption.namespaceUri(),
            false,
            List.of(),
            typeOption.typeDefinition()
        );
        return new RootSelection(
            syntheticRoot,
            resolvedRootName,
            localName(typeOption.name()),
            typeOption.namespaceUri()
        );
    }

    private SchemaTypeOption resolveRootTypeOption(
        List<SchemaTypeOption> options,
        Element rootElement,
        String requestedRootTypeName,
        String requestedRootTypeNamespaceUri
    ) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("schema has no root elements or selectable root types");
        }

        if (!isBlank(requestedRootTypeName)) {
            String requestedName = localName(requestedRootTypeName.trim());
            String requestedNs = blankToNull(requestedRootTypeNamespaceUri);
            return options.stream()
                .filter(option -> localName(option.name()).equals(requestedName))
                .filter(option -> requestedNs == null || Objects.equals(blankToNull(option.namespaceUri()), requestedNs))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selected root type not found in schema: " + requestedRootTypeName));
        }

        ParsedQualifiedName xsiType = parseXsiType(rootElement);
        if (xsiType != null) {
            return options.stream()
                .filter(option -> localName(option.name()).equals(xsiType.localName()))
                .filter(option -> xsiType.namespaceUri() == null || Objects.equals(blankToNull(option.namespaceUri()), xsiType.namespaceUri()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("xsi:type not found in schema options: " + xsiType.localName()));
        }

        if (options.size() == 1) {
            return options.get(0);
        }
        throw new IllegalArgumentException("Root type must be selected when schema has no global root elements");
    }

    private ParsedQualifiedName parseXsiType(Element rootElement) {
        String xsiType = rootElement.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
        if (isBlank(xsiType)) {
            return null;
        }
        String raw = xsiType.trim();
        int separator = raw.indexOf(':');
        if (separator < 0) {
            return new ParsedQualifiedName(raw, rootElement.lookupNamespaceURI(null));
        }
        String prefix = raw.substring(0, separator);
        String local = raw.substring(separator + 1);
        return new ParsedQualifiedName(local, rootElement.lookupNamespaceURI(prefix));
    }

    private void mapElement(
        SchemaNode node,
        Element xmlElement,
        String path,
        Map<String, Object> values,
        Map<String, Integer> repeatCounts,
        List<UnknownXmlContent> unknownContent
    ) {
        TypeDefinition typeDefinition = node.typeDefinition();
        if (!typeDefinition.isComplex()) {
            String text = nonBlankText(xmlElement);
            if (text != null) {
                values.put(path, text);
            }
            return;
        }

        ComplexType complex = (ComplexType) typeDefinition;
        mapAttributes(xmlElement, path, complex.attributes(), values);
        mapComplexChildren(xmlElement, path, complex.children(), values, repeatCounts, unknownContent);
    }

    private void mapAttributes(
        Element xmlElement,
        String path,
        List<AttributeNode> attributes,
        Map<String, Object> values
    ) {
        for (AttributeNode attribute : attributes) {
            if ("*".equals(attribute.name())) {
                continue;
            }
            if (xmlElement.hasAttribute(attribute.name())) {
                values.put(path + ".@" + attribute.name(), xmlElement.getAttribute(attribute.name()));
            }
        }
    }

    private Set<Element> mapComplexChildren(
        Element parentElement,
        String path,
        List<SchemaNode> schemaChildren,
        Map<String, Object> values,
        Map<String, Integer> repeatCounts,
        List<UnknownXmlContent> unknownContent
    ) {
        Set<Element> consumedElements = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (int childIndex = 0; childIndex < schemaChildren.size(); childIndex++) {
            SchemaNode child = schemaChildren.get(childIndex);
            if (child.kind() == NodeKind.GROUP) {
                mapGroupNode(child, parentElement, path + "." + child.name(), values, repeatCounts, unknownContent, consumedElements);
                continue;
            }
            if (child.kind() != NodeKind.ELEMENT) {
                continue;
            }

            List<Element> matches = findMatchingChildren(parentElement, child);
            if (child.occurrence().isRepeatable()) {
                repeatCounts.put(path + "." + child.name() + "." + childIndex, matches.size());
            }
            if (matches.isEmpty()) {
                continue;
            }

            if (child.occurrence().isRepeatable()) {
                for (int index = 0; index < matches.size(); index++) {
                    Element match = matches.get(index);
                    consumedElements.add(match);
                    mapElement(child, match, path + "." + child.name() + "[" + index + "]", values, repeatCounts, unknownContent);
                }
            } else {
                Element first = matches.get(0);
                consumedElements.add(first);
                mapElement(child, first, path + "." + child.name(), values, repeatCounts, unknownContent);
            }
        }
        captureUnknownChildren(parentElement, path, consumedElements, unknownContent);
        return consumedElements;
    }

    private void mapGroupNode(
        SchemaNode groupNode,
        Element parentElement,
        String path,
        Map<String, Object> values,
        Map<String, Integer> repeatCounts,
        List<UnknownXmlContent> unknownContent,
        Set<Element> consumedElements
    ) {
        TypeDefinition type = groupNode.typeDefinition();
        if (!type.isComplex()) {
            return;
        }
        ComplexType groupComplex = (ComplexType) type;
        consumedElements.addAll(
            mapComplexChildren(parentElement, path, groupComplex.children(), values, repeatCounts, unknownContent)
        );
        consumedElements.addAll(childElements(parentElement));
    }

    private void captureUnknownChildren(
        Element parentElement,
        String path,
        Set<Element> consumedElements,
        List<UnknownXmlContent> unknownContent
    ) {
        for (Element child : childElements(parentElement)) {
            if (consumedElements.contains(child)) {
                continue;
            }
            unknownContent.add(new UnknownXmlContent(path + "." + localNameOf(child), serializeNode(child)));
        }
    }

    private List<Element> findMatchingChildren(Element parent, SchemaNode schemaChild) {
        List<Element> matches = new ArrayList<>();
        String expectedName = schemaChild.name();
        String expectedNamespace = blankToNull(schemaChild.namespaceUri());
        for (Element child : childElements(parent)) {
            if (!expectedName.equals(localNameOf(child))) {
                continue;
            }
            String childNamespace = blankToNull(child.getNamespaceURI());
            if (expectedNamespace != null && !Objects.equals(expectedNamespace, childNamespace)) {
                continue;
            }
            matches.add(child);
        }
        return matches;
    }

    private List<Element> childElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private boolean matchesElement(SchemaNode schemaNode, Element element) {
        String schemaNamespace = blankToNull(schemaNode.namespaceUri());
        String elementNamespace = blankToNull(element.getNamespaceURI());
        return schemaNode.name().equals(localNameOf(element))
            && (schemaNamespace == null || Objects.equals(schemaNamespace, elementNamespace));
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse XML instance", ex);
        }
    }

    private String serializeNode(Node node) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize XML content", ex);
        }
    }

    private String localName(String name) {
        int separator = name.indexOf(':');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private String localNameOf(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : localName(element.getTagName());
    }

    private String nonBlankText(Element element) {
        String text = element.getTextContent();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    private record RootSelection(
        SchemaNode rootNode,
        String rootName,
        String rootTypeName,
        String rootTypeNamespaceUri
    ) {
    }

    private record ParsedQualifiedName(String localName, String namespaceUri) {
    }
}
