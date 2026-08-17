package com.bor.eboard.dms.masterdata;

public enum DmsMasterSourceType {
    STATIC_LIST("Static List"),
    DATABASE_QUERY("Database Query"),
    STORED_PROCEDURE("Stored Procedure"),
    REST_API("REST API"),
    GOVERNMENT_API("Government API"),
    LDAP("LDAP");

    private final String label;

    DmsMasterSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
