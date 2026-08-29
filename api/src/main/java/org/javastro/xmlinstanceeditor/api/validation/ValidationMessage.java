package org.javastro.xmlinstanceeditor.api.validation;

public record ValidationMessage(
    ValidationSeverity severity,
    String message,
    int line,
    int column
) {
}
