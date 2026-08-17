package com.bor.eboard.correspondence.mapper;

import java.util.Map;
import java.util.UUID;

/**
 * Bundle of reference-name maps resolved once per request via the
 * Identity and MasterData facades, then passed into the mapper.
 * Keeps the "no JPA associations" rule intact while avoiding N+1 lookups.
 */
public record CorrespondenceLookups(
        Map<UUID, String> categories,
        Map<UUID, String> priorities,
        Map<UUID, String> languages,
        Map<UUID, String> departments,
        Map<UUID, String> sections,
        Map<UUID, String> users) {

    public String category(UUID id) {
        return id != null ? categories.get(id) : null;
    }

    public String priority(UUID id) {
        return id != null ? priorities.get(id) : null;
    }

    public String language(UUID id) {
        return id != null ? languages.get(id) : null;
    }

    public String department(UUID id) {
        return id != null ? departments.get(id) : null;
    }

    public String section(UUID id) {
        return id != null ? sections.get(id) : null;
    }

    public String user(UUID id) {
        return id != null ? users.get(id) : null;
    }
}
