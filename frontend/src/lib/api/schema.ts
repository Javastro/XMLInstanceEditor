import type { PreloadedSchemaInfo, SchemaDocument, ValidationMessage } from '$lib/types/schema';

export interface SchemaParseRequest {
  xsd: string;
  systemId?: string | null;
}

export interface XmlValidationRequest {
  xsd: string;
  xml: string;
}

export interface XmlGenerationRequest {
  schema: SchemaDocument;
  values: Record<string, unknown>;
}

export interface XmlImportRequest {
  xsd: string;
  systemId?: string | null;
  xml: string;
  rootName?: string | null;
  rootTypeName?: string | null;
  rootTypeNamespaceUri?: string | null;
}

export interface UnknownXmlContent {
  path: string;
  xml: string;
}

export interface XmlImportResponse {
  rootName: string;
  rootTypeName?: string | null;
  rootTypeNamespaceUri?: string | null;
  values: Record<string, unknown>;
  repeatCounts: Record<string, number>;
  unknownContent: UnknownXmlContent[];
}

export interface PreloadedSchemaFetchResponse {
  xsd: string;
  systemId?: string | null;
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const details = await response.text();
    throw new Error(details || `Request failed with status ${response.status}`);
  }
  return (await response.json()) as T;
}

export async function parseSchema(request: SchemaParseRequest): Promise<SchemaDocument> {
  const response = await fetch('/api/schema/parse', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  return readJson<SchemaDocument>(response);
}

export async function validateXml(request: XmlValidationRequest): Promise<{ valid: boolean; messages: ValidationMessage[] }> {
  const response = await fetch('/api/instance/validate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  return readJson<{ valid: boolean; messages: ValidationMessage[] }>(response);
}

export async function generateXml(request: XmlGenerationRequest): Promise<{ xml: string }> {
  const response = await fetch('/api/instance/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  return readJson<{ xml: string }>(response);
}

export async function listPreloadedSchemas(): Promise<PreloadedSchemaInfo[]> {
  const response = await fetch('/api/schema/preloaded');
  return readJson<PreloadedSchemaInfo[]>(response);
}

export async function fetchPreloadedSchema(namespaceUri: string): Promise<PreloadedSchemaFetchResponse> {
  const response = await fetch('/api/schema/preloaded/fetch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ namespaceUri })
  });
  return readJson<PreloadedSchemaFetchResponse>(response);
}

export async function importXmlInstance(request: XmlImportRequest): Promise<XmlImportResponse> {
  const response = await fetch('/api/instance/import', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  return readJson<XmlImportResponse>(response);
}
