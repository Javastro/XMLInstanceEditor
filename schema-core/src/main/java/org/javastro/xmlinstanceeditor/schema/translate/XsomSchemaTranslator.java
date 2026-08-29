package org.javastro.xmlinstanceeditor.schema.translate;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSListSimpleType;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;
import org.javastro.xmlinstanceeditor.schema.model.AttributeNode;
import org.javastro.xmlinstanceeditor.schema.model.Compositor;
import org.javastro.xmlinstanceeditor.schema.model.ComplexType;
import org.javastro.xmlinstanceeditor.schema.model.FacetConstraints;
import org.javastro.xmlinstanceeditor.schema.model.NodeKind;
import org.javastro.xmlinstanceeditor.schema.model.Occurrence;
import org.javastro.xmlinstanceeditor.schema.model.SchemaDocument;
import org.javastro.xmlinstanceeditor.schema.model.SchemaNode;
import org.javastro.xmlinstanceeditor.schema.model.SchemaTypeOption;
import org.javastro.xmlinstanceeditor.schema.model.SimpleType;
import org.javastro.xmlinstanceeditor.schema.model.TypeDefinition;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class XsomSchemaTranslator implements SchemaTranslator {
    private final Map<XSSimpleType, SimpleType> simpleTypeCache = new IdentityHashMap<>();
    private final Map<XSComplexType, ComplexType> complexTypeCache = new IdentityHashMap<>();
    private final Deque<XSComplexType> complexTypeStack = new ArrayDeque<>();

    @Override
    public SchemaDocument translate(InputStream xsdStream, String systemId) {
        return translate(xsdStream, systemId, null);
    }

    public SchemaDocument translate(InputStream xsdStream, String systemId, EntityResolver entityResolver) {
        try {
            simpleTypeCache.clear();
            complexTypeCache.clear();
            complexTypeStack.clear();

            var factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var parser = new XSOMParser(factory);
            if (entityResolver != null) {
                parser.setEntityResolver(entityResolver);
            }
            var source = new InputSource(xsdStream);
            source.setSystemId(systemId);
            source.setEncoding(StandardCharsets.UTF_8.name());
            parser.parse(source);

            XSSchemaSet schemaSet = parser.getResult();
            if (schemaSet == null) {
                throw new SchemaTranslationException("XSOM returned no schema set");
            }

            return translateSchemaSet(schemaSet, systemId);
        } catch (Exception ex) {
            throw new SchemaTranslationException("Failed to translate XSD", ex);
        }
    }

    private SchemaDocument translateSchemaSet(XSSchemaSet schemaSet, String systemId) {
        List<SchemaNode> roots = new ArrayList<>();
        schemaSet.iterateElementDecls().forEachRemaining(decl -> {
            if (decl.isGlobal()) {
                roots.add(translateElementDecl(decl, Occurrence.required()));
            }
        });
        roots.sort(Comparator.comparing(SchemaNode::namespaceUri, Comparator.nullsFirst(String::compareTo))
            .thenComparing(SchemaNode::name));
        List<SchemaTypeOption> availableRootTypes = collectTypeOptions(schemaSet);

        String targetNamespace = roots.stream()
            .map(SchemaNode::namespaceUri)
            .filter(namespace -> namespace != null && !namespace.isBlank())
            .findFirst()
            .orElse(null);
        return new SchemaDocument(targetNamespace, Map.of(), roots, availableRootTypes, systemId);
    }

    private List<SchemaTypeOption> collectTypeOptions(XSSchemaSet schemaSet) {
        List<SchemaTypeOption> options = new ArrayList<>();
        schemaSet.iterateTypes().forEachRemaining(type -> {
            if (!type.isGlobal()) {
                return;
            }
            String name = type.getName();
            if (name == null || name.isBlank()) {
                return;
            }
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type.getTargetNamespace())) {
                return;
            }
            options.add(new SchemaTypeOption(
                name,
                type.getTargetNamespace(),
                translateType(type)
            ));
        });
        options.sort(Comparator.comparing(SchemaTypeOption::namespaceUri, Comparator.nullsFirst(String::compareTo))
            .thenComparing(SchemaTypeOption::name));
        return List.copyOf(options);
    }

    private SchemaNode translateElementDecl(XSElementDecl decl, Occurrence occurrence) {
        List<SchemaNode> substitutionOptions = decl.isAbstract()
            ? translateSubstitutionOptions(decl)
            : List.of();

        return new SchemaNode(
            decl.getName(),
            NodeKind.ELEMENT,
            occurrence,
            decl.isNillable(),
            xmlString(decl.getDefaultValue()),
            xmlString(decl.getFixedValue()),
            decl.getTargetNamespace(),
            decl.isAbstract(),
            substitutionOptions,
            translateType(decl.getType())
        );
    }

    private List<SchemaNode> translateSubstitutionOptions(XSElementDecl head) {
        return head.getSubstitutables().stream()
            .filter(candidate -> candidate != head)
            .filter(candidate -> !candidate.isAbstract())
            .map(candidate -> translateElementDecl(candidate, Occurrence.required()))
            .sorted(Comparator.comparing(SchemaNode::namespaceUri, Comparator.nullsFirst(String::compareTo))
                .thenComparing(SchemaNode::name))
            .toList();
    }

    private TypeDefinition translateType(XSType type) {
        if (type.isSimpleType()) {
            return translateSimpleType(type.asSimpleType());
        }
        return translateComplexType(type.asComplexType());
    }

    private SimpleType translateSimpleType(XSSimpleType type) {
        if (type == null) {
            return new SimpleType("string", FacetConstraints.empty());
        }
        SimpleType cached = simpleTypeCache.get(type);
        if (cached != null) {
            return cached;
        }

        SimpleType translated;
        if (type.isUnion()) {
            translated = new SimpleType("union", FacetConstraints.empty());
        } else if (type.isList()) {
            XSListSimpleType listType = type.asList();
            translated = new SimpleType(listType.getItemType().getName() != null ? listType.getItemType().getName() : "string", FacetConstraints.empty());
        } else {
            translated = new SimpleType(baseTypeName(type), extractFacets(type));
        }

        simpleTypeCache.put(type, translated);
        return translated;
    }

    private ComplexType translateComplexType(XSComplexType type) {
        ComplexType cached = complexTypeCache.get(type);
        if (cached != null) {
            return cached;
        }
        if (complexTypeStack.contains(type)) {
            return new ComplexType(Compositor.SEQUENCE, List.of(), List.of(), Optional.empty(), true);
        }

        complexTypeStack.push(type);
        try {
            ComplexBody body = translateContent(type.getContentType());
            List<AttributeNode> attributes = translateAttributes(type);
            ComplexType translated = new ComplexType(
                body.compositor,
                body.children,
                attributes,
                body.textContent,
                false
            );
            complexTypeCache.put(type, translated);
            return translated;
        } finally {
            complexTypeStack.pop();
        }
    }

    private ComplexBody translateContent(XSContentType contentType) {
        XSSimpleType simpleType = contentType.asSimpleType();
        if (simpleType != null) {
            return new ComplexBody(Compositor.SEQUENCE, List.of(), Optional.of(translateSimpleType(simpleType)));
        }

        XSParticle particle = contentType.asParticle();
        if (particle == null) {
            return new ComplexBody(Compositor.SEQUENCE, List.of(), Optional.empty());
        }

        return new ComplexBody(Compositor.SEQUENCE, List.of(translateParticle(particle)), Optional.empty());
    }

    private SchemaNode translateParticle(XSParticle particle) {
        Occurrence occurrence = occurrenceOf(particle);
        XSTerm term = particle.getTerm();

        if (term.isElementDecl()) {
            return translateElementDecl(term.asElementDecl(), occurrence);
        }

        if (term.isWildcard()) {
            return new SchemaNode(
                "any",
                NodeKind.ANY,
                occurrence,
                false,
                null,
                null,
                null,
                false,
                List.of(),
                new SimpleType("any", FacetConstraints.empty())
            );
        }

        if (term.isModelGroup()) {
            return translateGroup(term.asModelGroup(), occurrence);
        }

        if (term.isModelGroupDecl()) {
            return translateGroup(term.asModelGroupDecl().getModelGroup(), occurrence);
        }

        throw new SchemaTranslationException("Unsupported schema term: " + term.getClass().getName());
    }

    private SchemaNode translateGroup(XSModelGroup group, Occurrence occurrence) {
        List<SchemaNode> children = new ArrayList<>();
        for (XSParticle child : group.getChildren()) {
            children.add(translateParticle(child));
        }

        return new SchemaNode(
            group.getCompositor().name().toLowerCase(),
            NodeKind.GROUP,
            occurrence,
            false,
            null,
            null,
            null,
            false,
            List.of(),
            new ComplexType(mapCompositor(group.getCompositor()), children, List.of(), Optional.empty(), false)
        );
    }

    private List<AttributeNode> translateAttributes(XSComplexType type) {
        List<AttributeNode> attributes = new ArrayList<>();
        for (XSAttributeUse use : type.getAttributeUses()) {
            XSAttributeDecl decl = use.getDecl();
            attributes.add(new AttributeNode(
                decl.getName(),
                translateSimpleType(decl.getType()),
                use.isRequired(),
                xmlString(use.getDefaultValue() != null ? use.getDefaultValue() : decl.getDefaultValue()),
                xmlString(use.getFixedValue() != null ? use.getFixedValue() : decl.getFixedValue())
            ));
        }

        if (type.getAttributeWildcard() != null) {
            attributes.add(new AttributeNode(
                "*",
                new SimpleType("any", FacetConstraints.empty()),
                false,
                null,
                null
            ));
        }

        return List.copyOf(attributes);
    }

    private FacetConstraints extractFacets(XSSimpleType type) {
        if (!type.isRestriction()) {
            return FacetConstraints.empty();
        }

        XSRestrictionSimpleType restriction = type.asRestriction();
        List<String> enumeration = restriction.getDeclaredFacets(XSFacet.FACET_ENUMERATION).stream()
            .map(facet -> facet.getValue().value)
            .toList();
        String pattern = restriction.getDeclaredFacets(XSFacet.FACET_PATTERN).stream()
            .map(facet -> facet.getValue().value)
            .findFirst()
            .orElse(null);

        return new FacetConstraints(
            enumeration,
            pattern,
            integerFacet(restriction, XSFacet.FACET_MINLENGTH),
            integerFacet(restriction, XSFacet.FACET_MAXLENGTH)
        );
    }

    private Integer integerFacet(XSRestrictionSimpleType restriction, String facetName) {
        XSFacet facet = restriction.getDeclaredFacet(facetName);
        if (facet == null) {
            return null;
        }
        try {
            return Integer.valueOf(facet.getValue().value);
        } catch (NumberFormatException ex) {
            throw new SchemaTranslationException("Invalid integer facet value for " + facetName + ": " + facet.getValue(), ex);
        }
    }

    private Occurrence occurrenceOf(XSParticle particle) {
        int min = particle.getMinOccurs().intValueExact();
        Integer max = particle.getMaxOccurs() == null || particle.getMaxOccurs().intValue() == XSParticle.UNBOUNDED
            ? null
            : particle.getMaxOccurs().intValueExact();
        return new Occurrence(min, max);
    }

    private Compositor mapCompositor(XSModelGroup.Compositor compositor) {
        return switch (compositor) {
            case ALL -> Compositor.ALL;
            case CHOICE -> Compositor.CHOICE;
            case SEQUENCE -> Compositor.SEQUENCE;
        };
    }

    private String baseTypeName(XSSimpleType type) {
        String name = type.getName();
        if (name != null) {
            return name;
        }

        XSSimpleType primitive = type.getPrimitiveType();
        if (primitive != null && primitive != type) {
            return baseTypeName(primitive);
        }

        XSSimpleType base = type.getSimpleBaseType();
        if (base != null && base != type) {
            return baseTypeName(base);
        }

        return "string";
    }

    private String xmlString(com.sun.xml.xsom.XmlString value) {
        return value == null ? null : value.value;
    }

    private record ComplexBody(Compositor compositor, List<SchemaNode> children, Optional<SimpleType> textContent) {
        private ComplexBody {
            children = children == null ? List.of() : List.copyOf(children);
            textContent = textContent == null ? Optional.empty() : textContent;
        }
    }
}
