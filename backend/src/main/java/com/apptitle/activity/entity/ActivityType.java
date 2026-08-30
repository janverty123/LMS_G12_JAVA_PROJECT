package com.apptitle.activity.entity;

public enum ActivityType {
    WRITTEN_ACTIVITY("Written Activity"),
    PERFORMANCE_TASK("Performance Task"),
    TEST("Test");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
