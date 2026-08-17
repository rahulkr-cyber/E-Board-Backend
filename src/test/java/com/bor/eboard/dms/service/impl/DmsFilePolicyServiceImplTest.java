package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.FileStorageException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.config.DmsProperties;
import com.bor.eboard.dms.service.DmsFilePolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmsFilePolicyServiceImplTest {

    private DmsProperties properties;
    private DmsFilePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new DmsProperties();
        properties.getUpload().setMaxFileSizeBytes(1024);
        properties.getUpload().setMaxMetadataBytes(64);
        service = new DmsFilePolicyServiceImpl(properties, new ObjectMapper());
    }

    @Test
    void acceptsValidPdfAndCalculatesChecksum() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "order.pdf",
                "application/pdf",
                "%PDF-1.7\nvalid".getBytes(StandardCharsets.US_ASCII));

        DmsFilePolicyService.ValidatedFile validated = service.validateUpload(file);

        assertThat(validated.originalFileName()).isEqualTo("order.pdf");
        assertThat(validated.extension()).isEqualTo("pdf");
        assertThat(validated.mimeType()).isEqualTo("application/pdf");
        assertThat(validated.checksumSha256()).hasSize(64);
    }

    @Test
    void rejectsOversizedFile() {
        properties.getUpload().setMaxFileSizeBytes(4);
        MockMultipartFile file = new MockMultipartFile(
                "file", "order.pdf", "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.validateUpload(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void rejectsExtensionAndContentMismatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "order.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.validateUpload(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsExecutableDisguisedAsText() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain",
                new byte[]{'M', 'Z', 0, 1, 2});

        assertThatThrownBy(() -> service.validateUpload(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Executable content");
    }

    @Test
    void enforcesMetadataSizeLimit() {
        assertThatThrownBy(() -> service.validateMetadataPayload(Map.of(
                "large", "x".repeat(100))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("metadata exceeds");
    }

    @Test
    void verifiesStoredSizeAndChecksum() {
        byte[] content = "%PDF-1.7\nvalid".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "file", "order.pdf", "application/pdf", content);
        DmsFilePolicyService.ValidatedFile validated = service.validateUpload(file);

        assertThatCode(() -> service.verifyStoredContent(
                new ByteArrayResource(content),
                content.length,
                validated.checksumSha256()))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> service.verifyStoredContent(
                new ByteArrayResource("changed".getBytes(StandardCharsets.UTF_8)),
                content.length,
                validated.checksumSha256()))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("integrity verification");
    }
}
