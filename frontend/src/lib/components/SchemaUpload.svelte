<script lang="ts">
  import { createEventDispatcher, onMount } from 'svelte';
  import { listPreloadedSchemas, fetchPreloadedSchema } from '$lib/api/schema';
  import type { PreloadedSchemaInfo } from '$lib/types/schema';

  const dispatch = createEventDispatcher<{
    load: { xsd: string; systemId?: string | null };
  }>();

  const CUSTOM_VALUE = '__custom__';

  let preloadedSchemas: PreloadedSchemaInfo[] = [];
  let selectedId = '';
  let showCustomLoader = false;
  let xsd = '';
  let loadingPreloaded = false;
  let preloadError = '';

  onMount(async () => {
    try {
      preloadedSchemas = await listPreloadedSchemas();
    } catch {
      // custom loader still works if preloaded list fails
    }
  });

  async function onDropdownChange(event: Event) {
    const select = event.currentTarget as HTMLSelectElement;
    const id = select.value;
    preloadError = '';

    if (id === CUSTOM_VALUE) {
      showCustomLoader = true;
      return;
    }

    showCustomLoader = false;
    if (!id) return;

    loadingPreloaded = true;
    try {
      const payload = await fetchPreloadedSchema(id);
      dispatch('load', payload);
    } catch (cause) {
      preloadError = cause instanceof Error ? cause.message : 'Failed to load schema';
    } finally {
      loadingPreloaded = false;
    }
  }

  function emitCustomLoad() {
    dispatch('load', { xsd });
  }
</script>

<section>
  <h2>Schema</h2>
  {#if preloadedSchemas.length > 0}
    <label>
      Select a preloaded IVOA schema
      <select value={selectedId} on:change={onDropdownChange} disabled={loadingPreloaded}>
        <option value="">— choose a schema —</option>
        {#each preloadedSchemas as schema}
          <option value={schema.id}>{schema.label}</option>
        {/each}
        <option value={CUSTOM_VALUE}>Load other…</option>
      </select>
    </label>
    {#if loadingPreloaded}
      <p class="banner info" role="status">Loading schema…</p>
    {/if}
    {#if preloadError}
      <p class="banner error" role="alert">{preloadError}</p>
    {/if}
  {/if}

  {#if showCustomLoader || preloadedSchemas.length === 0}
    <label for="xsd">XSD</label>
    <textarea id="xsd" bind:value={xsd} rows="12" spellcheck="false"></textarea>
    <div>
      <input
        type="file"
        accept=".xsd,.xml"
        on:change={async (event) => {
          const input = event.currentTarget as HTMLInputElement;
          const file = input.files?.[0];
          if (!file) return;
          xsd = await file.text();
        }}
      />
      <button type="button" on:click={emitCustomLoad}>Parse schema</button>
    </div>
  {/if}
</section>
