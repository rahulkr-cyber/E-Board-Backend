package com.bor.eboard.dms.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.dms.dto.DmsDocumentSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentSearchResponse;

public interface DmsSearchService {

    PageResponse<DmsDocumentSearchResponse> search(DmsDocumentSearchRequest request);
}
