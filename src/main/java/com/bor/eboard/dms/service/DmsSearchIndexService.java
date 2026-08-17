package com.bor.eboard.dms.service;

import java.util.UUID;

public interface DmsSearchIndexService {

    void refresh(UUID documentId);
}
