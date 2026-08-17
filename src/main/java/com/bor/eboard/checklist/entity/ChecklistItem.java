package com.bor.eboard.checklist.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checklist_items")
public class ChecklistItem extends BaseEntity {
    @Column(name = "checklist_template_id", nullable = false)
    private UUID checklistTemplateId;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(name = "description")
    private String description;
    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = Boolean.FALSE;
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
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
}
