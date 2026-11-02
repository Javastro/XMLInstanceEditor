<script lang="ts">
  import { validationMessages } from '$lib/stores/validationStore';
</script>

<section>
  <h2>Validation</h2>
  {#if $validationMessages.length === 0}
    <p class="banner ok" role="status">No validation messages.</p>
  {:else}
    <ul class="messages" role="alert">
      {#each $validationMessages as message}
        <li class={message.severity === 'WARNING' ? 'warning' : 'error'}>[{message.severity}] {message.message}</li>
      {/each}
    </ul>
  {/if}
</section>
