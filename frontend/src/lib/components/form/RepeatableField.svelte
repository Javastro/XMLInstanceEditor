<script lang="ts">
  import type { SchemaNode as SchemaNodeModel } from '$lib/types/schema';
  import { repeatCounts } from '$lib/stores/instanceStore';
  import SchemaNode from './SchemaNode.svelte';

  export let node: SchemaNodeModel;
  export let path: string;
  export let childIndex = 0;

  $: repeatPath = `${path}.${node.name}.${childIndex}`;
  $: currentCount = $repeatCounts[repeatPath] ?? Math.max(node.occurrence.minOccurs, 1);

  $: maxOccurs = node.occurrence.maxOccurs ?? -1;
  $: atMax = maxOccurs > 0 && currentCount >= maxOccurs;
  $: atMin = currentCount <= Math.max(node.occurrence.minOccurs, 1);

  function addItem() {
    if (atMax) return;
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
  <header class="repeat-header">
    <strong>{node.name}</strong>
    <span class="count">
      {currentCount}{maxOccurs > 0 ? ` / ${maxOccurs}` : ''}
      {#if node.occurrence.minOccurs > 0}(min {node.occurrence.minOccurs}){/if}
    </span>
    <button type="button" on:click={addItem} disabled={atMax}>Add</button>
    <button type="button" class="danger" on:click={removeItem} disabled={atMin}>Remove</button>
  </header>

  {#each Array(currentCount) as _, index}
    <SchemaNode node={node} path={`${path}.${node.name}[${index}]`} />
  {/each}
</section>
