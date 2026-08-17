package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsAdministrationDashboardResponse;
import com.bor.eboard.dms.dto.DmsAdministrationHealthResponse;

public interface DmsAdministrationService {

    DmsAdministrationDashboardResponse getDashboard();

    DmsAdministrationHealthResponse checkHealth();
}
