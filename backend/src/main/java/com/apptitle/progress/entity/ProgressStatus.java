package com.apptitle.progress.entity;

public enum ProgressStatus {
    ON_TRACK("On Track"),
    NEEDS_ATTENTION("Needs Attention"),
    AT_RISK("At Risk");

    private final String label;
    ProgressStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
