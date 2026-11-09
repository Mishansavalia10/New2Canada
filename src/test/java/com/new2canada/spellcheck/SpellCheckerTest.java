package com.new2canada.spellcheck;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpellCheckerTest {

    @Test
    void knownWordIsReturnedAsIs() {
        SpellChecker sc = new SpellChecker();
        assertTrue(sc.isValid("Apartment"));
        assertEquals(List.of("apartment"), sc.suggest("Apartment"));
    }

    @Test
    void misspellingGetsCorrection() {
        SpellChecker sc = new SpellChecker();
        List<String> s = sc.suggest("apartmnt", 3);
        assertFalse(s.isEmpty());
        assertEquals("apartment", s.get(0));
    }

    @Test
    void blankInputGivesNoSuggestions() {
        SpellChecker sc = new SpellChecker();
        assertTrue(sc.suggest("   ").isEmpty());
        assertTrue(sc.suggest(null).isEmpty());
    }

    @Test
    void learnedWordsBecomeValidTokens() {
        SpellChecker sc = new SpellChecker();
        assertFalse(sc.isValid("scarborough"));
        sc.learn("Scarborough, ON!");
        assertTrue(sc.isValid("scarborough"));
    }
}
