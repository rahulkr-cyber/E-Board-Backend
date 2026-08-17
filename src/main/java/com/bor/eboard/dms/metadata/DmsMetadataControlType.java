package com.bor.eboard.dms.metadata;

public enum DmsMetadataControlType {
    TEXTBOX("Textbox", false, true, false, false, false),
    TEXTAREA("Textarea", false, true, false, false, false),
    NUMBER("Number", false, false, true, false, false),
    DECIMAL("Decimal", false, false, true, false, false),
    DATE("Date", false, false, false, true, false),
    DATE_TIME("Date Time", false, false, false, true, false),
    DROPDOWN("Dropdown", true, false, false, false, false),
    MULTI_SELECT("Multi Select", true, false, false, false, true),
    CHECKBOX("Checkbox", false, false, false, false, false),
    RADIO("Radio", true, false, false, false, false),
    AUTOCOMPLETE("Autocomplete", true, true, false, false, false),
    FILE_UPLOAD("File Upload", false, false, false, false, false),
    HIDDEN("Hidden", false, true, false, false, false),
    TAG_INPUT("Tag Input", false, true, false, false, true);

    private final String label;
    private final boolean supportsOptions;
    private final boolean supportsLength;
    private final boolean supportsNumericRange;
    private final boolean supportsDateConstraint;
    private final boolean multipleValues;

    DmsMetadataControlType(
            String label,
            boolean supportsOptions,
            boolean supportsLength,
            boolean supportsNumericRange,
            boolean supportsDateConstraint,
            boolean multipleValues) {
        this.label = label;
        this.supportsOptions = supportsOptions;
        this.supportsLength = supportsLength;
        this.supportsNumericRange = supportsNumericRange;
        this.supportsDateConstraint = supportsDateConstraint;
        this.multipleValues = multipleValues;
    }

    public String getLabel() {
        return label;
    }

    public boolean supportsOptions() {
        return supportsOptions;
    }

    public boolean supportsLength() {
        return supportsLength;
    }

    public boolean supportsNumericRange() {
        return supportsNumericRange;
    }

    public boolean supportsDateConstraint() {
        return supportsDateConstraint;
    }

    public boolean supportsMultipleValues() {
        return multipleValues;
    }
}
