package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsStorageConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DmsStorageConfigurationRepository
        extends JpaRepository<DmsStorageConfiguration, UUID> {

    Optional<DmsStorageConfiguration>
    findFirstByPrimaryProviderTrueAndActiveTrueAndDeletedFalse();

    Optional<DmsStorageConfiguration>
    findByProviderCodeIgnoreCaseAndActiveTrueAndDeletedFalse(String providerCode);

    Optional<DmsStorageConfiguration>
    findByProviderCodeIgnoreCaseAndDeletedFalse(String providerCode);

    List<DmsStorageConfiguration> findByPrimaryProviderTrueAndDeletedFalse();
}
