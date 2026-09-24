package daa.metrics;

/**
 * Physical operation counters that every structure in {@code daa.ds} reports into.
 *
 * <p>One counter tick means exactly one physical operation, counted inside the method that
 * performs it:
 *
 * <ul>
 *   <li>{@link #step()} - one read of an array cell, or one hop to the next node of a list;
 *   <li>{@link #move()} - one write of an array cell (element shift, placement, grow-copy),
 *       or one link update in a list ({@code head}, {@code tail} or a {@code next} field);
 *   <li>{@link #comparison()} - one comparison between two stored elements.
 * </ul>
 *
 * <p>Index bookkeeping such as {@code i < size} is not counted: it compares an index with a
 * length, not two elements.
 */
public final class Metrics {

    private long steps;
    private long moves;
    private long comparisons;

    /** One read of an array cell or one hop to the next node. */
    public void step() {
        steps++;
    }

    /** One element shifted inside an array, or one link (pointer) update in a list. */
    public void move() {
        moves++;
    }

    /** One comparison of two elements. */
    public void comparison() {
        comparisons++;
    }

    public long steps() {
        return steps;
    }

    public long moves() {
        return moves;
    }

    public long comparisons() {
        return comparisons;
    }

    /** Drops everything counted so far, e.g. after the untimed fill phase of a workload. */
    public void reset() {
        steps = 0;
        moves = 0;
        comparisons = 0;
    }
}
