package com.emulator.lambda;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StreamComparatorTest {

    @Test
    void testIdenticalStreams() throws IOException {
        StreamComparator comparator = new StreamComparator();
        ByteArrayInputStream is1 = new ByteArrayInputStream("Hello World".getBytes());
        ByteArrayInputStream is2 = new ByteArrayInputStream("Hello World".getBytes());

        StreamComparator.ComparisonResult result = comparator.compare(is1, is2);
        assertTrue(result.areEqual());
        assertEquals(-1, result.mismatchIndex());
    }

    @Test
    void testDifferentContent() throws IOException {
        StreamComparator comparator = new StreamComparator();
        ByteArrayInputStream is1 = new ByteArrayInputStream("Hello World".getBytes());
        ByteArrayInputStream is2 = new ByteArrayInputStream("Hello Java".getBytes());

        StreamComparator.ComparisonResult result = comparator.compare(is1, is2);
        assertFalse(result.areEqual());
        assertEquals(6, result.mismatchIndex()); // Mismatch at 'W' vs 'J' (index 6)
    }

    @Test
    void testDifferentLengths() throws IOException {
        StreamComparator comparator = new StreamComparator();
        ByteArrayInputStream is1 = new ByteArrayInputStream("Hello".getBytes());
        ByteArrayInputStream is2 = new ByteArrayInputStream("Hello World".getBytes());

        StreamComparator.ComparisonResult result = comparator.compare(is1, is2);
        assertFalse(result.areEqual());
        // Mismatch should be at index 5 (where " " starts vs EOF)
        assertEquals(5, result.mismatchIndex());
    }

    @Test
    void testEmptyStreams() throws IOException {
        StreamComparator comparator = new StreamComparator();
        ByteArrayInputStream is1 = new ByteArrayInputStream(new byte[0]);
        ByteArrayInputStream is2 = new ByteArrayInputStream(new byte[0]);

        StreamComparator.ComparisonResult result = comparator.compare(is1, is2);
        assertTrue(result.areEqual());
    }
}
