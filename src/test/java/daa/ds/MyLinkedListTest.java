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

class MyLinkedListTest {

    private static MyLinkedList newList() {
        return new MyLinkedList(new Metrics());
    }

    @Test
    void matchesArrayListOnRandomOperations() {
        Random random = new Random(42);
        MyLinkedList list = newList();
        List<Integer> oracle = new ArrayList<>();

        for (int round = 0; round < 5_000; round++) {
            int operation = oracle.isEmpty() ? random.nextInt(2) : random.nextInt(5);
            switch (operation) {
                case 0, 1 -> {
                    int value = random.nextInt(1_000);
                    list.add(value);
                    oracle.add(value);
                }
                case 2 -> {
                    int index = random.nextInt(oracle.size() + 1);
                    int value = random.nextInt(1_000);
                    list.add(index, value);
                    oracle.add(index, value);
                }
                case 3 -> {
                    int index = random.nextInt(oracle.size());
                    assertEquals(oracle.remove(index), list.remove(index));
                }
                default -> {
                    int index = random.nextInt(oracle.size());
                    assertEquals(oracle.get(index), list.get(index));
                }
            }
            assertEquals(oracle.size(), list.size());
            assertEquals(oracle.isEmpty(), list.isEmpty());
        }

        for (int i = 0; i < oracle.size(); i++) {
            assertEquals(oracle.get(i), list.get(i));
        }
    }

    @Test
    void handlesEmptyAndSingleElement() {
        MyLinkedList list = newList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.contains(0));

        list.add(5);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
        assertTrue(list.contains(5));
        assertFalse(list.contains(6));
        assertEquals(5, list.remove(0));
        assertTrue(list.isEmpty());

        list.add(9);
        assertEquals(9, list.get(0));
    }

    @Test
    void reachesFirstAndLastIndex() {
        MyLinkedList list = newList();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        assertEquals(0, list.get(0));
        assertEquals(99, list.get(99));
        assertEquals(0, list.remove(0));
        assertEquals(99, list.remove(98));
        assertEquals(98, list.size());
        assertEquals(1, list.get(0));
        assertEquals(98, list.get(97));

        list.add(0, -1);
        list.add(list.size(), 1_000);
        assertEquals(-1, list.get(0));
        assertEquals(1_000, list.get(list.size() - 1));

        assertEquals(1_000, list.remove(list.size() - 1));
        assertEquals(98, list.get(list.size() - 1));
    }

    @Test
    void handlesDuplicateValues() {
        MyLinkedList list = newList();
        for (int i = 0; i < 5; i++) {
            list.add(7);
        }
        list.add(9);
        assertTrue(list.contains(7));
        assertTrue(list.contains(9));
        assertFalse(list.contains(8));
        assertEquals(7, list.remove(0));
        assertTrue(list.contains(7));
    }

    @Test
    void rejectsInvalidIndices() {
        MyLinkedList list = newList();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 5));

        list.add(7);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(2, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 5));
    }

    @Test
    void getWalksOneNodePerStep() {
        Metrics metrics = new Metrics();
        MyLinkedList list = new MyLinkedList(metrics);
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        metrics.reset();
        assertEquals(0, list.get(0));
        assertEquals(0, metrics.steps());

        metrics.reset();
        assertEquals(7, list.get(7));
        assertEquals(7, metrics.steps());

        metrics.reset();
        assertTrue(list.contains(9));
        assertEquals(9, metrics.steps());
        assertEquals(10, metrics.comparisons());
    }

    @Test
    void boundaryInsertAndRemoveTouchOnlyLinks() {
        Metrics metrics = new Metrics();
        MyLinkedList list = new MyLinkedList(metrics);
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        metrics.reset();
        list.add(0, -1);
        assertEquals(0, metrics.steps());
        assertEquals(2, metrics.moves());

        metrics.reset();
        list.remove(0);
        assertEquals(0, metrics.steps());
        assertEquals(1, metrics.moves());

        metrics.reset();
        list.add(list.size(), 1_000);
        assertEquals(0, metrics.steps());
        assertEquals(2, metrics.moves());
    }

    @Test
    void middleInsertWalksHalfTheList() {
        Metrics metrics = new Metrics();
        MyLinkedList list = new MyLinkedList(metrics);
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        metrics.reset();
        list.add(50, -1);
        assertEquals(49, metrics.steps());
        assertEquals(2, metrics.moves());
    }
}
