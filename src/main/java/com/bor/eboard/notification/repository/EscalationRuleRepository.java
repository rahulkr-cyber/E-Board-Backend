package com.bor.eboard.notification.repository;

import com.bor.eboard.notification.entity.EscalationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, UUID> {

    List<EscalationRule> findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc();
}
