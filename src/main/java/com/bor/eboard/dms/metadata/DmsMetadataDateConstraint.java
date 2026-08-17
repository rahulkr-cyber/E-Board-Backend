package com.bor.eboard.dms.metadata;

public enum DmsMetadataDateConstraint {
    NONE("No restriction"),
    PAST("Past date only"),
    PAST_OR_PRESENT("Past or present date"),
    FUTURE("Future date only"),
    FUTURE_OR_PRESENT("Future or present date");

    private final String label;

    DmsMetadataDateConstraint(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
