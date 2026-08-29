<script lang="ts">
  import type { SchemaNode as SchemaNodeModel } from '$lib/types/schema';
  import { repeatCounts } from '$lib/stores/instanceStore';
  import SchemaNode from './SchemaNode.svelte';

  export let node: SchemaNodeModel;
  export let path: string;
  export let childIndex = 0;

  $: repeatPath = `${path}.${node.name}.${childIndex}`;
  $: currentCount = $repeatCounts[repeatPath] ?? Math.max(node.occurrence.minOccurs, 1);

  function addItem() {
    repeatCounts.update((current) => ({
      ...current,
      [repeatPath]: currentCount + 1
    }));
  }

  function removeItem() {
    repeatCounts.update((current) => ({
      ...current,
      [repeatPath]: Math.max(node.occurrence.minOccurs, currentCount - 1)
    }));
  }
</script>

<section>
  <header>
    <strong>{node.name}</strong>
    <button type="button" on:click={addItem}>Add</button>
    <button type="button" on:click={removeItem}>Remove</button>
  </header>

  {#each Array(currentCount) as _, index}
    <SchemaNode node={node} path={`${path}.${node.name}[${index}]`} />
  {/each}
</section>
