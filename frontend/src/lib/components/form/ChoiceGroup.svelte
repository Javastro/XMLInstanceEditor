<script lang="ts">
  import type { SchemaNode as SchemaNodeModel, ComplexType } from '$lib/types/schema';
  import SchemaNode from './SchemaNode.svelte';

  export let node: SchemaNodeModel;
  export let path: string;

  const group = node.typeDefinition as ComplexType;
  let selectedIndex = 0;
  let selectedChild: SchemaNodeModel | undefined;

  $: selectedChild = group.children[selectedIndex];
</script>

<fieldset>
  <legend>{node.name} (choice)</legend>
  <select bind:value={selectedIndex}>
    {#each group.children as child, index}
      <option value={index}>{child.name}</option>
    {/each}
  </select>

  {#if selectedChild}
    <SchemaNode node={selectedChild} path={`${path}.${selectedChild.name}`} />
  {/if}
</fieldset>
