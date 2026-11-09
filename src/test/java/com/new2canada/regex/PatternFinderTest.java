package com.new2canada.regex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternFinderTest {

    @Test
    void fourDigitPriceKeepsAllDigits() {
        // Regression: "$1,850" must not be read as 185.
        assertEquals(1850.0, PatternFinder.firstPriceAsNumber("Rent $1,850/mo", -1));
        assertEquals(1850.0, PatternFinder.firstPriceAsNumber("Rent $1850/mo", -1));
    }

    @Test
    void centsAndCadPrefixAreParsed() {
        assertEquals(1250.50, PatternFinder.firstPriceAsNumber("CAD $1,250.50", -1));
    }

    @Test
    void fallbackWhenNoPrice() {
        assertEquals(-1.0, PatternFinder.firstPriceAsNumber("call for price", -1));
        assertEquals(-1.0, PatternFinder.firstPriceAsNumber(null, -1));
    }

    @Test
    void findsAllPricesInOrder() {
        assertEquals(List.of("$900", "$1,200"),
                PatternFinder.findPrices("Studio $900 or 2 bed $1,200 utilities extra"));
    }

    @Test
    void findsPercentsEmailsAndDates() {
        assertEquals(List.of("5%", "2.5 %"), PatternFinder.findPercents("up 5% and 2.5 %"));
        assertEquals(List.of("a.b@c.ca"), PatternFinder.findEmails("mail a.b@c.ca now"));
        assertFalse(PatternFinder.findDates("Available 2026-09-01").isEmpty());
    }
}
