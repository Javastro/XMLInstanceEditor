export type NodeKind = 'ELEMENT' | 'ATTRIBUTE' | 'GROUP' | 'TEXT' | 'ANY';
export type Compositor = 'SEQUENCE' | 'CHOICE' | 'ALL';

export interface Occurrence {
  minOccurs: number;
  maxOccurs: number | null;
}

export interface FacetConstraints {
  enumeration: string[];
  pattern?: string | null;
  minLength?: number | null;
  maxLength?: number | null;
}

export interface SimpleType {
  baseType: string;
  constraints: FacetConstraints;
}

export interface AttributeNode {
  name: string;
  type: SimpleType;
  required: boolean;
  defaultValue?: string | null;
  fixedValue?: string | null;
}

export interface ComplexType {
  compositor: Compositor;
  children: SchemaNode[];
  attributes: AttributeNode[];
  textContent?: SimpleType | null;
  recursive: boolean;
}

export type TypeDefinition = SimpleType | ComplexType;

export interface SchemaNode {
  name: string;
  kind: NodeKind;
  occurrence: Occurrence;
  nillable: boolean;
  defaultValue?: string | null;
  fixedValue?: string | null;
  namespaceUri?: string | null;
  abstractElement: boolean;
  substitutionOptions: SchemaNode[];
  typeDefinition: TypeDefinition;
}

export interface SchemaTypeOption {
  name: string;
  namespaceUri?: string | null;
  typeDefinition: TypeDefinition;
}

export interface SchemaDocument {
  targetNamespace?: string | null;
  namespaces: Record<string, string>;
  roots: SchemaNode[];
  availableRootTypes: SchemaTypeOption[];
  sourceSystemId?: string | null;
}

export interface PreloadedSchemaInfo {
  id: string;
  label: string;
}

export interface ValidationMessage {
  severity: 'WARNING' | 'ERROR' | 'FATAL';
  message: string;
  line: number;
  column: number;
}
