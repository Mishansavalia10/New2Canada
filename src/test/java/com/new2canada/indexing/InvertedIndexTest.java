package com.new2canada.indexing;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvertedIndexTest {

    @Test
    void andSearchReturnsOnlyDocumentsContainingEveryTerm() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument("a", "bright apartment near the subway");
        idx.addDocument("b", "bright studio downtown");
        idx.addDocument("c", "apartment with parking");

        assertEquals(Set.of("a", "b"), idx.search("bright"));
        assertEquals(Set.of("a"), idx.search("bright apartment"));
        assertTrue(idx.search("bright parking").isEmpty());
    }

    @Test
    void emptyOrStopWordOnlyQueryReturnsNothing() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument("a", "a big apartment");
        assertTrue(idx.search("").isEmpty());
        assertTrue(idx.search("the and of").isEmpty());
    }

    @Test
    void readdingSameDocumentDoesNotDuplicatePostings() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument("a", "apartment apartment");
        idx.addDocument("a", "apartment apartment");   // crawler refresh

        assertEquals(1, idx.postings("apartment").size());
        assertEquals(1, idx.totalDocuments());
    }

    @Test
    void readdingDocumentReplacesOldTerms() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument("a", "old listing about parking");
        idx.addDocument("a", "new listing about balcony");

        assertTrue(idx.search("parking").isEmpty(), "stale term must be dropped");
        assertEquals(Set.of("a"), idx.search("balcony"));
        assertTrue(idx.postings("parking").isEmpty());
    }

    @Test
    void removeDocumentDropsItsPostingsAndTerms() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument("a", "unique zebra");
        idx.addDocument("b", "shared word");
        idx.removeDocument("a");

        assertTrue(idx.search("zebra").isEmpty());
        assertFalse(idx.terms().contains("zebra"));
        assertEquals(1, idx.totalDocuments());
    }

    @Test
    void nullOrBlankInputsAreIgnored() {
        InvertedIndex idx = new InvertedIndex();
        idx.addDocument(null, "text");
        idx.addDocument("", "text");
        idx.addDocument("a", null);
        assertEquals(0, idx.totalDocuments());
    }
}
