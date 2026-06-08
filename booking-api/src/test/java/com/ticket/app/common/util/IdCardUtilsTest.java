package com.ticket.app.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IdCardUtilsTest {

    @Test
    void validMainlandIdCardShouldPassChecksumAndBirthdayChecks() {
        assertTrue(IdCardUtils.isValid("11010519491231002X"));
    }

    @Test
    void invalidChecksumShouldFail() {
        assertFalse(IdCardUtils.isValid("110105194912310021"));
    }

    @Test
    void invalidBirthdayShouldFail() {
        assertFalse(IdCardUtils.isValid("11010519991331002X"));
    }

    @Test
    void calculateAgeShouldUseBirthdayAndReferenceDate() {
        assertEquals(18, IdCardUtils.calculateAge("11010520080608002X", LocalDate.of(2026, 6, 8)));
        assertEquals(17, IdCardUtils.calculateAge("110105200806090025", LocalDate.of(2026, 6, 8)));
    }

    @Test
    void maskShouldKeepFirstAndLastCharacters() {
        assertEquals("110105********002X", IdCardUtils.mask("11010519491231002X"));
    }
}
