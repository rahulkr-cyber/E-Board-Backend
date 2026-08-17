package com.bor.eboard.retirement.service;

import com.bor.eboard.retirement.dto.RetirementDashboardResponse;
import com.bor.eboard.retirement.dto.RetirementQuery;

public interface RetirementDashboardService {
    RetirementDashboardResponse get(RetirementQuery query);
}
