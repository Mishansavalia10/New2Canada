package com.new2canada.ranking;

import com.new2canada.models.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultSorterTest {

    private static SearchResult doc(String id, double score) {
        SearchResult r = new SearchResult(id, "apartments", id, "", "", "src");
        r.setScore(score);
        return r;
    }

    @Test
    void sortsDescendingByScore() {
        List<SearchResult> sorted = ResultSorter.sortDesc(List.of(
                doc("low", 0.1), doc("high", 9.0), doc("mid", 3.5), doc("top", 12.0)));
        assertEquals(List.of("top", "high", "mid", "low"),
                sorted.stream().map(SearchResult::getId).toList());
    }

    @Test
    void equalScoresKeepInputOrder() {
        List<SearchResult> sorted = ResultSorter.sortDesc(List.of(
                doc("first", 5.0), doc("second", 5.0), doc("third", 5.0)));
        assertEquals(List.of("first", "second", "third"),
                sorted.stream().map(SearchResult::getId).toList());
    }

    @Test
    void doesNotMutateInput() {
        List<SearchResult> input = List.of(doc("a", 1.0), doc("b", 2.0));
        ResultSorter.sortDesc(input);
        assertEquals("a", input.get(0).getId());
    }

    @Test
    void handlesNullAndSingleton() {
        assertTrue(ResultSorter.sortDesc(null).isEmpty());
        assertEquals(1, ResultSorter.sortDesc(List.of(doc("x", 1))).size());
    }
}
