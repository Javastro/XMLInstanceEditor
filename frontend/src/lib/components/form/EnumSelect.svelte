<script lang="ts">
  export let options: string[] = [];
  export let value = '';
</script>

<select bind:value>
  {#each options as option}
    <option value={option}>{option}</option>
  {/each}
</select>
