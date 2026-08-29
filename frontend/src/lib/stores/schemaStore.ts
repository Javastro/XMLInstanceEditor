import { derived, writable } from 'svelte/store';
import type { SchemaDocument } from '$lib/types/schema';

export const schemaStore = writable<SchemaDocument | null>(null);

export const rootNodes = derived(schemaStore, ($schema) => $schema?.roots ?? []);
