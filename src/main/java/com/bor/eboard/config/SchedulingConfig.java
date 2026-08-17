package com.bor.eboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution for the escalation sweep
 * (08_WORKFLOW_ENGINE.md section 12, 06_BUSINESS_RULES.md section 10).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
