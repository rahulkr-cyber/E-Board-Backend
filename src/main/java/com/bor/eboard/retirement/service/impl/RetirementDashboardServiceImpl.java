package com.bor.eboard.retirement.service.impl;

import com.bor.eboard.charge.facade.ChargeFacade;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dashboard.facade.DashboardScopeFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.retirement.dto.RetirementDashboardResponse;
import com.bor.eboard.retirement.dto.RetirementEmployeeRow;
import com.bor.eboard.retirement.dto.RetirementPeriod;
import com.bor.eboard.retirement.dto.RetirementQuery;
import com.bor.eboard.retirement.dto.RetirementSort;
import com.bor.eboard.retirement.service.RetirementDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetirementDashboardServiceImpl implements RetirementDashboardService {

    private final DashboardScopeFacade dashboardScopeFacade;
    private final IdentityFacade identityFacade;
    private final ChargeFacade chargeFacade;

    @Override
    @Transactional(readOnly = true)
    public RetirementDashboardResponse get(RetirementQuery query) {
        RetirementQuery safeQuery = query == null
                ? new RetirementQuery(null, null, null, null, null, null,
                        null, null, RetirementPeriod.WITHIN_180_DAYS, null,
                        RetirementSort.NEAREST_RETIREMENT, null, null, null, null)
                : query;
        validateMonthYear(safeQuery.retirementMonth(), safeQuery.retirementYear());

        DashboardScopeFacade.EmployeeScope scope = dashboardScopeFacade.resolveEmployeeScope(
                safeQuery.effectiveScopeType(), safeQuery.scopeId());
        LocalDate today = LocalDate.now();
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> designations = identityFacade.designationNames();
        Set<UUID> employeeIds = scope.employees().stream()
                .map(IdentityFacade.EmployeeRef::id)
                .collect(Collectors.toSet());
        Map<UUID, String> offices = chargeFacade.currentOfficeNames(employeeIds);

        List<RetirementEmployeeRow> filtered = scope.employees().stream()
                .filter(employee -> employee.dateOfBirth() != null)
                .filter(employee -> safeQuery.departmentId() == null
                        || safeQuery.departmentId().equals(employee.departmentId()))
                .filter(employee -> safeQuery.sectionId() == null
                        || safeQuery.sectionId().equals(employee.sectionId()))
                .filter(employee -> safeQuery.designationId() == null
                        || safeQuery.designationId().equals(employee.designationId()))
                .filter(employee -> matches(employee.district(), safeQuery.district()))
                .filter(employee -> matches(employee.status(), safeQuery.employeeStatus()))
                .filter(employee -> matches(employee.fullName(), safeQuery.employeeName()))
                .filter(employee -> matches(employee.employeeCode(), safeQuery.employeeCode()))
                .map(employee -> toRow(employee, departments, sections, designations,
                        offices.get(employee.id()), today))
                .filter(row -> matches(row.getOffice(), safeQuery.office()))
                .filter(row -> safeQuery.retirementMonth() == null
                        || row.getRetirementDate().getMonthValue() == safeQuery.retirementMonth())
                .filter(row -> safeQuery.retirementYear() == null
                        || row.getRetirementDate().getYear() == safeQuery.retirementYear())
                .filter(row -> matchesSearch(row, safeQuery.search()))
                .toList();

        long within180Days = countWithinDays(filtered, 180);
        long within90Days = countWithinDays(filtered, 90);
        long within30Days = countWithinDays(filtered, 30);
        long retiredToday = filtered.stream().filter(row -> row.getDaysRemaining() == 0).count();
        long retiringThisYear = filtered.stream()
                .filter(row -> row.getRetirementDate().getYear() == today.getYear())
                .count();
        long alreadyRetired = filtered.stream().filter(row -> row.getDaysRemaining() < 0).count();

        List<RetirementEmployeeRow> rows = filtered.stream()
                .filter(row -> inPeriod(row, safeQuery.effectivePeriod()))
                .sorted(comparator(safeQuery.effectiveSort()))
                .toList();

        return RetirementDashboardResponse.builder()
                .scope(scope.scopeType().name())
                .scopeId(scope.scopeId())
                .scopeLabel(scope.scopeLabel())
                .within180Days(within180Days)
                .within90Days(within90Days)
                .within30Days(within30Days)
                .retiredToday(retiredToday)
                .retiringThisYear(retiringThisYear)
                .alreadyRetired(alreadyRetired)
                .withinSixMonths(within180Days)
                .withinThreeMonths(within90Days)
                .withinOneMonth(within30Days)
                .today(retiredToday)
                .employees(rows)
                .build();
    }

    private RetirementEmployeeRow toRow(
            IdentityFacade.EmployeeRef employee,
            Map<UUID, String> departments,
            Map<UUID, String> sections,
            Map<UUID, String> designations,
            String office,
            LocalDate today) {
//        LocalDate retirementDate = employee.dateOfBirth().plusYears(60);
    	LocalDate sixtyYears = employee.dateOfBirth().plusYears(60);
    	LocalDate retirementDate = sixtyYears.withDayOfMonth(sixtyYears.lengthOfMonth());
        long daysRemaining = ChronoUnit.DAYS.between(today, retirementDate);
        return RetirementEmployeeRow.builder()
                .employeeId(employee.id())
                .profileAttachmentId(employee.profileAttachmentId())
                .employeeName(employee.fullName())
                .employeeCode(employee.employeeCode())
                .designation(designations.get(employee.designationId()))
                .department(departments.get(employee.departmentId()))
                .section(sections.get(employee.sectionId()))
                .office(office)
                .district(employee.district())
                .dateOfBirth(employee.dateOfBirth())
                .retirementDate(retirementDate)
                .daysRemaining(daysRemaining)
                .employeeStatus(employee.status())
                .retirementStatus(retirementStatus(daysRemaining))
                .build();
    }

    private long countWithinDays(List<RetirementEmployeeRow> rows, long days) {
        return rows.stream()
                .filter(row -> row.getDaysRemaining() >= 0 && row.getDaysRemaining() <= days)
                .count();
    }

    private boolean inPeriod(RetirementEmployeeRow row, RetirementPeriod period) {
        return switch (period) {
            case ALL -> true;
            case WITHIN_180_DAYS, WITHIN_6_MONTHS ->
                    row.getDaysRemaining() >= 0 && row.getDaysRemaining() <= 180;
            case WITHIN_90_DAYS, WITHIN_3_MONTHS ->
                    row.getDaysRemaining() >= 0 && row.getDaysRemaining() <= 90;
            case WITHIN_30_DAYS, WITHIN_1_MONTH ->
                    row.getDaysRemaining() >= 0 && row.getDaysRemaining() <= 30;
            case RETIRED_TODAY, TODAY -> row.getDaysRemaining() == 0;
            case ALREADY_RETIRED -> row.getDaysRemaining() < 0;
        };
    }

    private Comparator<RetirementEmployeeRow> comparator(RetirementSort sort) {
        Comparator<String> text = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<RetirementEmployeeRow> nearest = Comparator
                .comparing(RetirementEmployeeRow::getRetirementDate)
                .thenComparing(RetirementEmployeeRow::getEmployeeName, text);
        return switch (sort) {
            case OLDEST_EMPLOYEE -> Comparator
                    .comparing(RetirementEmployeeRow::getDateOfBirth)
                    .thenComparing(RetirementEmployeeRow::getEmployeeName, text);
            case DEPARTMENT -> Comparator
                    .comparing(RetirementEmployeeRow::getDepartment, text)
                    .thenComparing(nearest);
            case DESIGNATION -> Comparator
                    .comparing(RetirementEmployeeRow::getDesignation, text)
                    .thenComparing(nearest);
            case NEAREST_RETIREMENT -> nearest;
        };
    }

    private boolean matchesSearch(RetirementEmployeeRow row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String expected = search.trim().toLowerCase(Locale.ROOT);
        return contains(row.getEmployeeName(), expected)
                || contains(row.getEmployeeCode(), expected)
                || contains(row.getDesignation(), expected)
                || contains(row.getDepartment(), expected)
                || contains(row.getSection(), expected)
                || contains(row.getOffice(), expected);
    }

    private boolean contains(String actual, String expectedLowerCase) {
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(expectedLowerCase);
    }

    private boolean matches(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return contains(actual, expected.trim().toLowerCase(Locale.ROOT));
    }

    private String retirementStatus(long daysRemaining) {
        if (daysRemaining < 0) {
            return "RETIRED";
        }
        if (daysRemaining == 0) {
            return "RETIRED_TODAY";
        }
        if (daysRemaining <= 30) {
            return "WITHIN_30_DAYS";
        }
        if (daysRemaining <= 90) {
            return "WITHIN_90_DAYS";
        }
        if (daysRemaining <= 180) {
            return "WITHIN_180_DAYS";
        }
        return "FUTURE";
    }

    private void validateMonthYear(Integer month, Integer year) {
        if (month != null && (month < 1 || month > 12)) {
            throw new ValidationException("Retirement month must be between 1 and 12");
        }
        if (year != null && (year < 1900 || year > 9999)) {
            throw new ValidationException("Retirement year is invalid");
        }
    }
}
