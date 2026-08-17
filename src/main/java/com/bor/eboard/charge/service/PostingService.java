package com.bor.eboard.charge.service;

import com.bor.eboard.charge.dto.CreatePostingRequest;
import com.bor.eboard.charge.dto.PostingResponse;
import com.bor.eboard.charge.dto.PostingTimelineEntryResponse;
import com.bor.eboard.charge.entity.JoiningRelieving;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.identity.facade.IdentityFacade;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PostingService {
    PostingTimelineEntryResponse create(CreatePostingRequest request);
    List<PostingTimelineEntryResponse> getByUser(UUID userId);
    PostingResponse history(UUID userId);
    PostingTimelineEntryResponse uploadOrderDocument(UUID postingId, MultipartFile file);
    PostingTimelineEntryResponse addSupportingDocument(UUID postingId, MultipartFile file);

    void recordTransferPosting(TransferHistory transfer, IdentityFacade.UserRef previousUser);
    void applyJoiningRelieving(JoiningRelieving event);

    void syncSourceAttachment(String sourceEntityType, UUID sourceEntityId, UUID attachmentId);
}
