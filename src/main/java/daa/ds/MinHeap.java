package daa.ds;

import daa.metrics.Metrics;

/**
 * Array-based binary min-heap of primitive ints, written from scratch.
 *
 * <p>{@code data} is a complete binary tree stored level by level: the children of node
 * {@code i} are {@code 2i + 1} and {@code 2i + 2}, the parent of node {@code i} is
 * {@code (i - 1) / 2}. Every parent is less than or equal to its children.
 */
public final class MinHeap {

    private static final int DEFAULT_CAPACITY = 10;

    private final Metrics metrics;
    private int[] data;
    private int size;

    public MinHeap(Metrics metrics) {
        this.metrics = metrics;
        this.data = new int[DEFAULT_CAPACITY];
    }

    /** Appends {@code value} and restores the heap property by bubbling it up. */
    public void insert(int value) {
        ensureCapacity(size + 1);
        data[size] = value;
        metrics.move();
        size++;
        bubbleUp(size - 1);
    }

    /** Returns the smallest element without removing it. */
    public int peekMin() {
        if (size == 0) {
            throw new IllegalStateException("peekMin() on an empty heap");
        }
        metrics.step();
        return data[0];
    }

    /** Removes and returns the smallest element, then restores the heap property. */
    public int extractMin() {
        if (size == 0) {
            throw new IllegalStateException("extractMin() on an empty heap");
        }
        metrics.step();
        int minimum = data[0];
        size--;
        if (size > 0) {
            metrics.step();
            data[0] = data[size];
            metrics.move();
            bubbleDown(0);
        }
        return minimum;
    }

    /**
     * Floyd's bottom-up heap construction: copies {@code array} and then sifts down every node
     * that has children, from the last one up to the root. Total work is O(n), because the
     * nodes high in the tree, which sift the farthest, are few.
     */
    public void buildHeap(int[] array) {
        ensureCapacity(array.length);
        for (int i = 0; i < array.length; i++) {
            data[i] = array[i];
            metrics.move();
        }
        size = array.length;
        for (int i = size / 2 - 1; i >= 0; i--) {
            bubbleDown(i);
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Copy of the internal tree, level by level; exposed so tests can check the heap property. */
    int[] heapArray() {
        int[] copy = new int[size];
        for (int i = 0; i < size; i++) {
            copy[i] = data[i];
        }
        return copy;
    }

    private void bubbleUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            metrics.step();
            int parentValue = data[parent];
            metrics.step();
            int value = data[index];
            metrics.comparison();
            if (parentValue <= value) {
                return;
            }
            data[parent] = value;
            metrics.move();
            data[index] = parentValue;
            metrics.move();
            index = parent;
        }
    }

    private void bubbleDown(int index) {
        while (true) {
            int left = 2 * index + 1;
            int right = left + 1;
            int smallest = index;
            if (left < size) {
                metrics.step();
                metrics.comparison();
                if (data[left] < data[smallest]) {
                    smallest = left;
                }
            }
            if (right < size) {
                metrics.step();
                metrics.comparison();
                if (data[right] < data[smallest]) {
                    smallest = right;
                }
            }
            if (smallest == index) {
                return;
            }
            metrics.step();
            int value = data[index];
            metrics.step();
            data[index] = data[smallest];
            metrics.move();
            data[smallest] = value;
            metrics.move();
            index = smallest;
        }
    }

    private void ensureCapacity(int required) {
        if (required <= data.length) {
            return;
        }
        int newCapacity = data.length * 2;
        if (newCapacity < required) {
            newCapacity = required;
        }
        int[] grown = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            metrics.step();
            grown[i] = data[i];
            metrics.move();
        }
        data = grown;
    }
}
