import { writable } from 'svelte/store';
import type { ValidationMessage } from '$lib/types/schema';

export const validationMessages = writable<ValidationMessage[]>([]);
export const validationSummary = writable<string>('');
