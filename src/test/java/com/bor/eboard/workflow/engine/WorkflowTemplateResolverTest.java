package com.bor.eboard.workflow.engine;

import com.bor.eboard.common.exception.WorkflowException;
import com.bor.eboard.workflow.entity.WorkflowTemplate;
import com.bor.eboard.workflow.repository.WorkflowTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowTemplateResolver")
class WorkflowTemplateResolverTest {

    @Mock private WorkflowTemplateRepository templateRepository;
    @InjectMocks private WorkflowTemplateResolver resolver;

    private WorkflowTemplate template(String name, UUID cat, UUID dept, UUID sec, UUID pri) {
        WorkflowTemplate t = new WorkflowTemplate();
        t.setName(name);
        t.setCategoryId(cat);
        t.setDepartmentId(dept);
        t.setSectionId(sec);
        t.setPriorityId(pri);
        return t;
    }

    @Test
    @DisplayName("prefers the most specific template (section+priority beats category-only)")
    void picksMostSpecific() {
        UUID cat = UUID.randomUUID();
        UUID sec = UUID.randomUUID();
        UUID pri = UUID.randomUUID();

        WorkflowTemplate categoryOnly = template("Category Only", cat, null, null, null);
        WorkflowTemplate sectionPriority = template("Section+Priority", null, null, sec, pri);
        WorkflowTemplate catchAll = template("Default", null, null, null, null);

        when(templateRepository.findCandidates(any(), any(), any(), any()))
                .thenReturn(List.of(categoryOnly, catchAll, sectionPriority));

        WorkflowTemplate resolved = resolver.resolve(cat, null, sec, pri);

        // section(4) + priority(8) = 12 beats category(1) and default(0)
        assertThat(resolved.getName()).isEqualTo("Section+Priority");
    }

    @Test
    @DisplayName("falls back to the catch-all default when it is the only candidate")
    void fallsBackToDefault() {
        WorkflowTemplate catchAll = template("Default", null, null, null, null);
        when(templateRepository.findCandidates(any(), any(), any(), any()))
                .thenReturn(List.of(catchAll));

        WorkflowTemplate resolved = resolver.resolve(UUID.randomUUID(), null, null, null);

        assertThat(resolved.getName()).isEqualTo("Default");
    }

    @Test
    @DisplayName("throws when no template matches")
    void throwsWhenNoCandidate() {
        when(templateRepository.findCandidates(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve(null, null, null, null))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("No workflow template");
    }
}
