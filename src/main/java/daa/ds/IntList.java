package daa.ds;

/**
 * The five operations shared by {@link DynamicArray} and {@link MyLinkedList}, so that one
 * benchmark can run both structures through the same code.
 */
public interface IntList {

    /** Appends {@code value} at the end of the structure. */
    void add(int value);

    /** Inserts {@code value} at {@code index}; {@code index == size()} appends. */
    void add(int index, int value);

    /** Removes and returns the element at {@code index}, shifting the rest left. */
    int remove(int index);

    /** Returns the element at {@code index}. */
    int get(int index);

    /** Returns {@code true} if {@code value} is stored at least once. */
    boolean contains(int value);

    /** Number of stored elements. */
    int size();

    /** Returns {@code true} if nothing is stored. */
    boolean isEmpty();
}
