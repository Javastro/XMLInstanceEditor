<script lang="ts">
  import type { SchemaNode as SchemaNodeModel } from '$lib/types/schema';
  import SchemaNode from '$lib/components/form/SchemaNode.svelte';

  export let roots: SchemaNodeModel[] = [];
  export let xml = '';
</script>

<section>
  <h2>Instance editor</h2>
  {#if roots.length === 0}
    <p class="empty">Load an XSD schema to start editing an instance.</p>
  {:else}
    {#each roots as root}
      <SchemaNode node={root} path={root.name} />
    {/each}
  {/if}
  <h3>Instance XML</h3>
  <textarea bind:value={xml} rows="16" spellcheck="false"></textarea>
</section>
