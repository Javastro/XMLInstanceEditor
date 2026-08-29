import { derived, writable } from 'svelte/store';
import type { SchemaDocument } from '$lib/types/schema';
import { schemaStore } from './schemaStore';

export const instanceValues = writable<Record<string, unknown>>({});
export const expandedOptional = writable<Set<string>>(new Set());
export const repeatCounts = writable<Record<string, number>>({});
export const previewRootName = writable<string | null>(null);
export const previewRootType = writable<{ name: string; namespaceUri?: string | null } | null>(null);

export const xmlPreview = derived(
  [schemaStore, instanceValues, previewRootName, previewRootType],
  ([$schema, $values, $previewRootName, $previewRootType]) =>
    buildPreview($schema, $values, $previewRootName, $previewRootType)
);

function buildPreview(
  schema: SchemaDocument | null,
  values: Record<string, unknown>,
  selectedRootName: string | null,
  selectedRootType: { name: string; namespaceUri?: string | null } | null
): string {
  const rootName = selectedRootName ?? schema?.roots[0]?.name ?? null;
  if (!rootName) {
    return '<!-- Load an XSD to generate XML preview -->';
  }

  const attrs = buildRootAttributes(values, rootName, selectedRootType);
  const children: string[] = [];

  for (const [path, value] of Object.entries(values)) {
    if (value === undefined || value === null || value === '') continue;
    if (!path.startsWith(`${rootName}.`)) continue;

    const localPath = path.slice(rootName.length + 1);
    if (localPath.startsWith('@')) continue;

    const elementName = extractElementName(localPath);
    children.push(`<${elementName}>${escapeXml(String(value))}</${elementName}>`);
  }

  const attrsPart = attrs.length > 0 ? ` ${attrs.join(' ')}` : '';
  const body = children.length > 0 ? children.join('') : '';
  return `<${rootName}${attrsPart}>${body}</${rootName}>`;
}

function buildRootAttributes(
  values: Record<string, unknown>,
  rootName: string,
  selectedRootType: { name: string; namespaceUri?: string | null } | null
): string[] {
  const attrs: string[] = [];
  for (const [path, value] of Object.entries(values)) {
    if (value === undefined || value === null || value === '') continue;
    if (!path.startsWith(`${rootName}.`)) continue;

    const localPath = path.slice(rootName.length + 1);
    if (!localPath.startsWith('@')) continue;

    const attrName = localPath.slice(1);
    attrs.push(`${attrName}="${escapeXml(String(value))}"`);
  }

  if (!selectedRootType) {
    return attrs;
  }

  attrs.push('xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"');
  if (selectedRootType.namespaceUri) {
    attrs.push(`xmlns:${typePrefix(selectedRootType.name)}="${escapeXml(selectedRootType.namespaceUri)}"`);
  }
  attrs.push(`xsi:type="${formatXsiType(selectedRootType.name, selectedRootType.namespaceUri)}"`);
  return attrs;
}

function formatXsiType(name: string, namespaceUri?: string | null): string {
  if (!namespaceUri) {
    return escapeXml(name);
  }
  const localName = name.includes(':') ? name.slice(name.indexOf(':') + 1) : name;
  return `${typePrefix(name)}:${escapeXml(localName)}`;
}

function typePrefix(name: string): string {
  if (name.includes(':')) {
    return name.slice(0, name.indexOf(':'));
  }
  return 'tns';
}

function extractElementName(path: string): string {
  const last = path.split('.').at(-1) ?? path;
  return last.replace(/\[\d+]/g, '');
}

function escapeXml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}
