package com.bor.eboard.dms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration owned exclusively by the DMS module.
 */
@ConfigurationProperties(prefix = "dms")
public class DmsProperties {

    private boolean enabled = true;
    private final Storage storage = new Storage();
    private final Upload upload = new Upload();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public Upload getUpload() {
        return upload;
    }

    public static class Storage {
        private String provider = "local";
        private String localBasePath = "D:/E-Board/uploads/dms";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getLocalBasePath() {
            return localBasePath;
        }

        public void setLocalBasePath(String localBasePath) {
            this.localBasePath = localBasePath;
        }
    }

    public static class Upload {
        private long maxFileSizeBytes = 20L * 1024L * 1024L;
        private int maxMetadataBytes = 256 * 1024;
        private boolean verifyChecksumOnDownload = true;
        private List<String> allowedExtensions = new ArrayList<>(List.of(
                "pdf", "doc", "docx", "xls", "xlsx", "csv", "txt",
                "jpg", "jpeg", "png", "tif", "tiff"));
        private List<String> allowedMimeTypes = new ArrayList<>(List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv",
                "text/plain",
                "image/jpeg",
                "image/png",
                "image/tiff"));

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public int getMaxMetadataBytes() {
            return maxMetadataBytes;
        }

        public void setMaxMetadataBytes(int maxMetadataBytes) {
            this.maxMetadataBytes = maxMetadataBytes;
        }

        public boolean isVerifyChecksumOnDownload() {
            return verifyChecksumOnDownload;
        }

        public void setVerifyChecksumOnDownload(boolean verifyChecksumOnDownload) {
            this.verifyChecksumOnDownload = verifyChecksumOnDownload;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions == null
                    ? new ArrayList<>()
                    : new ArrayList<>(allowedExtensions);
        }

        public List<String> getAllowedMimeTypes() {
            return allowedMimeTypes;
        }

        public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
            this.allowedMimeTypes = allowedMimeTypes == null
                    ? new ArrayList<>()
                    : new ArrayList<>(allowedMimeTypes);
        }
    }
}
