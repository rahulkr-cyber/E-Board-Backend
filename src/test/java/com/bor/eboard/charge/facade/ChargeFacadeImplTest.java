package com.bor.eboard.charge.facade;

import com.bor.eboard.charge.facade.impl.ChargeFacadeImpl;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeFacade")
class ChargeFacadeImplTest {

    @Mock private ChargeAssignmentRepository chargeRepository;
    @InjectMocks private ChargeFacadeImpl facade;

    @Test
    @DisplayName("returns the grantor user-ids for a user's active charges")
    void returnsActiveGrantors() {
        UUID toUser = UUID.randomUUID();
        UUID grantorA = UUID.randomUUID();
        UUID grantorB = UUID.randomUUID();
        when(chargeRepository.findActiveGrantorUserIds(eq(toUser), any(LocalDateTime.class)))
                .thenReturn(List.of(grantorA, grantorB));

        List<UUID> result = facade.activeChargeGrantorsFor(toUser);

        assertThat(result).containsExactlyInAnyOrder(grantorA, grantorB);
    }

    @Test
    @DisplayName("returns empty (and skips the query) for a null user")
    void nullUserShortCircuits() {
        List<UUID> result = facade.activeChargeGrantorsFor(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(chargeRepository);
    }

    @Test
    @DisplayName("returns empty when the user holds no active charge")
    void noActiveCharge() {
        UUID toUser = UUID.randomUUID();
        when(chargeRepository.findActiveGrantorUserIds(eq(toUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertThat(facade.activeChargeGrantorsFor(toUser)).isEmpty();
    }
}
