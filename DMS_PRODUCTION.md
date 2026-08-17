# Configurable DMS — Production Deployment Notes

The DMS module is independent from the existing attachment subsystem. Its storage root, database tables, permissions, APIs and Angular routes are DMS-owned.

## Required environment configuration

| Variable | Default | Purpose |
|---|---:|---|
| `DMS_ENABLED` | `true` | Enables DMS runtime health reporting. |
| `DMS_STORAGE_PROVIDER` | `local` | Selects the active DMS storage provider. |
| `DMS_LOCAL_STORAGE_BASE_PATH` | `D:/E-Board/uploads/dms` | DMS-only local storage root. Do not point this to existing attachment storage. |
| `DMS_MAX_FILE_SIZE_BYTES` | `20971520` | Maximum file size accepted by DMS service validation. Keep this at or below Spring multipart limits. |
| `DMS_MAX_METADATA_BYTES` | `262144` | Maximum serialized metadata payload per document. |
| `DMS_ALLOWED_EXTENSIONS` | `pdf,doc,docx,xls,xlsx,csv,txt,jpg,jpeg,png,tif,tiff` | Comma-separated upload extension allowlist. |
| `DMS_ALLOWED_MIME_TYPES` | common document/image MIME types | Comma-separated MIME allowlist. |
| `DMS_VERIFY_CHECKSUM_ON_DOWNLOAD` | `true` | Verifies stored length and SHA-256 before download. Disable only after an explicit performance assessment. |

## Deployment checks

1. Apply Flyway migrations through `V34`.
2. Confirm the DMS storage root is on a dedicated volume and is not a symbolic link.
3. Confirm the application user has read/write permission only for the DMS root it needs.
4. Call `GET /api/v1/dms/admin/health` as `DMS_ADMIN` and resolve every `UNHEALTHY` result.
5. Call `GET /api/v1/dms/storage/health` and verify the selected provider is operational.
6. Upload and download a representative PDF, Office document and image.
7. Confirm oversized, executable and extension-mismatched files are rejected.
8. Confirm a stored-file checksum mismatch blocks download and creates an operational alert in application logs.
9. Confirm backup jobs include DMS database tables and the independent DMS storage root as one recoverable set.
10. Confirm existing Diary, Letter, Workflow and Dispatch attachment upload/download behavior is unchanged.

## Operational recommendations

- Keep `DMS_VERIFY_CHECKSUM_ON_DOWNLOAD=true` for high-integrity deployments. For very large future limits or remote providers, benchmark the additional read before changing it.
- Monitor storage capacity, storage health, failed uploads and integrity-verification failures.
- Restore database and storage from the same backup point to preserve version/storage-key consistency.
- Do not manually rename or replace objects under the DMS storage root.
- Use object-level sharing with expiry dates for temporary access.
