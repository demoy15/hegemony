package com.example.hegemony.web;

import java.util.List;

public record ApiErrorResponse(String message, List<String> details) {
}
