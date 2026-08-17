package com.bor.eboard.charge.service;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreateChargeRequest;
import com.bor.eboard.charge.entity.ChargeAssignment;
import com.bor.eboard.charge.mapper.ChargeMapper;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import com.bor.eboard.charge.service.impl.ChargeServiceImpl;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.identity.facade.IdentityFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeService")
class ChargeServiceImplTest {

    @Mock private ChargeAssignmentRepository chargeRepository;
    @Mock private ChargeMapper mapper;
    @Mock private IdentityFacade identityFacade;
    @Mock private AuditService auditService;

    @InjectMocks private ChargeServiceImpl service;

    private UUID fromUser;
    private UUID toUser;

    @BeforeEach
    void setUp() {
        fromUser = UUID.randomUUID();
        toUser = UUID.randomUUID();
    }

    private CreateChargeRequest validRequest() {
        CreateChargeRequest req = new CreateChargeRequest();
        req.setFromUserId(fromUser);
        req.setToUserId(toUser);
        req.setChargeType("TEMPORARY");
        req.setEffectiveFrom(LocalDateTime.now());
        req.setEffectiveTo(LocalDateTime.now().plusDays(5));
        return req;
    }

    private void bothUsersExist() {
        lenient().when(identityFacade.findUser(fromUser)).thenReturn(Optional.of(
                new IdentityFacade.UserRef(fromUser, null, null, null, "From", true)));
        lenient().when(identityFacade.findUser(toUser)).thenReturn(Optional.of(
                new IdentityFacade.UserRef(toUser, null, null, null, "To", true)));
    }

    @Test
    @DisplayName("creates an ACTIVE charge, persists it, and audits")
    void createsActiveCharge() {
        bothUsersExist();
        when(chargeRepository.save(any(ChargeAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toChargeResponse(any(ChargeAssignment.class)))
                .thenReturn(ChargeResponse.builder().status("ACTIVE").build());

        ChargeResponse response = service.create(validRequest());

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        ArgumentCaptor<ChargeAssignment> captor = ArgumentCaptor.forClass(ChargeAssignment.class);
        verify(chargeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getChargeType()).isEqualTo("TEMPORARY");
        verify(auditService).record(eq("CHARGE"), eq("CHARGE_ASSIGNMENT"),
                any(), eq("CREATE"), isNull(), anyString());
    }

    @Test
    @DisplayName("rejects an invalid charge type")
    void rejectsInvalidType() {
        CreateChargeRequest req = validRequest();
        req.setChargeType("BOGUS");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("charge type");
        verify(chargeRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a charge from a user to themselves")
    void rejectsSelfCharge() {
        CreateChargeRequest req = validRequest();
        req.setToUserId(fromUser);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    @DisplayName("rejects an effectiveTo before effectiveFrom")
    void rejectsInvertedWindow() {
        bothUsersExist();
        CreateChargeRequest req = validRequest();
        req.setEffectiveFrom(LocalDateTime.now());
        req.setEffectiveTo(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("effectiveTo");
    }

    @Test
    @DisplayName("cancel transitions an ACTIVE charge to CANCELLED and audits")
    void cancelsActiveCharge() {
        UUID id = UUID.randomUUID();
        ChargeAssignment active = new ChargeAssignment();
        active.setStatus("ACTIVE");
        when(chargeRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(active));
        when(chargeRepository.save(any(ChargeAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toChargeResponse(any(ChargeAssignment.class)))
                .thenReturn(ChargeResponse.builder().status("CANCELLED").build());

        service.cancel(id);

        assertThat(active.getStatus()).isEqualTo("CANCELLED");
        verify(auditService).record(eq("CHARGE"), eq("CHARGE_ASSIGNMENT"),
                any(), eq("CANCEL"), eq("ACTIVE"), eq("CANCELLED"));
    }

    @Test
    @DisplayName("cannot cancel a charge that is not ACTIVE")
    void cannotCancelInactive() {
        UUID id = UUID.randomUUID();
        ChargeAssignment expired = new ChargeAssignment();
        expired.setStatus("EXPIRED");
        when(chargeRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("cancel of an unknown charge raises not-found")
    void cancelUnknown() {
        UUID id = UUID.randomUUID();
        when(chargeRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
