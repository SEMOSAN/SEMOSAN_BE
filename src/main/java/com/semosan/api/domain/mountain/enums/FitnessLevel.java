package com.semosan.api.domain.mountain.enums;

public enum FitnessLevel {
    ENTRY("입문"),
    BEGINNER("초"),
    INTERMEDIATE("중"),
    ADVANCED("상");

    private final String label;

    FitnessLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
