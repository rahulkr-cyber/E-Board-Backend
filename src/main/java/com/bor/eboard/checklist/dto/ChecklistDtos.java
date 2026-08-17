package com.bor.eboard.checklist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO namespace for the independent metadata-driven Checklist Engine. */
public final class ChecklistDtos {
    private ChecklistDtos() { }

    public static final String CONTROL_TYPE_PATTERN =
            "DOCUMENT_UPLOAD|YES_NO|CHECKBOX|TEXTBOX|TEXT_AREA|NUMBER|DATE|DATE_TIME|" +
            "DROPDOWN|MULTI_SELECT|RADIO_BUTTON|LABEL|REMARKS|AUTO_VALUE|HYPERLINK|SIGNATURE";

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TemplateRequest {
        @NotBlank @Size(max = 80) private String code;
        @NotBlank @Size(max = 200) private String name;
        private String description;
        private Boolean allowForwardIfIncomplete;
        private Boolean active;
        @Valid private List<ItemRequest> items = new ArrayList<>();
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ItemRequest {
        @NotBlank @Size(max = 200) private String name;
        @Size(max = 200) private String label;
        private String description;
        @Pattern(regexp = CONTROL_TYPE_PATTERN, message = "Unsupported checklist control type")
        private String controlType;
        @Size(max = 300) private String placeholder;
        private String helpText;
        private Boolean mandatory;
        private Boolean readOnly;
        private Boolean visible;
        private Object defaultValue;
        private Map<String, Object> validationRules = new LinkedHashMap<>();
        @Min(0) private Integer minimumLength;
        @Min(0) private Integer maximumLength;
        @Size(max = 1000) private String regexPattern;
        private List<@Size(max = 500) String> options = new ArrayList<>();
        @Size(max = 1000) private String autoValueExpression;
        private Integer parentItemSequence;
        @Size(max = 40) private String visibilityConditionOperator;
        private Object visibilityConditionValue;
        @Size(max = 40) private String mandatoryConditionOperator;
        private Object mandatoryConditionValue;
        private Boolean active;
        @NotNull @Min(1) @Max(10000) private Integer sequence;
        private Boolean remarksRequired;
        private Boolean allowMultipleAttachments;
        @Size(max = 500) private String supportedFileTypes;
        @Min(1) private Long maximumFileSizeBytes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TemplateResponse {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private Boolean allowForwardIfIncomplete;
        private Boolean active;
        private List<ItemResponse> items;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private UUID templateId;
        private String name;
        private String label;
        private String description;
        private String controlType;
        private String placeholder;
        private String helpText;
        private Boolean mandatory;
        private Boolean readOnly;
        private Boolean visible;
        private Object defaultValue;
        private Map<String, Object> validationRules;
        private Integer minimumLength;
        private Integer maximumLength;
        private String regexPattern;
        private List<String> options;
        private String autoValueExpression;
        private Integer parentItemSequence;
        private String visibilityConditionOperator;
        private Object visibilityConditionValue;
        private String mandatoryConditionOperator;
        private Object mandatoryConditionValue;
        private Boolean active;
        private Integer sequence;
        private Boolean remarksRequired;
        private Boolean allowMultipleAttachments;
        private String supportedFileTypes;
        private Long maximumFileSizeBytes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MappingRequest {
        @NotNull private UUID categoryId;
        @NotNull private UUID templateId;
        private Boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MappingResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private UUID templateId;
        private String templateName;
        private Boolean active;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ResponseRequest {
        private Object value;
        private String remarks;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class VerificationRequest {
        @NotBlank
        @Pattern(regexp = "VERIFIED|REJECTED|NOT_APPLICABLE|PENDING",
                message = "Status must be VERIFIED, REJECTED, NOT_APPLICABLE or PENDING")
        private String status;
        private String remarks;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AdditionalItemRequest {
        @NotBlank @Size(max = 200) private String name;
        @Size(max = 200) private String label;
        private String description;
        @Pattern(regexp = CONTROL_TYPE_PATTERN, message = "Unsupported checklist control type")
        private String controlType;
        @Size(max = 300) private String placeholder;
        private String helpText;
        private Boolean mandatory;
        private Boolean readOnly;
        private Boolean visible;
        private Object defaultValue;
        private Map<String, Object> validationRules = new LinkedHashMap<>();
        @Min(0) private Integer minimumLength;
        @Min(0) private Integer maximumLength;
        @Size(max = 1000) private String regexPattern;
        private List<@Size(max = 500) String> options = new ArrayList<>();
        @Size(max = 1000) private String autoValueExpression;
        private Integer parentItemSequence;
        @Size(max = 40) private String visibilityConditionOperator;
        private Object visibilityConditionValue;
        @Size(max = 40) private String mandatoryConditionOperator;
        private Object mandatoryConditionValue;
        private Boolean remarksRequired;
        private Boolean allowMultipleAttachments;
        @Size(max = 500) private String supportedFileTypes;
        @Min(1) private Long maximumFileSizeBytes;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttachmentResponse {
        private UUID id;
        private String originalFileName;
        private String fileExtension;
        private String mimeType;
        private Long fileSize;
        private String checksum;
        private UUID uploadedBy;
        private String uploadedByName;
        private LocalDateTime uploadedAt;
        private String downloadUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InstanceItemResponse {
        private UUID id;
        private UUID templateItemId;
        private String name;
        private String label;
        private String description;
        private String controlType;
        private String placeholder;
        private String helpText;
        private Boolean mandatory;
        private Boolean effectiveMandatory;
        private Boolean readOnly;
        private Boolean visible;
        private Boolean effectiveVisible;
        private Object defaultValue;
        private Map<String, Object> validationRules;
        private Integer minimumLength;
        private Integer maximumLength;
        private String regexPattern;
        private List<String> options;
        private String autoValueExpression;
        private Integer parentItemSequence;
        private String visibilityConditionOperator;
        private Object visibilityConditionValue;
        private String mandatoryConditionOperator;
        private Object mandatoryConditionValue;
        private Integer sequence;
        private Boolean remarksRequired;
        private Boolean allowMultipleAttachments;
        private String supportedFileTypes;
        private Long maximumFileSizeBytes;
        private String status;
        private Object responseValue;
        private String responseDisplayValue;
        private Boolean responseComplete;
        private UUID respondedBy;
        private String respondedByName;
        private LocalDateTime respondedAt;
        private UUID uploadedBy;
        private String uploadedByName;
        private LocalDateTime uploadedAt;
        private UUID verifiedBy;
        private String verifiedByName;
        private LocalDateTime verifiedAt;
        private UUID requestedBy;
        private String requestedByName;
        private LocalDateTime requestedAt;
        private String remarks;
        private List<AttachmentResponse> attachments;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private UUID instanceId;
        private UUID templateId;
        private String templateName;
        private String status;
        private long totalItems;
        private long completedItems;
        private long uploadedItems;
        private long answeredItems;
        private long pendingVerification;
        private long pendingResponse;
        private long missingMandatory;
        private boolean complete;
        private boolean forwardOverrideUsed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InstanceResponse {
        private UUID id;
        private UUID templateId;
        private String templateName;
        private String templateDescription;
        private UUID categoryId;
        private String categoryName;
        private UUID diaryEntryId;
        private UUID letterId;
        private String status;
        private Boolean allowForwardIfIncomplete;
        private UUID forwardOverrideBy;
        private String forwardOverrideByName;
        private LocalDateTime forwardOverrideAt;
        private String forwardOverrideRemarks;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private Summary summary;
        private List<InstanceItemResponse> items;
        private List<TimelineEvent> timeline;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimelineEvent {
        private LocalDateTime timestamp;
        private String action;
        private String title;
        private String detail;
        private UUID itemId;
        private UUID userId;
        private String userName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardSummary {
        private long totalChecklists;
        private long complete;
        private long incomplete;
        private long forwardedWithExceptions;
        private long pendingVerification;
        private long pendingResponses;
        private long totalResponses;
        private long answeredResponses;
        private long lettersWithMissingDocuments;
        private double compliancePercent;
        @Builder.Default private Map<String, Long> incompleteByCategory = new LinkedHashMap<>();
        @Builder.Default private Map<String, Double> complianceByDepartment = new LinkedHashMap<>();
        @Builder.Default private Map<String, Double> complianceBySection = new LinkedHashMap<>();
        @Builder.Default private Map<String, Double> complianceByCategory = new LinkedHashMap<>();
    }
}
