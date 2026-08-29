package org.javastro.xmlinstanceeditor.api.validation;

import java.util.List;

public record ValidationResult(boolean valid, List<ValidationMessage> messages) {
    public ValidationResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
