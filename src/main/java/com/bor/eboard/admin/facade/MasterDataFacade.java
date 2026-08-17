package com.bor.eboard.admin.facade;

import java.util.Map;
import java.util.UUID;

/**
 * Cross-module entry point into the Admin/Masters module
 * (02_ARCHITECTURE.md section 25).
 */
public interface MasterDataFacade {

    boolean categoryExists(UUID categoryId);

    boolean priorityExists(UUID priorityId);

    boolean languageExists(UUID languageId);

    Map<UUID, String> categoryNames();

    Map<UUID, String> priorityNames();

    Map<UUID, String> languageNames();
}
