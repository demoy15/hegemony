package com.example.hegemony.web;

import com.example.hegemony.application.composer.ActionPreviewDelta;
import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.List;

public record PreviewActionResponse(
        boolean accepted,
        List<String> errors,
        List<ValidationReasonCode> reasonCodes,
        ActionPreviewDelta delta,
        List<String> supportNotes
) {
}
