package daa.ds;

import daa.benchmark.Benchmark;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinHeapTest {

    private static MinHeap newHeap() {
        return new MinHeap(new Metrics());
    }

    @Test
    void heapPropertyHoldsAfterEveryInsertAndExtractMin() {
        Random random = new Random(42);
        MinHeap heap = newHeap();

        for (int i = 0; i < 500; i++) {
            heap.insert(random.nextInt(1_000));
            assertHeapProperty(heap);
        }
        while (!heap.isEmpty()) {
            heap.extractMin();
            assertHeapProperty(heap);
        }
    }

    @Test
    void extractMinReturnsNonDecreasingValues() {
        Random random = new Random(7);
        int[] values = new int[1_000];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(100);
        }

        MinHeap heap = newHeap();
        for (int value : values) {
            heap.insert(value);
        }

        int[] expected = Arrays.copyOf(values, values.length);
        Arrays.sort(expected);
        for (int value : expected) {
            assertEquals(value, heap.extractMin());
        }
        assertTrue(heap.isEmpty());
    }

    @Test
    void matchesPriorityQueueOnRandomOperations() {
        Random random = new Random(11);
        MinHeap heap = newHeap();
        PriorityQueue<Integer> oracle = new PriorityQueue<>(Comparator.naturalOrder());

        for (int round = 0; round < 2_000; round++) {
            if (oracle.isEmpty() || random.nextBoolean()) {
                int value = random.nextInt(500);
                heap.insert(value);
                oracle.add(value);
            } else {
                assertEquals(oracle.poll(), heap.extractMin());
            }
            assertEquals(oracle.size(), heap.size());
        }

        while (!oracle.isEmpty()) {
            assertEquals(oracle.poll(), heap.extractMin());
        }
        assertTrue(heap.isEmpty());
    }

    @Test
    void handlesDuplicatesAndSingleElement() {
        MinHeap heap = newHeap();
        assertTrue(heap.isEmpty());
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);

        heap.insert(5);
        heap.insert(5);
        heap.insert(5);
        assertEquals(3, heap.size());
        assertEquals(5, heap.peekMin());
        assertEquals(3, heap.size());
        assertEquals(5, heap.extractMin());
        assertEquals(5, heap.extractMin());
        assertEquals(5, heap.extractMin());
        assertTrue(heap.isEmpty());
        assertThrows(IllegalStateException.class, heap::extractMin);
    }

    @Test
    void peekMinDoesNotRemove() {
        MinHeap heap = newHeap();
        heap.insert(3);
        heap.insert(1);
        heap.insert(2);
        assertEquals(1, heap.peekMin());
        assertEquals(1, heap.peekMin());
        assertEquals(3, heap.size());
        assertFalse(heap.isEmpty());
    }

    @Test
    void buildHeapProducesTheSameSortedSequence() {
        Random random = new Random(3);
        int[] values = new int[500];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(1_000);
        }

        MinHeap heap = newHeap();
        heap.buildHeap(values);
        assertHeapProperty(heap);
        assertEquals(values.length, heap.size());

        int[] expected = Arrays.copyOf(values, values.length);
        Arrays.sort(expected);
        for (int value : expected) {
            assertEquals(value, heap.extractMin());
        }
    }

    @Test
    void buildHeapComparesLessThanSeparateInserts() {
        int[] values = Benchmark.shuffledValues(10_000);

        Metrics inserts = new Metrics();
        MinHeap byInsert = new MinHeap(inserts);
        for (int value : values) {
            byInsert.insert(value);
        }

        Metrics build = new Metrics();
        MinHeap byBuild = new MinHeap(build);
        byBuild.buildHeap(values);

        assertEquals(values.length, byInsert.size());
        assertEquals(values.length, byBuild.size());
        assertTrue(build.comparisons() < inserts.comparisons(),
                "buildHeap made " + build.comparisons() + " comparisons, n inserts made " + inserts.comparisons());
    }

    private static void assertHeapProperty(MinHeap heap) {
        int[] data = heap.heapArray();
        for (int parent = 0; parent < data.length; parent++) {
            int left = 2 * parent + 1;
            int right = left + 1;
            if (left < data.length) {
                assertTrue(data[parent] <= data[left],
                        "parent " + data[parent] + " at " + parent + " > left child " + data[left]);
            }
            if (right < data.length) {
                assertTrue(data[parent] <= data[right],
                        "parent " + data[parent] + " at " + parent + " > right child " + data[right]);
            }
        }
    }
}
