package com.bor.eboard.charge;

import com.bor.eboard.charge.entity.ChargeAssignment;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChargeAssignmentRepository")
class ChargeAssignmentRepositoryTest {

    @Autowired private ChargeAssignmentRepository repository;

    private final UUID holder = UUID.randomUUID();
    private final UUID grantor = UUID.randomUUID();

    private ChargeAssignment charge(UUID from, UUID to, String status,
                                    LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        ChargeAssignment c = new ChargeAssignment();
        c.setFromUserId(from);
        c.setToUserId(to);
        c.setChargeType("TEMPORARY");
        c.setStatus(status);
        c.setEffectiveFrom(effectiveFrom);
        c.setEffectiveTo(effectiveTo);
        return c;
    }

    @BeforeEach
    void seed() {
        LocalDateTime now = LocalDateTime.now();
        // Active and currently within window -> should be returned.
        repository.save(charge(grantor, holder, "ACTIVE",
                now.minusDays(1), now.plusDays(5)));
        // Active but window not yet started -> excluded.
        repository.save(charge(UUID.randomUUID(), holder, "ACTIVE",
                now.plusDays(1), now.plusDays(5)));
        // Active but already expired by window -> excluded.
        repository.save(charge(UUID.randomUUID(), holder, "ACTIVE",
                now.minusDays(10), now.minusDays(1)));
        // Cancelled -> excluded.
        repository.save(charge(UUID.randomUUID(), holder, "CANCELLED",
                now.minusDays(1), now.plusDays(5)));
        // Open-ended active charge (no effectiveTo) -> should be returned.
        repository.save(charge(UUID.randomUUID(), holder, "ACTIVE",
                now.minusDays(2), null));
    }

    @Test
    @DisplayName("returns only grantors of active charges within their effective window")
    void findsActiveWindowGrantors() {
        List<UUID> grantors = repository.findActiveGrantorUserIds(holder, LocalDateTime.now());

        // The pre-window, post-window, and cancelled charges are excluded;
        // the current-window and open-ended charges are included.
        assertThat(grantors).hasSize(2).contains(grantor);
    }

    @Test
    @DisplayName("finds expirable charges whose effective_to has passed")
    void findsExpirable() {
        List<ChargeAssignment> expirable = repository.findExpirable(LocalDateTime.now());

        assertThat(expirable).hasSize(1);
        assertThat(expirable.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(expirable.get(0).getEffectiveTo()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("filters active charges by status")
    void findsByStatus() {
        List<ChargeAssignment> active = repository
                .findByDeletedFalseAndStatusOrderByEffectiveFromDesc("ACTIVE");
        assertThat(active).hasSize(4); // all but the cancelled one
    }
}
