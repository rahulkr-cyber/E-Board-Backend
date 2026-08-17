package com.bor.eboard.dms.storage;

import com.bor.eboard.common.exception.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 checksum helper for DMS-owned content.
 */
public final class ChecksumUtils {

    private ChecksumUtils() {
    }

    public static String sha256(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256(inputStream);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to calculate DMS file checksum", ex);
        }
    }

    public static String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException ex) {
            throw new FileStorageException("Failed to calculate DMS file checksum", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new FileStorageException("SHA-256 is unavailable", ex);
        }
    }
}
