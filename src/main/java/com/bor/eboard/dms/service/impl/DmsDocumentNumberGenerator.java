package com.bor.eboard.dms.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class DmsDocumentNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public DmsDocumentNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next() {
        Long sequence = jdbcTemplate.queryForObject(
                "SELECT nextval('dms_document_number_seq')",
                Long.class);
        if (sequence == null) {
            throw new IllegalStateException("Unable to generate DMS document number");
        }
        return "DMS-" + Year.now().getValue() + "-" + String.format("%010d", sequence);
    }
}
