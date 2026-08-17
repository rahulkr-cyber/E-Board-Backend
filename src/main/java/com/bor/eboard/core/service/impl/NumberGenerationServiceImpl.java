package com.bor.eboard.core.service.impl;

import com.bor.eboard.core.entity.NumberSequence;
import com.bor.eboard.core.repository.NumberSequenceRepository;
import com.bor.eboard.core.service.NumberGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class NumberGenerationServiceImpl implements NumberGenerationService {

    private final NumberSequenceRepository numberSequenceRepository;

    /**
     * MANDATORY participation in the caller's transaction: the allocated
     * number must roll back together with the business record it was
     * generated for, so no diary/file row can exist without its number
     * and vice versa.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public GeneratedNumber next(String sequenceKey, String prefix) {
        int year = Year.now().getValue();

        NumberSequence sequence = numberSequenceRepository
                .lockByKeyAndYear(sequenceKey, year)
                .orElseGet(() -> initializeSequence(sequenceKey, year));

        long nextValue = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(nextValue);
        numberSequenceRepository.save(sequence);

        String formatted = String.format("%s/%d/%06d", prefix, year, nextValue);
        return new GeneratedNumber(formatted, year, nextValue);
    }

    /**
     * First allocation of a key/year (e.g. the first diary entry of a new
     * year). A concurrent initializer may win the unique-constraint race,
     * in which case we lock and use the winner's row.
     */
    private NumberSequence initializeSequence(String sequenceKey, int year) {
        try {
            NumberSequence created = new NumberSequence();
            created.setSequenceKey(sequenceKey);
            created.setSequenceYear(year);
            created.setCurrentValue(0L);
            numberSequenceRepository.saveAndFlush(created);
            return numberSequenceRepository.lockByKeyAndYear(sequenceKey, year)
                    .orElse(created);
        } catch (DataIntegrityViolationException e) {
            return numberSequenceRepository.lockByKeyAndYear(sequenceKey, year)
                    .orElseThrow(() -> e);
        }
    }
}
