package com.bor.eboard.registry.service;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.core.service.NumberGenerationService;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.registry.dto.ForwardDiaryRequest;
import com.bor.eboard.registry.entity.DiaryEntry;
import com.bor.eboard.registry.mapper.DiaryMapper;
import com.bor.eboard.registry.repository.DiaryEntryRepository;
import com.bor.eboard.registry.repository.ReceiptRegisterRepository;
import com.bor.eboard.registry.service.impl.DiaryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Guards on diary forwarding. A forwarded diary is terminal for the registry
 * clerk — it cannot be forwarded again, mirroring the rule that the registry
 * role cannot approve or re-route once a file has entered the workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryService — forward guards")
class DiaryServiceForwardTest {

    @Mock private DiaryEntryRepository diaryEntryRepository;
    @Mock private ReceiptRegisterRepository receiptRegisterRepository;
    @Mock private NumberGenerationService numberGenerationService;
    @Mock private CorrespondenceFacade correspondenceFacade;
    @Mock private AttachmentStorageService attachmentStorageService;
    @Mock private MasterDataFacade masterDataFacade;
    @Mock private IdentityFacade identityFacade;
    @Mock private DiaryMapper diaryMapper;
    @Mock private AuditService auditService;
    @Mock private NotificationFacade notificationFacade;

    @InjectMocks private DiaryServiceImpl service;

    private DiaryEntry entryWithStatus(String status) {
        DiaryEntry entry = new DiaryEntry();
        entry.setStatus(status);
        entry.setDiaryNumber("BOR/2026/000001");
        return entry;
    }

    @Test
    @DisplayName("a diary already FORWARDED cannot be forwarded again")
    void cannotForwardAlreadyForwarded() {
        UUID id = UUID.randomUUID();
        when(diaryEntryRepository.findByIdAndDeletedFalse(id))
                .thenReturn(Optional.of(entryWithStatus("FORWARDED")));

        ForwardDiaryRequest request = new ForwardDiaryRequest();
        request.setSectionId(UUID.randomUUID());

        assertThatThrownBy(() -> service.forward(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be forwarded");
    }

    @Test
    @DisplayName("forwarding to an unknown section is rejected")
    void rejectsUnknownSection() {
        UUID id = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(diaryEntryRepository.findByIdAndDeletedFalse(id))
                .thenReturn(Optional.of(entryWithStatus("DIARIZED")));
        when(identityFacade.findSection(sectionId)).thenReturn(Optional.empty());

        ForwardDiaryRequest request = new ForwardDiaryRequest();
        request.setSectionId(sectionId);

        assertThatThrownBy(() -> service.forward(id, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid target section");
    }

    @Test
    @DisplayName("a target user outside the target section is rejected")
    void rejectsUserNotInSection() {
        UUID id = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherSection = UUID.randomUUID();

        when(diaryEntryRepository.findByIdAndDeletedFalse(id))
                .thenReturn(Optional.of(entryWithStatus("DIARIZED")));
        when(identityFacade.findSection(sectionId)).thenReturn(Optional.of(
                new IdentityFacade.SectionRef(sectionId, UUID.randomUUID(), "Sec")));
        when(identityFacade.findUser(userId)).thenReturn(Optional.of(
                new IdentityFacade.UserRef(userId, otherSection, null, null, "U", true)));

        ForwardDiaryRequest request = new ForwardDiaryRequest();
        request.setSectionId(sectionId);
        request.setUserId(userId);

        assertThatThrownBy(() -> service.forward(id, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong");
    }
}
