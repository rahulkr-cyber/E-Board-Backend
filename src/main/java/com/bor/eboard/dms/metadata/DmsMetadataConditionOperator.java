package com.bor.eboard.dms.metadata;

public enum DmsMetadataConditionOperator {
    EQUALS("Equals", true),
    NOT_EQUALS("Does not equal", true),
    IN("In comma-separated values", true),
    NOT_IN("Not in comma-separated values", true),
    EMPTY("Is empty", false),
    NOT_EMPTY("Is not empty", false),
    TRUE("Is true", false),
    FALSE("Is false", false);

    private final String label;
    private final boolean requiresValue;

    DmsMetadataConditionOperator(String label, boolean requiresValue) {
        this.label = label;
        this.requiresValue = requiresValue;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresValue() {
        return requiresValue;
    }
}
