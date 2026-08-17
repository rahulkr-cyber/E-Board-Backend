package com.bor.eboard.checklist.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checklist_instance_items")
public class ChecklistInstanceItem extends BaseEntity {
    @Column(name = "checklist_instance_id", nullable = false)
    private UUID checklistInstanceId;
    @Column(name = "checklist_item_id")
    private UUID checklistItemId;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(name = "description")
    private String description;
    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = Boolean.FALSE;
    @Column(name = "sequence_no", nullable = false)
    private Integer sequence;
    @Column(name = "remarks_required", nullable = false)
    private Boolean remarksRequired = Boolean.FALSE;
    @Column(name = "allow_multiple_attachments", nullable = false)
    private Boolean allowMultipleAttachments = Boolean.FALSE;
    @Column(name = "supported_file_types", length = 500)
    private String supportedFileTypes;
    @Column(name = "maximum_file_size_bytes")
    private Long maximumFileSizeBytes;
    @Column(name = "control_type", nullable = false, length = 40)
    private String controlType = "DOCUMENT_UPLOAD";
    @Column(name = "display_label", length = 200)
    private String displayLabel;
    @Column(name = "placeholder", length = 300)
    private String placeholder;
    @Column(name = "help_text")
    private String helpText;
    @Column(name = "read_only", nullable = false)
    private Boolean readOnly = Boolean.FALSE;
    @Column(name = "visible", nullable = false)
    private Boolean visible = Boolean.TRUE;
    @Column(name = "default_value_json")
    private String defaultValueJson;
    @Column(name = "validation_rules_json")
    private String validationRulesJson;
    @Column(name = "minimum_length")
    private Integer minimumLength;
    @Column(name = "maximum_length")
    private Integer maximumLength;
    @Column(name = "regex_pattern", length = 1000)
    private String regexPattern;
    @Column(name = "options_json")
    private String optionsJson;
    @Column(name = "auto_value_expression", length = 1000)
    private String autoValueExpression;
    @Column(name = "parent_item_sequence")
    private Integer parentItemSequence;
    @Column(name = "visibility_condition_operator", length = 40)
    private String visibilityConditionOperator;
    @Column(name = "visibility_condition_value")
    private String visibilityConditionValue;
    @Column(name = "mandatory_condition_operator", length = 40)
    private String mandatoryConditionOperator;
    @Column(name = "mandatory_condition_value")
    private String mandatoryConditionValue;
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    @Column(name = "uploaded_by")
    private UUID uploadedBy;
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    @Column(name = "verified_by")
    private UUID verifiedBy;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "requested_by")
    private UUID requestedBy;
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;
    @Column(name = "remarks")
    private String remarks;
    @Column(name = "response_value_json")
    private String responseValueJson;
    @Column(name = "response_display_value")
    private String responseDisplayValue;
    @Column(name = "responded_by")
    private UUID respondedBy;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
