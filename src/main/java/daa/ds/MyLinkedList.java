package daa.ds;

import daa.metrics.Metrics;

/**
 * Singly linked list of primitive ints, written from scratch.
 *
 * <p>{@code head} and {@code tail} references keep appending and boundary access cheap, but
 * reaching the i-th node still costs i hops: that is the pointer chasing this structure is
 * measured for.
 */
public final class MyLinkedList implements IntList {

    private static final class Node {
        final int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    private final Metrics metrics;
    private Node head;
    private Node tail;
    private int size;

    public MyLinkedList(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void add(int value) {
        Node node = new Node(value);
        if (tail == null) {
            head = node;
            metrics.move();
        } else {
            tail.next = node;
            metrics.move();
        }
        tail = node;
        metrics.move();
        size++;
    }

    @Override
    public void add(int index, int value) {
        checkIndexForAdd(index);
        if (index == size) {
            add(value);
            return;
        }
        Node node = new Node(value);
        if (index == 0) {
            node.next = head;
            metrics.move();
            head = node;
            metrics.move();
        } else {
            Node previous = nodeAt(index - 1);
            node.next = previous.next;
            metrics.move();
            previous.next = node;
            metrics.move();
        }
        size++;
    }

    @Override
    public int remove(int index) {
        checkIndex(index);
        Node target;
        if (index == 0) {
            target = head;
            head = head.next;
            metrics.move();
            if (head == null) {
                tail = null;
                metrics.move();
            }
        } else {
            Node previous = nodeAt(index - 1);
            target = previous.next;
            previous.next = target.next;
            metrics.move();
            if (target == tail) {
                tail = previous;
                metrics.move();
            }
        }
        size--;
        return target.value;
    }

    @Override
    public int get(int index) {
        checkIndex(index);
        return nodeAt(index).value;
    }

    @Override
    public boolean contains(int value) {
        Node current = head;
        while (current != null) {
            metrics.comparison();
            if (current.value == value) {
                return true;
            }
            current = current.next;
            metrics.step();
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

    /** Walks from the head; every hop is one counted step. */
    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
            metrics.step();
        }
        return current;
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
