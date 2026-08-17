package com.bor.eboard.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingDocumentResponse {
    private UUID id;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private String documentCategory;
}
