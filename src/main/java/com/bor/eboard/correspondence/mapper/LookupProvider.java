package com.bor.eboard.correspondence.mapper;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the full reference-name bundle once per request via the Identity
 * and MasterData facades (no cross-module repository access).
 */
@Component
@RequiredArgsConstructor
public class LookupProvider {

    private final MasterDataFacade masterDataFacade;
    private final IdentityFacade identityFacade;

    @Transactional(readOnly = true)
    public CorrespondenceLookups load() {
        return new CorrespondenceLookups(
                masterDataFacade.categoryNames(),
                masterDataFacade.priorityNames(),
                masterDataFacade.languageNames(),
                identityFacade.departmentNames(),
                identityFacade.sectionNames(),
                identityFacade.userNames());
    }
}
