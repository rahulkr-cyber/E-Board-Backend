package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDocumentAccessSummaryResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsDocumentAccessScope;

public interface DmsDocumentAuthorizationService {

    DmsDocumentAccessScope currentScope();

    DmsDocumentAccessSummaryResponse summarize(DmsDocument document);

    void require(DmsDocument document, DmsDocumentAccessLevel accessLevel);
}
