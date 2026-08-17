package com.bor.eboard.dashboard.facade;

import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.identity.facade.IdentityFacade;

import java.util.List;
import java.util.UUID;

/** Reuses the dashboard's established permission and object-scope resolution. */
public interface DashboardScopeFacade {

    record EmployeeScope(DashboardScopeType scopeType, UUID scopeId,
                         String scopeLabel,
                         List<IdentityFacade.EmployeeRef> employees) {
        public EmployeeScope {
            employees = employees == null ? List.of() : List.copyOf(employees);
        }
    }

    EmployeeScope resolveEmployeeScope(DashboardScopeType scopeType, UUID scopeId);
}
