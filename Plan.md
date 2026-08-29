# Plan for XMLInstance editor

## Goal

A tool where a user uploads (or pastes) an XSD, and the app dynamically builds a UI for producing a valid XML instance against it — rather than hand-coding a form per schema.

The core technical challenge is that XSD is expressive (choices, sequences, recursive types, restrictions, substitution groups, namespaces, imports), so the design lives or dies on how well you model the schema internally before you ever touch UI.

## Architecture overview

**Frontend** (Svelte)
- Schema upload/paste + parse trigger
- Dynamic form renderer driven by an internal schema model
- Live XML preview pane (source view, updates as the form changes)
- Validation feedback inline

**Backend** (Quarkus)
- XSD parser → internal schema model (IR)
- Validation service (validate generated instance against original XSD)
- Optional: XML generation/serialization service if you want heavy lifting off the client

Use quarkus Quinoa extension to serve the Svelte frontend as static assets, so you can deploy this as a single Quarkus app.

You *can* do this client-side only (there are JS XSD parsers), but pushing parsing/validation to a backend using a mature library (e.g. `xmlschema` in Python, or Java's Xerces via a microservice) gives you much more reliable XSD compliance than any JS-native parser currently offers. I'd recommend: backend does parsing + validation, frontend just renders and edits.

## Step 1: Convert XSD → internal schema model

Don't try to render straight from the XSD DOM — build an intermediate representation (IR) first. Something like:

```
SchemaNode {
  name: string
  type: "element" | "attribute"
  dataType: SimpleType | ComplexType
  minOccurs / maxOccurs
  nillable: boolean
  fixed / default value
}

ComplexType {
  compositor: "sequence" | "choice" | "all"
  children: SchemaNode[]
  attributes: AttributeNode[]
  isRecursive: boolean  // flagged during resolution
}

SimpleType {
  baseType: xsd primitive (string, int, date, ...)
  restrictions: { enumeration?, pattern?, minLength?, maxLength?, minInclusive?, ... }
}
```

This resolution step needs to handle, in order of importance:
- **xs:import / xs:include** — resolve across files/namespaces before rendering anything
- **Named type reuse** (`complexType name="..."`, `group`, `attributeGroup`) — resolve references, but keep the reference identity so recursive types don't infinitely expand
- **xs:choice** — becomes a UI "pick one of N" control, not just a flattened list of fields
- **xs:sequence vs xs:all** — affects both validation and element ordering in output
- **Occurrence constraints** (`minOccurs`/`maxOccurs`) — drives repeatable-field UI (add/remove instance buttons)
- **Restrictions/facets on simple types** — drives which input control you render (enum → dropdown, pattern → regex-validated text input, numeric bounds → number input with min/max)
- **Substitution groups & abstract elements** — user needs to pick a concrete substitutable element
- **xs:any / xs:anyAttribute** — the escape hatch; render as a raw XML/attribute text box since it's schema-open by definition
- **Recursion** — detect cycles during resolution and render recursive branches lazily (don't try to expand infinitely; let the user "add a nested X" on demand)

## Step 2: Render the form from the IR

Map IR node types to controls:

| IR construct | UI control |
|---|---|
| simple type, no restriction | text/number/date input by base type |
| enumeration | dropdown or radio |
| pattern facet | text input + regex validation, show pattern as hint |
| boolean | toggle |
| complex type, sequence | grouped fieldset, children in order |
| complex type, choice | tab or radio switch between alternative branches |
| repeatable element (maxOccurs > 1) | list with add/remove, respecting min/max bounds |
| optional element (minOccurs = 0) | collapsed/toggleable section |
| attributes | rendered inline at top of the element's fieldset |
| xs:any | raw XML snippet textarea |

Nested complex types become collapsible tree sections so deep schemas don't overwhelm the screen — this is probably your main UX risk, since real-world XSDs (e.g. industry standards like HL7, UBL, FpML) can be hundreds of elements deep.

## Step 3: Serialize to XML

As the user fills the form, walk the same IR (now populated with values) and serialize in schema-declared order (respecting sequence/choice/all), handling:
- namespace prefixes and `xmlns` declarations at the root
- proper escaping of text content
- omission of optional elements left empty (unless `nillable` and user explicitly nils it)

## Step 4: Validate

Round-trip the generated XML back through a real XSD validator (don't trust your own generation logic alone) — this catches bugs in the IR/generation step and gives the user confidence the output is actually conformant. Show errors inline, mapped back to the relevant form field where possible.

## Key design decisions to flag to the user upfront

- **Client vs. server-side validation**: server-side is more correct but adds latency; consider debounced background validation.
- **How to handle unbounded repeatable elements** (`maxOccurs="unbounded"`) in the UI without infinite scrolling — pagination or lazy rendering per section.
- **Multiple root elements**: if the XSD declares several possible root elements, let the user pick which one they're instantiating.
- **Save/load partial instances**: probably wanted, since large schemas won't be filled in one sitting — store form state (not XML) so users can resume.

## Suggested stack

### Backend - Quarkus (Java)

Java is actually a strong fit for this — the XSD tooling ecosystem there is more mature than JS or even Python, since Java's own JAXB/Xerces stack grew up alongside the XSD spec itself.

## Core XSD parsing/model library

**Xerces2-J** (Apache) is the reference-quality XSD 1.0/1.1 processor and is what most other validators use under the hood. Two ways to use it:

1. **Xerces XSD Schema API (XSModel)** — gives you a fully resolved, in-memory object model of the schema (`XSModel`, `XSElementDeclaration`, `XSComplexTypeDefinition`, `XSParticle`, etc.) with imports/includes already resolved, type references already linked, and substitution groups already computed. This maps almost directly onto the IR I described earlier — you're mostly doing a translation pass from `XSModel` to your own simplified schema tree, not re-implementing resolution logic yourself.
2. Raw SAX/DOM parsing of the XSD as XML — much more manual work, avoid unless you have a specific reason.

Go with option 1. It saves you from reimplementing the gnarliest parts (substitution group resolution, type derivation, import/include chains).

## Alternative/complementary libraries

- **JAXB (`xjc`)** — compiles XSD into Java classes at build time. Not useful here since your schemas are arbitrary and uploaded at runtime, not known in advance. Skip it for the core engine, though it's worth knowing about if you ever want a "generate typed Java bindings" side feature.
- **Eclipse XSD (`org.eclipse.xsd`)** — another mature XSD object model, used in Eclipse's own tooling. Comparable to Xerces XSModel, a bit more ceremony to set up standalone (it expects an EMF environment), so I'd default to Xerces unless you specifically want EMF integration.
- **xmlschema-core (Apache, formerly XSOM lineage / actually distinct)** — worth a mention: **XSOM** (`com.sun.xml.xsom`) is a lighter-weight pure-Java XSD object model, easier API than raw Xerces XSModel, no XML parsing ceremony. Good middle ground if XSModel's API feels too low-level. Many people find XSOM's traversal API more pleasant to work with than XSModel's.

My actual recommendation: **start with XSOM** for readability/maintainability of your IR-building code, and keep **Xerces** in the stack anyway for the final validation step (since Xerces's validator is the most battle-tested).

## Validation (round-trip check)

Use **`javax.xml.validation.SchemaFactory`** with `http://www.w3.org/2001/XMLSchema` (backed by Xerces on the classpath) to validate your generated instance XML against the original XSD before returning it to the user. This is standard JAXP, no extra dependency needed beyond Xerces being on the classpath.

## Serialization

Plain **DOM + `javax.xml.transform.Transformer`**, or **Woodstox/StAX** if you want streaming for very large instances. For most form-driven generation, DOM is simpler and fine — instances built from a form aren't going to be huge.

## Framework/stack suggestion

- **Quarkus** for the REST API (`/schema/parse`, `/instance/validate`, `/instance/generate`) — standard choice, easy to add async/caching later for large schemas
- Keep the **XSD → IR translation layer** as a separate, framework-agnostic module/library (no Spring annotations in it) — it's the part most likely to need unit testing in isolation against tricky real-world schemas
- Cache parsed `XSModel`/XSOM results per uploaded schema (keyed by hash) — re-resolving a large industry schema (HL7, UBL, FpML) on every request is wasteful since resolution is the expensive part

## Suggested module layout

```
schema-core/       # XSD -> IR translation (XSOM/Xerces), no web deps
  ├── resolver/     # import/include resolution, substitution groups
  ├── model/        # IR classes (SchemaNode, ComplexType, SimpleType...)
  └── validator/    # JAXP-based instance validation

xml-generator/      # IR + form values -> XML instance (DOM/StAX serialization)

api/                # Quarkus controllers, DTOs for frontend
```

One thing worth deciding early: do you want the IR (schema-core) to be a library you could later reuse outside this web app (e.g., a CLI tool, a batch validator)? If so, keeping it dependency-free from Spring is worth the small extra effort now rather than retrofitting later.



I'll sketch this as actual Java files so you have something concrete to build from — the IR model classes plus the XSOM-to-IR translator that does the real work.That's the full IR model plus the XSOM traversal that builds it. Key points worth calling out:

- **`model/`** — `SchemaNode`, `ComplexType`, `SimpleType`, `AttributeNode`, `Compositor` are the plain-data IR classes, framework-free so this module stays reusable outside a web app.
- **`translator/XsomToIrTranslator.java`** — the actual traversal. The interesting parts:
    - **Recursion handling**: an `inProgress` stack tracks named complex types currently being expanded; hitting one already on the stack produces a `recursive=true` stub instead of infinite recursion.
    - **Choice/sequence/all**: `translateModelGroupInto` walks `XSModelGroup` children, recursing into nested groups (a `<xs:choice>` inside a `<xs:sequence>`) via a synthetic wrapper node so the frontend can still render the right "pick one" affordance at the right depth.
    - **Facets → `SimpleType`**: enumeration, pattern, length/digit bounds are all pulled off `XSRestrictionSimpleType`.
    - **Substitution groups**: resolved via a reverse scan since XSOM doesn't index this itself (fine for typical schema sizes; worth caching if you're dealing with huge ones like UBL).
    - Left as a marked TODO: `xs:union`/`xs:list` simple types and `simpleContent` text-typing — noted in comments where they'd plug in.

Two things I deliberately left unfinished since they're implementation choices, not sketching gaps:
1. `simpleContent` (complex type = attributes + typed text) — `ComplexType` needs a `textContent: SimpleType` field to fully capture this.
2. Caching `isSubstitutionHead`/`findSubstitutionMembers` — currently O(n) scans per element; fine until you hit a schema with thousands of global elements.


## Svelte Front End

Svelte actually fits this problem well — arguably better than React for the specific rendering challenge here.

## Why it's a good fit

The core UI problem is: **render a deeply nested, recursive tree of form controls driven by a JSON tree (the IR), where user input needs to flow back into that same tree and immediately re-serialize to XML.** That's a lot of fine-grained, nested reactive state — exactly the case where Svelte's compiler-driven reactivity (no virtual DOM diffing, no memoization boilerplate) tends to stay simpler than React as nesting grows. You won't be reaching for `useMemo`/`useCallback` to avoid re-rendering a 40-level-deep schema tree on every keystroke.

The recursive rendering itself (a complex type containing children that may themselves be complex types, `xs:choice` branches containing more branches) maps cleanly onto **Svelte's native support for a component importing itself** — `SchemaNode.svelte` rendering more `SchemaNode.svelte` instances for its children, no special-casing needed.

## Suggested structure

```
src/
  lib/
    api/              # fetch calls to the Spring backend
      schema.ts        # POST /schema/parse -> IR
      validate.ts       # POST /instance/validate
    stores/
      schemaStore.ts    # the parsed IR (read-only after load)
      instanceStore.ts  # the user's in-progress values, mirrors IR shape
      validationStore.ts # errors keyed by node path
    components/
      SchemaUpload.svelte
      form/
        SchemaNode.svelte      # dispatch component: picks the right control
        SequenceGroup.svelte
        ChoiceGroup.svelte     # tabs/radio between alternatives
        RepeatableField.svelte # add/remove for maxOccurs > 1
        LeafInput.svelte       # dispatches to control by SimpleType.baseKind
        EnumSelect.svelte
        PatternInput.svelte
        RecursiveBranch.svelte # "+ Add nested X" on-demand expansion
      preview/
        XmlPreview.svelte      # CodeMirror/Monaco, derived from instanceStore
      ValidationPanel.svelte
  routes/
    +page.svelte
```

## State shape

Keep the IR (`schemaStore`) and the user's values (`instanceStore`) as **separate trees that mirror each other by node path**, rather than mutating the IR directly. This matters because:
- the IR is essentially immutable once parsed (it came from the backend)
- validation errors, "is this optional section expanded", and repeated-element instance counts are all *view state*, not schema state, and keeping them separate avoids polluting your schema model with UI concerns

```ts
// instanceStore.ts
import { writable } from 'svelte/store';

// Keyed by node path, e.g. "Order.Items[2].Sku"
export const instanceValues = writable<Record<string, unknown>>({});
export const expandedOptional = writable<Set<string>>(new Set());
export const repeatCounts = writable<Record<string, number>>({});
```

Each leaf `LeafInput.svelte` writes to `instanceValues` on change; a derived store recomputes the XML preview reactively:

```ts
// derived XML preview, recalculated whenever values change
export const xmlPreview = derived(
  [schemaStore, instanceValues],
  ([$schema, $values]) => serializeToXml($schema, $values)
);
```

If serialization is at all expensive on large schemas, debounce this derivation rather than recomputing on every keystroke.

## The recursive component itself

This is the part that benefits most from Svelte specifically:

```svelte
<!-- SchemaNode.svelte -->
<script lang="ts">
  export let node: SchemaNode;
  export let path: string;
</script>

{#if node.isLeaf}
  <LeafInput {node} {path} />
{:else if node.complexType.compositor === 'CHOICE'}
  <ChoiceGroup {node} {path} />
{:else}
  <SequenceGroup {node} {path} />
{/if}
```

```svelte
<!-- SequenceGroup.svelte -->
<script lang="ts">
  export let node: SchemaNode;
  export let path: string;
</script>

<fieldset>
  <legend>{node.name}</legend>
  {#each node.complexType.children as child}
    {#if child.isRepeatable}
      <RepeatableField node={child} path={`${path}.${child.name}`} />
    {:else}
      <svelte:self node={child} path={`${path}.${child.name}`} />
    {/if}
  {/each}
</fieldset>
```

`<svelte:self>` is the clean part — no manual registration or forward-declaration needed to let a component recurse into instances of itself, which is exactly your "nested complex type contains more complex types" case.

## Testing

The canonical set of of schemas to test against are the the IVOA schema in https://github.com/Javastro/ivoa-schemata - they should be included in the project as a gradle dependency for testing purposes. In particular test cases should concentrate on the `VODataService-v1.2.xsd` found in that dependency.




