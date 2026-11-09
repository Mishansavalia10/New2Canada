package com.new2canada.spellcheck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditDistanceTest {

    @Test
    void classicExamples() {
        assertEquals(3, EditDistance.distance("kitten", "sitting"));
        assertEquals(2, EditDistance.distance("flaw", "lawn"));
        assertEquals(1, EditDistance.distance("apartmnt", "apartment"));
    }

    @Test
    void identicalAndEmptyStrings() {
        assertEquals(0, EditDistance.distance("toronto", "toronto"));
        assertEquals(0, EditDistance.distance("", ""));
        assertEquals(5, EditDistance.distance("", "hello"));
        assertEquals(5, EditDistance.distance("hello", null));
    }

    @Test
    void isSymmetric() {
        assertEquals(EditDistance.distance("saturday", "sunday"),
                     EditDistance.distance("sunday", "saturday"));
    }
}
