package com.bor.eboard.dms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bootstrap configuration for the independent DMS module.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DmsProperties.class)
public class DmsConfiguration {
}
