package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.FileStorageException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.config.DmsProperties;
import com.bor.eboard.dms.dto.DmsUploadPolicyResponse;
import com.bor.eboard.dms.service.DmsFilePolicyService;
import com.bor.eboard.dms.storage.ChecksumUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DmsFilePolicyServiceImpl implements DmsFilePolicyService {

    private static final String OCTET_STREAM = "application/octet-stream";
    private static final int SIGNATURE_SAMPLE_BYTES = 8192;

    private final DmsProperties properties;
    private final ObjectMapper objectMapper;

    public DmsFilePolicyServiceImpl(DmsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidatedFile validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new ValidationException("DMS document file is required");
        }

        DmsProperties.Upload policy = properties.getUpload();
        if (policy.getMaxFileSizeBytes() <= 0) {
            throw new FileStorageException("DMS upload size policy is not configured correctly");
        }
        if (file.getSize() > policy.getMaxFileSizeBytes()) {
            throw new ValidationException(
                    "DMS file exceeds the maximum allowed size of "
                            + formatBytes(policy.getMaxFileSizeBytes()));
        }

        String originalFileName = safeOriginalFilename(file.getOriginalFilename());
        String extension = extension(originalFileName);
        Set<String> allowedExtensions = normalized(policy.getAllowedExtensions(), true);
        if (allowedExtensions.isEmpty()) {
            throw new FileStorageException("DMS allowed file extensions are not configured");
        }
        if (!allowedExtensions.contains(extension)) {
            throw new ValidationException(
                    "DMS file type ." + extension + " is not allowed");
        }

        Set<String> allowedMimeTypes = normalized(policy.getAllowedMimeTypes(), false);
        String submittedMimeType = normalizeMimeType(file.getContentType());
        if (!submittedMimeType.isBlank()
                && !OCTET_STREAM.equals(submittedMimeType)
                && !allowedMimeTypes.contains(submittedMimeType)) {
            throw new ValidationException(
                    "DMS MIME type " + submittedMimeType + " is not allowed");
        }

        byte[] sample = readSample(file);
        validateSignature(extension, sample);
        String checksum = checksum(file);
        String effectiveMimeType = submittedMimeType.isBlank() || OCTET_STREAM.equals(submittedMimeType)
                ? canonicalMimeType(extension)
                : submittedMimeType;

        return new ValidatedFile(
                originalFileName,
                extension,
                effectiveMimeType,
                file.getSize(),
                checksum);
    }

    @Override
    public void validateMetadataPayload(Map<String, Object> metadata) {
        int maximum = properties.getUpload().getMaxMetadataBytes();
        if (maximum <= 0) {
            throw new FileStorageException("DMS metadata size policy is not configured correctly");
        }
        try {
            int size = objectMapper.writeValueAsBytes(metadata == null ? Map.of() : metadata).length;
            if (size > maximum) {
                throw new ValidationException(
                        "DMS metadata exceeds the maximum allowed size of " + formatBytes(maximum));
            }
        } catch (JsonProcessingException ex) {
            throw new ValidationException("DMS metadata payload cannot be serialized");
        }
    }

    @Override
    public boolean verifyStoredContent(
            Resource resource,
            long expectedSize,
            String expectedChecksum) {
        if (!properties.getUpload().isVerifyChecksumOnDownload()) {
            return false;
        }
        try {
            long actualSize = resource.contentLength();
            if (actualSize != expectedSize) {
                throw new FileStorageException("DMS stored file failed integrity verification");
            }
            String actualChecksum;
            try (InputStream inputStream = resource.getInputStream()) {
                actualChecksum = ChecksumUtils.sha256(inputStream);
            }
            if (expectedChecksum == null
                    || !expectedChecksum.equalsIgnoreCase(actualChecksum)) {
                throw new FileStorageException("DMS stored file failed integrity verification");
            }
            return true;
        } catch (IOException ex) {
            throw new FileStorageException("Unable to verify DMS stored file integrity", ex);
        }
    }

    @Override
    public DmsUploadPolicyResponse getPolicy() {
        DmsProperties.Upload policy = properties.getUpload();
        return new DmsUploadPolicyResponse(
                policy.getMaxFileSizeBytes(),
                policy.getMaxMetadataBytes(),
                List.copyOf(normalized(policy.getAllowedExtensions(), true)),
                List.copyOf(normalized(policy.getAllowedMimeTypes(), false)),
                policy.isVerifyChecksumOnDownload());
    }

    private String safeOriginalFilename(String original) {
        if (original == null || original.isBlank()) {
            throw new ValidationException("DMS file name is required");
        }
        String normalized = original.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        fileName = fileName.trim();
        if (fileName.isBlank()) {
            throw new ValidationException("DMS file name is required");
        }
        if (fileName.length() > 500) {
            throw new ValidationException("DMS file name cannot exceed 500 characters");
        }
        if (fileName.chars().anyMatch(character -> Character.isISOControl(character))) {
            throw new ValidationException("DMS file name contains invalid control characters");
        }
        return fileName;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            throw new ValidationException("DMS file must have an allowed extension");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readSample(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(SIGNATURE_SAMPLE_BYTES);
        } catch (IOException ex) {
            throw new FileStorageException("Unable to inspect DMS file content", ex);
        }
    }

    private String checksum(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return ChecksumUtils.sha256(inputStream);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to calculate DMS file checksum", ex);
        }
    }

    private void validateSignature(String extension, byte[] sample) {
        if (sample.length == 0) {
            throw new ValidationException("DMS document file is empty");
        }
        if (isWindowsExecutable(sample)
                || startsWith(sample, new byte[]{0x7f, 'E', 'L', 'F'})
                || startsWith(sample, new byte[]{'#', '!'})) {
            throw new ValidationException("Executable content is not allowed in DMS uploads");
        }

        boolean valid = switch (extension) {
            case "pdf" -> startsWith(sample, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "jpg", "jpeg" -> startsWith(sample,
                    new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
            case "png" -> startsWith(sample,
                    new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a});
            case "tif", "tiff" -> startsWith(sample, new byte[]{'I', 'I', 0x2a, 0x00})
                    || startsWith(sample, new byte[]{'M', 'M', 0x00, 0x2a});
            case "doc", "xls" -> startsWith(sample, new byte[]{
                    (byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                    (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1});
            case "docx", "xlsx" -> isZip(sample);
            case "txt", "csv" -> isText(sample);
            default -> true;
        };
        if (!valid) {
            throw new ValidationException(
                    "DMS file content does not match the ." + extension + " extension");
        }
    }

    private boolean isWindowsExecutable(byte[] sample) {
        return startsWith(sample, new byte[]{'M', 'Z'});
    }

    private boolean isZip(byte[] sample) {
        return startsWith(sample, new byte[]{'P', 'K', 0x03, 0x04})
                || startsWith(sample, new byte[]{'P', 'K', 0x05, 0x06})
                || startsWith(sample, new byte[]{'P', 'K', 0x07, 0x08});
    }

    private boolean isText(byte[] sample) {
        for (byte value : sample) {
            if (value == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length
                && Arrays.equals(Arrays.copyOf(value, prefix.length), prefix);
    }

    private Set<String> normalized(List<String> values, boolean stripDot) {
        Set<String> result = new LinkedHashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (stripDot && normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "";
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(';');
        return separator >= 0 ? normalized.substring(0, separator).trim() : normalized;
    }

    private String canonicalMimeType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "tif", "tiff" -> "image/tiff";
            default -> OCTET_STREAM;
        };
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
