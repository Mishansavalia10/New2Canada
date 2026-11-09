package com.new2canada.autocomplete;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TrieTest {

    @Test
    void insertCountsDistinctWordsOnly() {
        Trie t = new Trie();
        t.insert("toronto");
        t.insert("toronto");
        t.insert("toronto");
        t.insert("tor");
        assertEquals(2, t.size());
        assertTrue(t.contains("toronto"));
        assertFalse(t.contains("toront"));
    }

    @Test
    void findByPrefixReturnsAllCompletions() {
        Trie t = new Trie();
        for (String w : List.of("bed", "bedroom", "bedding", "bath")) t.insert(w);
        List<String> words = t.findByPrefix("bed").stream()
                .map(n -> n.word).sorted().collect(Collectors.toList());
        assertEquals(List.of("bed", "bedding", "bedroom"), words);
    }

    @Test
    void unknownPrefixReturnsEmpty() {
        Trie t = new Trie();
        t.insert("kitchen");
        assertTrue(t.findByPrefix("xyz").isEmpty());
    }
}
