package com.bor.eboard.charge.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Cross-module read-only entry point for charge and posting information. */
public interface ChargeFacade {

    record OperationalScope(Set<UUID> employeeIds, Set<UUID> sectionIds,
                            UUID ownerId, boolean organization) {
        public OperationalScope {
            employeeIds = employeeIds == null ? Set.of() : Set.copyOf(employeeIds);
            sectionIds = sectionIds == null ? Set.of() : Set.copyOf(sectionIds);
        }
    }

    record OperationalMetrics(long additionalCharges, long vacantSeats,
                              long joiningPending, long relievingPending,
                              long transfersToday) {
    }

    List<UUID> activeChargeGrantorsFor(UUID toUserId);

    OperationalMetrics operationalMetrics(OperationalScope scope, LocalDate today);

    /** Current substantive posting office by employee, for retirement reporting. */
    Map<UUID, String> currentOfficeNames(Set<UUID> employeeIds);
}
