package com.example.hegemony.domain.model;

public enum PolicyCourse {
    A,
    B,
    C;

    public boolean isAdjacentTo(PolicyCourse other) {
        return Math.abs(ordinal() - other.ordinal()) == 1;
    }
}
