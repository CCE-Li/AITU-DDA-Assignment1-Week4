package daa.ds;

import daa.metrics.Metrics;

/**
 * Growable array of primitive ints, written from scratch.
 *
 * <p>When the backing array is full it is replaced by one twice as large, which makes
 * {@link #add(int)} amortized O(1). Elements are copied one by one instead of using
 * {@code System.arraycopy} so that every physical read and write is counted honestly.
 */
public final class DynamicArray implements IntList {

    private static final int DEFAULT_CAPACITY = 10;

    private final Metrics metrics;
    private int[] data;
    private int size;

    public DynamicArray(Metrics metrics) {
        this.metrics = metrics;
        this.data = new int[DEFAULT_CAPACITY];
    }

    @Override
    public void add(int value) {
        ensureCapacity(size + 1);
        data[size] = value;
        metrics.move();
        size++;
    }

    @Override
    public void add(int index, int value) {
        checkIndexForAdd(index);
        ensureCapacity(size + 1);
        for (int i = size; i > index; i--) {
            metrics.step();
            data[i] = data[i - 1];
            metrics.move();
        }
        data[index] = value;
        metrics.move();
        size++;
    }

    @Override
    public int remove(int index) {
        checkIndex(index);
        int removed = data[index];
        metrics.step();
        for (int i = index; i < size - 1; i++) {
            metrics.step();
            data[i] = data[i + 1];
            metrics.move();
        }
        size--;
        return removed;
    }

    @Override
    public int get(int index) {
        checkIndex(index);
        metrics.step();
        return data[index];
    }

    @Override
    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            metrics.step();
            metrics.comparison();
            if (data[i] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** Current length of the backing array; exposed for tests of the doubling growth. */
    int capacity() {
        return data.length;
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

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
        }
    }

    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
        }
    }
}
