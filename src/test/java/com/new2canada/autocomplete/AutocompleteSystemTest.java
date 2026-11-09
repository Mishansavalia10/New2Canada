package com.new2canada.autocomplete;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteSystemTest {

    @Test
    void suggestionsAreOrderedByFrequency() {
        AutocompleteSystem ac = new AutocompleteSystem();
        ac.indexDocument("parking parking parking");
        ac.indexDocument("parka");
        ac.indexDocument("park");
        ac.indexDocument("park");

        assertEquals("parking", ac.suggest("par", 3).get(0));
        assertEquals(3, ac.suggest("par", 3).size());
    }

    @Test
    void limitIsRespected() {
        AutocompleteSystem ac = new AutocompleteSystem();
        ac.indexDocument("bed bedroom bedding bedside");
        assertEquals(2, ac.suggest("bed", 2).size());
    }

    @Test
    void prefixIsNormalisedAndEmptyPrefixGivesNothing() {
        AutocompleteSystem ac = new AutocompleteSystem();
        ac.indexDocument("kitchen");
        assertEquals("kitchen", ac.suggest("  KIT ", 5).get(0));
        assertTrue(ac.suggest("   ", 5).isEmpty());
    }
}
