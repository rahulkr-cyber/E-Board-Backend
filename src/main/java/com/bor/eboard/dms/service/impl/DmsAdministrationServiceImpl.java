package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.config.DmsProperties;
import com.bor.eboard.dms.dto.DmsAdministrationDashboardResponse;
import com.bor.eboard.dms.dto.DmsAdministrationHealthResponse;
import com.bor.eboard.dms.dto.DmsConfigurationCheckResponse;
import com.bor.eboard.dms.dto.StorageHealthResponse;
import com.bor.eboard.dms.dto.DmsUploadPolicyResponse;
import com.bor.eboard.dms.repository.DmsAdministrationRepository;
import com.bor.eboard.dms.service.DmsAdministrationService;
import com.bor.eboard.dms.service.DmsStorageService;
import com.bor.eboard.dms.service.DmsFilePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DmsAdministrationServiceImpl implements DmsAdministrationService {

    private static final String HEALTHY = "HEALTHY";
    private static final String WARNING = "WARNING";
    private static final String UNHEALTHY = "UNHEALTHY";

    private final DmsAdministrationRepository administrationRepository;
    private final DmsStorageService storageService;
    private final DmsFilePolicyService filePolicyService;
    private final DmsProperties properties;

    public DmsAdministrationServiceImpl(
            DmsAdministrationRepository administrationRepository,
            DmsStorageService storageService,
            DmsFilePolicyService filePolicyService,
            DmsProperties properties) {
        this.administrationRepository = administrationRepository;
        this.storageService = storageService;
        this.filePolicyService = filePolicyService;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsAdministrationDashboardResponse getDashboard() {
        DmsAdministrationRepository.DashboardMetrics metrics =
                administrationRepository.loadDashboardMetrics();
        StorageHealthResponse storage = storageService.checkHealth();
        long missingMandatoryMetadata =
                administrationRepository.countDocumentsMissingMandatoryMetadata();
        double metadataCompletionPercent = metrics.totalDocuments() == 0
                ? 100.0
                : Math.round(((metrics.totalDocuments() - missingMandatoryMetadata) * 1000.0)
                        / metrics.totalDocuments()) / 10.0;
        return new DmsAdministrationDashboardResponse(
                metrics.totalDocuments(),
                metrics.activeDocuments(),
                metrics.archivedDocuments(),
                metrics.uploadedToday(),
                metrics.uploadedThisWeek(),
                metrics.uploadedThisMonth(),
                metrics.totalVersions(),
                metrics.totalStorageBytes(),
                metrics.activeDocumentTypes(),
                metrics.activeMetadataFields(),
                metrics.activeMasterSources(),
                metrics.activeShares(),
                metrics.auditEvents(),
                metrics.indexedDocuments(),
                metrics.activeUsersLast30Days(),
                metrics.inactiveDocuments(),
                metrics.searchesToday(),
                metrics.searchesThisWeek(),
                missingMandatoryMetadata,
                metadataCompletionPercent,
                administrationRepository.loadDocumentsByType(),
                administrationRepository.loadDocumentsByDepartment(),
                administrationRepository.loadDocumentsBySection(),
                administrationRepository.loadMostViewedDocuments(),
                administrationRepository.loadMostDownloadedDocuments(),
                administrationRepository.loadRecentUploads(),
                administrationRepository.loadRecentActivities(),
                storage,
                LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public DmsAdministrationHealthResponse checkHealth() {
        List<DmsConfigurationCheckResponse> checks = new ArrayList<>();

        boolean databaseHealthy = administrationRepository.databaseAvailable();
        checks.add(new DmsConfigurationCheckResponse(
                "DATABASE",
                databaseHealthy ? HEALTHY : UNHEALTHY,
                databaseHealthy ? "DMS database connection is available" : "DMS database connection is unavailable"));

        StorageHealthResponse storage = storageService.checkHealth();
        checks.add(new DmsConfigurationCheckResponse(
                "STORAGE",
                storage.healthy() ? HEALTHY : UNHEALTHY,
                storage.message()));

        DmsUploadPolicyResponse uploadPolicy = filePolicyService.getPolicy();
        boolean uploadPolicyHealthy = uploadPolicy.maxFileSizeBytes() > 0
                && uploadPolicy.maxMetadataBytes() > 0
                && !uploadPolicy.allowedExtensions().isEmpty()
                && !uploadPolicy.allowedMimeTypes().isEmpty();
        checks.add(new DmsConfigurationCheckResponse(
                "UPLOAD_POLICY",
                uploadPolicyHealthy ? HEALTHY : UNHEALTHY,
                uploadPolicyHealthy
                        ? "DMS upload policy is configured"
                        : "DMS upload policy requires positive limits and non-empty allowlists"));

        long withoutMetadata = administrationRepository.countDocumentTypesWithoutMetadata();
        checks.add(new DmsConfigurationCheckResponse(
                "DOCUMENT_TYPE_METADATA",
                withoutMetadata == 0 ? HEALTHY : WARNING,
                withoutMetadata == 0
                        ? "All active document types have active metadata fields"
                        : withoutMetadata + " active document type(s) have no active metadata fields"));

        long unindexed = administrationRepository.countUnindexedDocuments();
        checks.add(new DmsConfigurationCheckResponse(
                "SEARCH_INDEX",
                unindexed == 0 ? HEALTHY : WARNING,
                unindexed == 0
                        ? "All DMS documents are indexed"
                        : unindexed + " DMS document(s) are missing from the search index"));

        long inactiveMasterSources = administrationRepository.countInactiveReferencedMasterSources();
        checks.add(new DmsConfigurationCheckResponse(
                "MASTER_SOURCES",
                inactiveMasterSources == 0 ? HEALTHY : WARNING,
                inactiveMasterSources == 0
                        ? "Active metadata fields reference only active master sources"
                        : inactiveMasterSources + " inactive master source(s) are referenced by active metadata fields"));

        boolean moduleEnabled = properties.isEnabled();
        checks.add(new DmsConfigurationCheckResponse(
                "MODULE",
                moduleEnabled ? HEALTHY : UNHEALTHY,
                moduleEnabled ? "DMS module is enabled" : "DMS module is disabled"));

        boolean healthy = moduleEnabled
                && checks.stream().noneMatch(check -> UNHEALTHY.equals(check.status()));
        return new DmsAdministrationHealthResponse(
                healthy,
                moduleEnabled,
                storage,
                List.copyOf(checks),
                LocalDateTime.now());
    }
}
