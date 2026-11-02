<script lang="ts">
  import type { SchemaNode as SchemaNodeModel, SimpleType } from '$lib/types/schema';
  import { instanceValues } from '$lib/stores/instanceStore';

  export let node: SchemaNodeModel;
  export let path: string;

  const simpleType = node.typeDefinition as SimpleType;

  $: required = node.occurrence.minOccurs > 0;
  $: current = $instanceValues[path];
  $: missing = required && simpleType.baseType !== 'boolean' && (current === undefined || current === '') && !node.defaultValue;

  function updateValue(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    instanceValues.update((current) => ({
      ...current,
      [path]: input.type === 'checkbox' ? input.checked : input.value
    }));
  }
</script>

<label class:field-missing={missing}>
  <span>
    {node.name}{#if required}<span class="required-mark" aria-hidden="true">*</span>{/if}
    {#if missing}<span class="hint">required</span>{/if}
  </span>
  {#if simpleType.constraints.enumeration.length > 0}
    <select on:change={updateValue}>
      {#each simpleType.constraints.enumeration as option}
        <option value={option}>{option}</option>
      {/each}
    </select>
  {:else if simpleType.baseType === 'boolean'}
    <input type="checkbox" on:change={updateValue} />
  {:else}
    <input type="text" value={node.defaultValue ?? ''} on:input={updateValue} />
  {/if}
</label>
