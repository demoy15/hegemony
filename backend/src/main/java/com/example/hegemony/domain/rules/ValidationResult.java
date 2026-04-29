package com.example.hegemony.domain.rules;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ValidationResult {
    private final List<String> errors;
    private final List<ValidationReasonCode> reasonCodes;

    private ValidationResult(List<String> errors, List<ValidationReasonCode> reasonCodes) {
        this.errors = errors;
        this.reasonCodes = reasonCodes;
    }

    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList(), Collections.emptyList());
    }

    public static ValidationResult invalid(List<String> errors, List<ValidationReasonCode> reasonCodes) {
        return new ValidationResult(new ArrayList<>(errors), new ArrayList<>(reasonCodes));
    }

    public static ValidationResult invalid(ValidationReasonCode code, String error) {
        return new ValidationResult(List.of(error), List.of(code));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
