package com.bor.eboard.admin.repository;

import com.bor.eboard.admin.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findByIdAndDeletedFalse(UUID id);

    Optional<SystemSetting> findBySettingKeyAndDeletedFalse(String settingKey);

    boolean existsBySettingKeyAndDeletedFalse(String settingKey);

    List<SystemSetting> findByDeletedFalseOrderBySettingKeyAsc();
}
