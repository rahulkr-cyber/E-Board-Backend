package com.bor.eboard.admin.facade.impl;

import com.bor.eboard.admin.entity.Language;
import com.bor.eboard.admin.entity.LetterCategory;
import com.bor.eboard.admin.entity.Priority;
import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.admin.repository.LanguageRepository;
import com.bor.eboard.admin.repository.LetterCategoryRepository;
import com.bor.eboard.admin.repository.PriorityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MasterDataFacadeImpl implements MasterDataFacade {

    private final LetterCategoryRepository letterCategoryRepository;
    private final PriorityRepository priorityRepository;
    private final LanguageRepository languageRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean categoryExists(UUID categoryId) {
        return letterCategoryRepository.findByIdAndDeletedFalse(categoryId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean priorityExists(UUID priorityId) {
        return priorityRepository.findByIdAndDeletedFalse(priorityId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean languageExists(UUID languageId) {
        return languageRepository.findByIdAndDeletedFalse(languageId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> categoryNames() {
        return letterCategoryRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(LetterCategory::getId, LetterCategory::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> priorityNames() {
        return priorityRepository.findByDeletedFalseOrderBySortOrderAsc().stream()
                .collect(Collectors.toMap(Priority::getId, Priority::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> languageNames() {
        return languageRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Language::getId, Language::getName));
    }
}
