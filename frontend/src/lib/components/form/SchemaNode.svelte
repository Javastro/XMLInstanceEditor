<script lang="ts">
  import type { SchemaNode as SchemaNodeModel, ComplexType } from '$lib/types/schema';
  import ChoiceGroup from './ChoiceGroup.svelte';
  import LeafInput from './LeafInput.svelte';
  import SequenceGroup from './SequenceGroup.svelte';

  export let node: SchemaNodeModel;
  export let path: string;

  function isComplexType(type: SchemaNodeModel['typeDefinition']): type is ComplexType {
    return 'children' in type;
  }
</script>

{#if node.kind === 'GROUP' && isComplexType(node.typeDefinition)}
  {#if node.typeDefinition.compositor === 'CHOICE'}
    <ChoiceGroup {node} {path} />
  {:else}
    <SequenceGroup {node} {path} />
  {/if}
{:else if isComplexType(node.typeDefinition)}
  <SequenceGroup {node} {path} />
{:else}
  <LeafInput {node} {path} />
{/if}
