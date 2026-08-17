package com.bor.eboard.assignment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * What the Mark dialog needs: the roles the caller may mark to, and — once a
 * role and section are chosen — the actual users eligible under the rules.
 */
@Data
@Builder
public class MarkTargetResponse {

    private List<RoleOption> roles;
    private List<UserOption> users;
    private Boolean multiUserAllowed;

    @Data
    @Builder
    public static class RoleOption {
        private UUID roleId;
        private String roleCode;
        private String roleName;
        private Boolean crossSectionAllowed;
    }

    @Data
    @Builder
    public static class UserOption {
        private UUID userId;
        private String fullName;
        private String designationName;
        private UUID sectionId;
        private String sectionName;
    }
}
