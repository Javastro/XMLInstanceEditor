<script lang="ts">
  import { validationMessages } from '$lib/stores/validationStore';
</script>

<section>
  <h2>Validation</h2>
  {#if $validationMessages.length === 0}
    <p>No validation messages.</p>
  {:else}
    <ul>
      {#each $validationMessages as message}
        <li>[{message.severity}] {message.message}</li>
      {/each}
    </ul>
  {/if}
</section>
