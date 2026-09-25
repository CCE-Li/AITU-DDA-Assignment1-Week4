package daa.ds;

import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicArrayTest {

    private static DynamicArray newArray() {
        return new DynamicArray(new Metrics());
    }

    @Test
    void matchesArrayListOnRandomOperations() {
        Random random = new Random(42);
        DynamicArray array = newArray();
        List<Integer> oracle = new ArrayList<>();

        for (int round = 0; round < 5_000; round++) {
            int operation = oracle.isEmpty() ? random.nextInt(2) : random.nextInt(5);
            switch (operation) {
                case 0, 1 -> {
                    int value = random.nextInt(1_000);
                    array.add(value);
                    oracle.add(value);
                }
                case 2 -> {
                    int index = random.nextInt(oracle.size() + 1);
                    int value = random.nextInt(1_000);
                    array.add(index, value);
                    oracle.add(index, value);
                }
                case 3 -> {
                    int index = random.nextInt(oracle.size());
                    assertEquals(oracle.remove(index), array.remove(index));
                }
                default -> {
                    int index = random.nextInt(oracle.size());
                    assertEquals(oracle.get(index), array.get(index));
                }
            }
            assertEquals(oracle.size(), array.size());
            assertEquals(oracle.isEmpty(), array.isEmpty());
        }

        for (int i = 0; i < oracle.size(); i++) {
            assertEquals(oracle.get(i), array.get(i));
        }
    }

    @Test
    void doublesTheBackingArrayWhenFull() {
        DynamicArray array = newArray();
        int capacity = array.capacity();
        assertTrue(capacity >= 1);

        for (int i = 0; i < capacity; i++) {
            array.add(i);
        }
        assertEquals(capacity, array.capacity());

        array.add(1_234);
        assertEquals(capacity * 2, array.capacity());
        for (int i = 0; i < capacity; i++) {
            assertEquals(i, array.get(i));
        }
        assertEquals(1_234, array.get(capacity));
    }

    @Test
    void handlesEmptyAndSingleElement() {
        DynamicArray array = newArray();
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
        assertFalse(array.contains(0));

        array.add(5);
        assertEquals(1, array.size());
        assertEquals(5, array.get(0));
        assertTrue(array.contains(5));
        assertFalse(array.contains(6));
        assertEquals(5, array.remove(0));
        assertTrue(array.isEmpty());
    }

    @Test
    void reachesFirstAndLastIndex() {
        DynamicArray array = newArray();
        for (int i = 0; i < 100; i++) {
            array.add(i);
        }
        assertEquals(0, array.get(0));
        assertEquals(99, array.get(99));
        assertEquals(0, array.remove(0));
        assertEquals(99, array.remove(98));
        assertEquals(98, array.size());
        assertEquals(1, array.get(0));
        assertEquals(98, array.get(97));

        array.add(0, -1);
        array.add(array.size(), 1_000);
        assertEquals(-1, array.get(0));
        assertEquals(1_000, array.get(array.size() - 1));
    }

    @Test
    void handlesDuplicateValues() {
        DynamicArray array = newArray();
        for (int i = 0; i < 5; i++) {
            array.add(7);
        }
        array.add(9);
        assertTrue(array.contains(7));
        assertTrue(array.contains(9));
        assertFalse(array.contains(8));
        assertEquals(7, array.remove(0));
        assertTrue(array.contains(7));
    }

    @Test
    void rejectsInvalidIndices() {
        DynamicArray array = newArray();
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(1, 5));

        array.add(7);
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(2, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(-1, 5));
    }

    @Test
    void countsReadsShiftsAndComparisons() {
        Metrics metrics = new Metrics();
        DynamicArray array = new DynamicArray(metrics);
        for (int i = 0; i < 10; i++) {
            array.add(i);
        }

        metrics.reset();
        array.get(4);
        assertEquals(1, metrics.steps());
        assertEquals(0, metrics.moves());

        metrics.reset();
        assertTrue(array.contains(9));
        assertEquals(10, metrics.steps());
        assertEquals(10, metrics.comparisons());

        metrics.reset();
        assertEquals(0, array.remove(0));
        assertEquals(10, metrics.steps());
        assertEquals(9, metrics.moves());
    }
}
