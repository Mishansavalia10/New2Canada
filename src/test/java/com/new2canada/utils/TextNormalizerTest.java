package com.new2canada.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void tokenizeLowercasesSplitsAndDropsStopWords() {
        assertEquals(List.of("bright", "apartment", "downtown"),
                TextNormalizer.tokenize("The BRIGHT apartment, downtown!"));
    }

    @Test
    void tokenizeHandlesNullAndBlank() {
        assertTrue(TextNormalizer.tokenize(null).isEmpty());
        assertTrue(TextNormalizer.tokenize("   ").isEmpty());
    }

    @Test
    void normalizePrefixTrimsAndCollapsesSpaces() {
        assertEquals("new york", TextNormalizer.normalizePrefix("  New   York "));
        assertEquals("", TextNormalizer.normalizePrefix(null));
    }
}
