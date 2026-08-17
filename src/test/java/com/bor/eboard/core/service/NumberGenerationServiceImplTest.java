package com.bor.eboard.core.service;

import com.bor.eboard.core.entity.NumberSequence;
import com.bor.eboard.core.repository.NumberSequenceRepository;
import com.bor.eboard.core.service.impl.NumberGenerationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NumberGenerationService")
class NumberGenerationServiceImplTest {

    @Mock
    private NumberSequenceRepository numberSequenceRepository;

    @InjectMocks
    private NumberGenerationServiceImpl service;

    private final int currentYear = Year.now().getValue();

    @Test
    @DisplayName("formats the next number as PREFIX/YEAR/6-digit-sequence and increments")
    void allocatesAndFormats() {
        NumberSequence existing = new NumberSequence();
        existing.setSequenceKey("DIARY");
        existing.setSequenceYear(currentYear);
        existing.setCurrentValue(41L);
        when(numberSequenceRepository.lockByKeyAndYear("DIARY", currentYear))
                .thenReturn(Optional.of(existing));

        NumberGenerationService.GeneratedNumber result = service.next("DIARY", "BOR");

        assertThat(result.sequence()).isEqualTo(42L);
        assertThat(result.year()).isEqualTo(currentYear);
        assertThat(result.formatted()).isEqualTo(String.format("BOR/%d/000042", currentYear));

        ArgumentCaptor<NumberSequence> captor = ArgumentCaptor.forClass(NumberSequence.class);
        verify(numberSequenceRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("initializes a new key/year sequence starting at 1")
    void initializesNewSequence() {
        when(numberSequenceRepository.lockByKeyAndYear(eq("FILE"), eq(currentYear)))
                .thenReturn(Optional.empty())          // first lookup: nothing yet
                .thenAnswer(inv -> {                    // after insert: return the row
                    NumberSequence created = new NumberSequence();
                    created.setSequenceKey("FILE");
                    created.setSequenceYear(currentYear);
                    created.setCurrentValue(0L);
                    return Optional.of(created);
                });

        NumberGenerationService.GeneratedNumber result = service.next("FILE", "BOR");

        assertThat(result.sequence()).isEqualTo(1L);
        assertThat(result.formatted()).isEqualTo(String.format("BOR/%d/000001", currentYear));
        verify(numberSequenceRepository).saveAndFlush(any(NumberSequence.class));
    }
}
