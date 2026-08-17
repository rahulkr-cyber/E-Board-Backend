package com.bor.eboard.core.service;

/**
 * System-only number generation (03_DATABASE.md sections 17-19).
 * Numbers are unique per year, reset yearly, and can never be
 * assigned or edited manually.
 */
public interface NumberGenerationService {

    /**
     * Result of allocating the next number in a sequence.
     */
    record GeneratedNumber(String formatted, int year, long sequence) {
    }

    /**
     * Allocates the next value for the given key in the current year and
     * formats it as {prefix}/{YEAR}/{SEQUENCE(6 digits)}.
     * Example key "DIARY" with prefix "BOR" -> BOR/2026/000001.
     */
    GeneratedNumber next(String sequenceKey, String prefix);
}
