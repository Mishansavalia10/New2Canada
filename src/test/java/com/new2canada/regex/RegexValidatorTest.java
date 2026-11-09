package com.new2canada.regex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RegexValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"K1A 0B1", "k1a0b1", "M5V3L9", "T2P 1J9"})
    void acceptsValidCanadianPostalCodes(String pc) {
        assertTrue(RegexValidator.isPostalCode(pc), pc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"D1A 0B1", "K1O 0B1", "K1A 0B", "12345", "", "K1A-0B1"})
    void rejectsInvalidPostalCodes(String pc) {
        assertFalse(RegexValidator.isPostalCode(pc), pc);
    }

    @Test
    void emailValidation() {
        assertTrue(RegexValidator.isEmail("student@uwindsor.ca"));
        assertFalse(RegexValidator.isEmail("student@uwindsor"));
        assertFalse(RegexValidator.isEmail("no-at-sign.ca"));
        assertFalse(RegexValidator.isEmail(null));
    }

    @Test
    void phoneValidation() {
        assertTrue(RegexValidator.isPhone("519-253-3000"));
        assertTrue(RegexValidator.isPhone("(519) 253-3000"));
        assertFalse(RegexValidator.isPhone("253-3000"));
    }

    @Test
    void sinValidation() {
        assertTrue(RegexValidator.isSin("046 454 286"));
        assertTrue(RegexValidator.isSin("046-454-286"));
        assertFalse(RegexValidator.isSin("04645428"));
    }

    @Test
    void dispatcherRoutesTypesAndRejectsUnknown() {
        assertTrue(RegexValidator.validate("POSTAL", "N9A 1B2"));
        assertTrue(RegexValidator.validate("email", "a@b.co"));
        assertFalse(RegexValidator.validate("credit-card", "4111111111111111"));
        assertFalse(RegexValidator.validate(null, "x"));
    }
}
