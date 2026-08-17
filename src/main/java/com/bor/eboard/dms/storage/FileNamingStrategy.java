package com.bor.eboard.dms.storage;

import com.bor.eboard.common.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates opaque DMS storage keys. Original file names are never used as
 * physical file names.
 */
@Component
public class FileNamingStrategy {

    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,20}");

    public String createStorageKey(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException("File name is missing");
        }

        LocalDate today = LocalDate.now();
        String extension = extractSafeExtension(originalFilename);
        String generatedName = UUID.randomUUID().toString();
        if (!extension.isBlank()) {
            generatedName += "." + extension;
        }

        return String.format("%04d/%02d/%s",
                today.getYear(), today.getMonthValue(), generatedName);
    }

    private String extractSafeExtension(String originalFilename) {
        String normalized = originalFilename.replace('\\', '/');
        String nameOnly = normalized.substring(normalized.lastIndexOf('/') + 1);
        int dot = nameOnly.lastIndexOf('.');
        if (dot < 0 || dot == nameOnly.length() - 1) {
            return "";
        }

        String extension = nameOnly.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(extension).matches() ? extension : "";
    }
}
