package com.bor.eboard.correspondence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfo {

    private UUID id;
    private String originalFileName;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String checksum;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}
