package com.bor.eboard.dms.storage;

import com.bor.eboard.common.exception.FileStorageException;
import com.bor.eboard.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Local-disk implementation for DMS content only. Files are placed under the
 * configured DMS root and never share the existing attachment storage path.
 */
@Component
public class LocalStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageProvider.class);

    private final StorageConfigurationResolver configurationResolver;
    private final FileNamingStrategy fileNamingStrategy;

    public LocalStorageProvider(
            StorageConfigurationResolver configurationResolver,
            FileNamingStrategy fileNamingStrategy) {
        this.configurationResolver = configurationResolver;
        this.fileNamingStrategy = fileNamingStrategy;
    }

    @Override
    public String providerCode() {
        return StorageConfigurationResolver.LOCAL_PROVIDER;
    }

    @Override
    public String store(InputStream inputStream,
                        long contentLength,
                        String originalFilename,
                        String contentType) throws IOException {
        if (inputStream == null) {
            throw new ValidationException("DMS file content is missing");
        }
        if (contentLength < -1) {
            throw new ValidationException("DMS file content length is invalid");
        }

        String storageKey = fileNamingStrategy.createStorageKey(originalFilename);
        Path target = resolveStoragePath(storageKey);
        rejectSymbolicLinks(target.getParent());
        Files.createDirectories(target.getParent());
        rejectSymbolicLinks(target.getParent());

        Path temporaryFile = Files.createTempFile(target.getParent(), ".dms-upload-", ".tmp");
        try {
            long copied = Files.copy(inputStream, temporaryFile,
                    StandardCopyOption.REPLACE_EXISTING);
            if (contentLength >= 0 && copied != contentLength) {
                throw new FileStorageException(
                        "DMS file size changed during storage");
            }
            rejectSymbolicLinks(temporaryFile);
            moveIntoPlace(temporaryFile, target);
            log.debug("Stored DMS object using provider={} key={} size={} contentType={}",
                    providerCode(), storageKey, copied, contentType);
            return storageKey;
        } catch (RuntimeException | IOException ex) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolveStoragePath(storageKey);
        rejectSymbolicLinks(path);
        try {
            UrlResource resource = new UrlResource(path.toUri());
            if (!resource.exists()
                    || !resource.isReadable()
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new FileStorageException(
                        "DMS stored object is missing or unreadable");
            }
            return resource;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read DMS stored object", ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path path = resolveStoragePath(storageKey);
        rejectSymbolicLinks(path);
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && Files.isReadable(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path path = resolveStoragePath(storageKey);
        rejectSymbolicLinks(path);
        Files.deleteIfExists(path);
        log.debug("Deleted DMS object using provider={} key={}", providerCode(), storageKey);
    }

    @Override
    public StorageProviderHealth health() {
        try {
            Path root = resolveRootPath();
            rejectSymbolicLinks(root);
            Files.createDirectories(root);
            rejectSymbolicLinks(root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                return StorageProviderHealth.unhealthy(
                        "Configured DMS local storage path is not a directory");
            }
            if (!Files.isReadable(root) || !Files.isWritable(root)) {
                return StorageProviderHealth.unhealthy(
                        "Configured DMS local storage path must be readable and writable");
            }
            return StorageProviderHealth.healthy(
                    "DMS local storage is available");
        } catch (RuntimeException | IOException ex) {
            log.warn("DMS local storage health check failed", ex);
            return StorageProviderHealth.unhealthy(ex.getMessage());
        }
    }

    private Path resolveStoragePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ValidationException("DMS storage key is missing");
        }

        Path root = resolveRootPath();
        Path resolved = root.resolve(storageKey.replace('\\', '/'))
                .toAbsolutePath()
                .normalize();
        if (!resolved.startsWith(root)) {
            throw new ValidationException("Invalid DMS storage key");
        }
        return resolved;
    }

    private Path resolveRootPath() {
        String basePath = configurationResolver.resolve(providerCode()).effectiveBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new FileStorageException("DMS local storage base path is not configured");
        }
        Path root = Path.of(basePath).toAbsolutePath().normalize();
        rejectSymbolicLinks(root);
        return root;
    }

    private void rejectSymbolicLinks(Path path) {
        if (path == null) {
            throw new FileStorageException("DMS local storage path is invalid");
        }
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path segment : absolute) {
            current = current == null ? segment : current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.isSymbolicLink(current)) {
                throw new FileStorageException(
                        "Symbolic links are not allowed in DMS local storage paths");
            }
        }
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }
}
