package org.javastro.xmlinstanceeditor.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.ParserConfigurationException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.xml.sax.InputSource;

public final class XmlSerializer {
    public String toXml(XmlElement root) {
        var buffer = new java.io.ByteArrayOutputStream();
        write(root, buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public void write(XmlElement root, OutputStream outputStream) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var document = factory.newDocumentBuilder().newDocument();
            document.appendChild(writeElement(document, root));

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize XML", ex);
        }
    }

    private Element writeElement(Document document, XmlElement source) {
        Element element = source.namespaceUri() == null || source.namespaceUri().isBlank()
            ? document.createElement(source.name())
            : document.createElementNS(source.namespaceUri(), source.name());

        for (Map.Entry<String, String> attribute : source.attributes().entrySet()) {
            if (attribute.getKey().startsWith("xmlns")) {
                element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attribute.getKey(), attribute.getValue());
            } else {
                element.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }

        for (XmlNode child : source.children()) {
            appendChild(document, element, child);
        }

        return element;
    }

    private void appendChild(Document document, Element parent, XmlNode child) {
        if (child instanceof XmlElement element) {
            parent.appendChild(writeElement(document, element));
            return;
        }
        if (child instanceof XmlText text) {
            Text domText = document.createTextNode(text.value());
            parent.appendChild(domText);
            return;
        }
        if (child instanceof XmlRawFragment fragment) {
            appendRawFragment(document, parent, fragment.xml());
            return;
        }
        throw new IllegalArgumentException("Unsupported XML node: " + child.getClass().getName());
    }

    private void appendRawFragment(Document document, Element parent, String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var fragmentDocument = factory.newDocumentBuilder().parse(new InputSource(new StringReader("<fragment>" + xml + "</fragment>")));
            var fragmentRoot = fragmentDocument.getDocumentElement();
            var childNodes = fragmentRoot.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                var imported = document.importNode(childNodes.item(i), true);
                parent.appendChild(imported);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to embed raw XML fragment", ex);
        }
    }
}
