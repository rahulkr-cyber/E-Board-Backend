package com.bor.eboard.dms.constants;

/**
 * Permission codes used by the DMS module through the existing RBAC system.
 */
public final class DmsPermissions {

    private DmsPermissions() {
    }

    public static final String VIEW = "DMS_VIEW";
    public static final String CREATE = "DMS_CREATE";
    public static final String UPDATE = "DMS_UPDATE";
    public static final String DELETE = "DMS_DELETE";
    public static final String ADMIN = "DMS_ADMIN";
    public static final String DOWNLOAD = "DMS_DOWNLOAD";
    public static final String UPLOAD = "DMS_UPLOAD";
    public static final String SHARE = "DMS_SHARE";
    public static final String METADATA_ADMIN = "DMS_METADATA_ADMIN";
    public static final String DOCUMENT_TYPE_ADMIN = "DMS_DOCUMENTTYPE_ADMIN";
    public static final String MASTER_DATA_ADMIN = "DMS_MASTERDATA_ADMIN";
    public static final String AUDIT_VIEW = "DMS_AUDIT_VIEW";
}
