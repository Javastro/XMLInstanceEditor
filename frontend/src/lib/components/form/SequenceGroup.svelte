<script lang="ts">
  import type { AttributeNode, SchemaNode as SchemaNodeModel, ComplexType } from '$lib/types/schema';
  import { instanceValues } from '$lib/stores/instanceStore';
  import RepeatableField from './RepeatableField.svelte';
  import SchemaNode from './SchemaNode.svelte';

  export let node: SchemaNodeModel;
  export let path: string;

  const group = node.typeDefinition as ComplexType;

  function attributePath(attribute: AttributeNode): string {
    return `${path}.@${attribute.name}`;
  }

  function updateAttributeValue(attribute: AttributeNode, event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const key = attributePath(attribute);
    instanceValues.update((current) => ({
      ...current,
      [key]: input.type === 'checkbox' ? input.checked : input.value
    }));
  }
</script>

<fieldset>
  <legend>{node.name}</legend>
  {#each group.attributes as attribute}
    <div class="attribute">
      <label>
        <strong>@{attribute.name}</strong>
        {#if attribute.type.constraints.enumeration.length > 0}
          <select
            value={($instanceValues[attributePath(attribute)] as string) ?? attribute.defaultValue ?? ''}
            on:change={(event) => updateAttributeValue(attribute, event)}
            disabled={Boolean(attribute.fixedValue)}
          >
            {#if !attribute.required}
              <option value="">(none)</option>
            {/if}
            {#each attribute.type.constraints.enumeration as option}
              <option value={option}>{option}</option>
            {/each}
          </select>
        {:else if attribute.type.baseType === 'boolean'}
          <input
            type="checkbox"
            checked={Boolean($instanceValues[attributePath(attribute)] ?? (attribute.defaultValue === 'true'))}
            on:change={(event) => updateAttributeValue(attribute, event)}
            disabled={Boolean(attribute.fixedValue)}
          />
        {:else}
          <input
            type="text"
            value={($instanceValues[attributePath(attribute)] as string) ?? attribute.defaultValue ?? attribute.fixedValue ?? ''}
            on:input={(event) => updateAttributeValue(attribute, event)}
            readonly={Boolean(attribute.fixedValue)}
          />
        {/if}
      </label>
    </div>
  {/each}

  {#each group.children as child, index}
    {#if child.occurrence.maxOccurs !== 1 || child.occurrence.minOccurs > 1}
      <RepeatableField node={child} {path} childIndex={index} />
    {:else}
      <SchemaNode node={child} path={`${path}.${child.name}`} />
    {/if}
  {/each}
</fieldset>
