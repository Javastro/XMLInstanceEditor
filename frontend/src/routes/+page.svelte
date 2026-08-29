<script lang="ts">
  import SchemaUpload from '$lib/components/SchemaUpload.svelte';
  import InstanceEditor from '$lib/components/editor/InstanceEditor.svelte';
  import ValidationPanel from '$lib/components/ValidationPanel.svelte';
  import XmlPreview from '$lib/components/preview/XmlPreview.svelte';
  import { importXmlInstance, parseSchema, type UnknownXmlContent } from '$lib/api/schema';
  import { instanceValues, previewRootName, previewRootType, repeatCounts, xmlPreview } from '$lib/stores/instanceStore';
  import { schemaStore, rootNodes } from '$lib/stores/schemaStore';
  import { validationMessages, validationSummary } from '$lib/stores/validationStore';
  import type { SchemaNode, SchemaTypeOption } from '$lib/types/schema';

  let loading = false;
  let importLoading = false;
  let error = '';
  let instanceXml = '';
  let sourceXsd = '';
  let sourceSystemId: string | null = null;
  let importXml = '';
  let importError = '';
  let unknownContent: UnknownXmlContent[] = [];
  let syntheticRootName = '';
  let syntheticTypeIndex = 0;
  let syntheticRootVersion = '';

  $: availableRootTypes = $schemaStore?.availableRootTypes ?? [];
  $: selectedType = availableRootTypes[syntheticTypeIndex];
  $: syntheticRoot =
    $rootNodes.length === 0 && selectedType && syntheticRootName.trim().length > 0
      ? buildSyntheticRoot(syntheticRootName.trim(), selectedType)
      : null;
  $: editorRoots = $rootNodes.length > 0 ? $rootNodes : syntheticRoot ? [syntheticRoot] : [];
  $: previewRootName.set(editorRoots[0]?.name ?? null);
  $: previewRootType.set(
    $rootNodes.length === 0 && selectedType
      ? { name: selectedType.name, namespaceUri: selectedType.namespaceUri ?? null }
      : null
  );
  $: editorKey = editorRoots
    .map((root) => `${root.name}:${root.namespaceUri ?? ''}`)
    .join('|');
  $: syntheticSelectionKey = `${syntheticRootName.trim()}::${selectedType?.name ?? ''}::${syntheticTypeIndex}`;
  $: if ($rootNodes.length === 0 && syntheticRoot && syntheticSelectionKey !== syntheticRootVersion) {
    instanceXml = `<${syntheticRoot.name}></${syntheticRoot.name}>`;
    syntheticRootVersion = syntheticSelectionKey;
  }

  async function handleLoad(event: CustomEvent<{ xsd: string; systemId?: string | null }>) {
    loading = true;
    error = '';
    try {
      const schema = await parseSchema({ xsd: event.detail.xsd, systemId: event.detail.systemId ?? null });
      sourceXsd = event.detail.xsd;
      sourceSystemId = schema.sourceSystemId ?? event.detail.systemId ?? null;
      schemaStore.set(schema);
      instanceValues.set({});
      repeatCounts.set({});
      unknownContent = [];
      importXml = '';
      validationMessages.set([]);
      validationSummary.set('');
      instanceXml = $xmlPreview;
      syntheticTypeIndex = 0;
      syntheticRootName = schema.availableRootTypes[0]?.name ?? '';
    } catch (cause) {
      error = cause instanceof Error ? cause.message : 'Failed to parse schema';
    } finally {
      loading = false;
    }
  }

  async function handleImportXml() {
    if (!sourceXsd || !importXml.trim()) {
      importError = 'Load a schema and XML instance before importing.';
      return;
    }

    importLoading = true;
    importError = '';
    try {
      const result = await importXmlInstance({
        xsd: sourceXsd,
        systemId: sourceSystemId,
        xml: importXml,
        rootName: syntheticRootName || null,
        rootTypeName: selectedType?.name ?? null,
        rootTypeNamespaceUri: selectedType?.namespaceUri ?? null
      });
      if ($rootNodes.length === 0) {
        syntheticRootName = result.rootName;
        const rootType = result.rootTypeName;
        if (rootType) {
          const index = availableRootTypes.findIndex(
            (option) =>
              option.name === rootType &&
              (option.namespaceUri ?? null) === (result.rootTypeNamespaceUri ?? null)
          );
          if (index >= 0) {
            syntheticTypeIndex = index;
          }
        }
      }
      instanceValues.set(result.values);
      repeatCounts.set(result.repeatCounts);
      unknownContent = result.unknownContent;
      instanceXml = importXml;
    } catch (cause) {
      importError = cause instanceof Error ? cause.message : 'Failed to import XML instance';
    } finally {
      importLoading = false;
    }
  }

  function buildSyntheticRoot(name: string, selected: SchemaTypeOption): SchemaNode {
    return {
      name,
      kind: 'ELEMENT',
      occurrence: { minOccurs: 1, maxOccurs: 1 },
      nillable: false,
      defaultValue: null,
      fixedValue: null,
      namespaceUri: selected.namespaceUri ?? null,
      abstractElement: false,
      substitutionOptions: [],
      typeDefinition: selected.typeDefinition
    };
  }

  function onSyntheticTypeChange(event: Event) {
    const select = event.currentTarget as HTMLSelectElement;
    syntheticTypeIndex = Number(select.value);
  }
</script>

<main>
  <h1>XMLInstance Editor</h1>
  <SchemaUpload on:load={handleLoad} />
  {#if sourceXsd}
    <section>
      <h2>Load XML instance</h2>
      <textarea bind:value={importXml} rows="8" spellcheck="false" placeholder="Paste XML instance here"></textarea>
      <div>
        <input
          type="file"
          accept=".xml,text/xml,application/xml"
          on:change={async (event) => {
            const input = event.currentTarget as HTMLInputElement;
            const file = input.files?.[0];
            if (!file) return;
            importXml = await file.text();
          }}
        />
        <button type="button" on:click={handleImportXml}>Import XML into editor</button>
      </div>
      {#if importLoading}
        <p>Importing XML...</p>
      {/if}
      {#if importError}
        <p>{importError}</p>
      {/if}
    </section>
  {/if}
  {#if $rootNodes.length === 0 && availableRootTypes.length > 0}
    <section>
      <h2>Root element setup</h2>
      <label>
        Root element name
        <input type="text" bind:value={syntheticRootName} />
      </label>
      <label>
        Root type
        <select value={syntheticTypeIndex} on:change={onSyntheticTypeChange}>
          {#each availableRootTypes as option, index}
            <option value={index}>
              {option.name}{option.namespaceUri ? ` (${option.namespaceUri})` : ''}
            </option>
          {/each}
        </select>
      </label>
    </section>
  {/if}
  {#key editorKey}
    <InstanceEditor roots={editorRoots} bind:xml={instanceXml} />
  {/key}
  <button type="button" on:click={() => (instanceXml = $xmlPreview)}>Load generated XML draft</button>

  {#if loading}
    <p>Parsing schema...</p>
  {/if}

  {#if error}
    <p>{error}</p>
  {/if}

  <XmlPreview xml={$xmlPreview} />
  {#if unknownContent.length > 0}
    <section>
      <h3>Unmapped XML content</h3>
      {#each unknownContent as fragment}
        <p>{fragment.path}</p>
        <textarea value={fragment.xml} rows="4" readonly spellcheck="false"></textarea>
      {/each}
    </section>
  {/if}
  <ValidationPanel />
</main>
