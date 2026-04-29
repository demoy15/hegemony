package com.example.hegemony.application.composer;

import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.List;

public record ActionPreviewResult(
        boolean accepted,
        List<String> errors,
        List<ValidationReasonCode> reasonCodes,
        ActionPreviewDelta delta,
        List<String> supportNotes
) {
}
