package com.bor.eboard.workflow.engine;

import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.WorkflowException;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowValidationService")
class WorkflowValidationServiceTest {

    @Mock private IdentityFacade identityFacade;
    @Mock private CorrespondenceFacade correspondenceFacade;
    @InjectMocks private WorkflowValidationService validation;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID userId, String... authorities) {
        var auths = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", auths));
    }

    private WorkflowTask task(String status, UUID assignedUser) {
        WorkflowTask t = new WorkflowTask();
        t.setStatus(status);
        t.setAssignedToUserId(assignedUser);
        return t;
    }

    @Nested
    @DisplayName("requireActiveUser")
    class RequireActiveUser {

        @Test
        @DisplayName("returns the id for an active authenticated user")
        void activeUser() {
            UUID id = UUID.randomUUID();
            authenticateAs(id);
            when(identityFacade.findUser(id)).thenReturn(Optional.of(
                    new IdentityFacade.UserRef(id, null, null, null, "U", true)));

            assertThat(validation.requireActiveUser()).isEqualTo(id);
        }

        @Test
        @DisplayName("rejects an unauthenticated caller")
        void notAuthenticated() {
            assertThatThrownBy(() -> validation.requireActiveUser())
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("forbids an inactive user")
        void inactiveUser() {
            UUID id = UUID.randomUUID();
            authenticateAs(id);
            when(identityFacade.findUser(id)).thenReturn(Optional.of(
                    new IdentityFacade.UserRef(id, null, null, null, "U", false)));

            assertThatThrownBy(() -> validation.requireActiveUser())
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requirePermission")
    class RequirePermission {

        @Test
        @DisplayName("passes when the authority is present")
        void hasPermission() {
            authenticateAs(UUID.randomUUID(), "WORKFLOW_APPROVE");
            assertThatCode(() -> validation.requirePermission("WORKFLOW_APPROVE"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("forbids when the authority is missing")
        void missingPermission() {
            authenticateAs(UUID.randomUUID(), "WORKFLOW_VIEW");
            assertThatThrownBy(() -> validation.requirePermission("WORKFLOW_APPROVE"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireFileOpen")
    class RequireFileOpen {

        @Test
        @DisplayName("throws for a closed file")
        void closedFile() {
            UUID fileId = UUID.randomUUID();
            when(correspondenceFacade.findFileState(fileId)).thenReturn(Optional.of(
                    new CorrespondenceFacade.FileState(fileId, "BOR/2026/000001", "s",
                            null, null, null, null, null, null, null, "CLOSED", true)));

            assertThatThrownBy(() -> validation.requireFileOpen(fileId))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("cannot move");
        }

        @Test
        @DisplayName("passes for an open file")
        void openFile() {
            UUID fileId = UUID.randomUUID();
            when(correspondenceFacade.findFileState(fileId)).thenReturn(Optional.of(
                    new CorrespondenceFacade.FileState(fileId, "BOR/2026/000001", "s",
                            null, null, null, null, null, null, null, "IN_PROGRESS", false)));

            assertThatCode(() -> validation.requireFileOpen(fileId)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("task guards")
    class TaskGuards {

        @Test
        @DisplayName("requireTaskPending rejects a null task")
        void nullTask() {
            assertThatThrownBy(() -> validation.requireTaskPending(null))
                    .isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("requireTaskPending rejects a non-pending task")
        void notPending() {
            assertThatThrownBy(() -> validation.requireTaskPending(task("COMPLETED", null)))
                    .isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("requireAssignedUser forbids a non-assignee")
        void notAssignee() {
            UUID user = UUID.randomUUID();
            WorkflowTask t = task("PENDING", UUID.randomUUID());
            when(identityFacade.userMatchesAssignment(any(), any(), any(), any()))
                    .thenReturn(false);

            assertThatThrownBy(() -> validation.requireAssignedUser(user, t))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("step capability guards")
    class StepGuards {

        private WorkflowStep step(boolean canReturn, boolean canReject, boolean canReassign) {
            WorkflowStep s = new WorkflowStep();
            s.setCanReturn(canReturn);
            s.setCanReject(canReject);
            s.setCanReassign(canReassign);
            return s;
        }

        @Test
        @DisplayName("blocks return when the step disallows it")
        void returnBlocked() {
            assertThatThrownBy(() -> validation.requireReturnAllowed(step(false, true, true)))
                    .isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("blocks reject when the step disallows it")
        void rejectBlocked() {
            assertThatThrownBy(() -> validation.requireRejectAllowed(step(true, false, true)))
                    .isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("allows reassign when the step permits it")
        void reassignAllowed() {
            assertThatCode(() -> validation.requireReassignAllowed(step(true, true, true)))
                    .doesNotThrowAnyException();
        }
    }
}
