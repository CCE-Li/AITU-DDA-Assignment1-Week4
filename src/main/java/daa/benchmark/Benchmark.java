package daa.benchmark;

import daa.ds.DynamicArray;
import daa.ds.IntList;
import daa.ds.MinHeap;
import daa.ds.MyLinkedList;
import daa.metrics.Metrics;
import daa.metrics.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Runs the four workloads of the assignment for n = 100, 1 000, 10 000 and 100 000.
 *
 * <p>Every case is measured once for warm-up and then five times; the median of the five measured
 * runs is reported, together with the physical operations counted during the last measured run.
 * The counters are reset after the untimed fill phase, so {@code steps}, {@code moves} and
 * {@code comparisons} describe the workload itself and not the setup.
 *
 * <p>The whole benchmark is also executed once without recording ({@link #runAll(boolean)}) so
 * that the JIT has compiled every code path before the measured runs start.
 */
public final class Benchmark {

    public static final int SEED = 42;
    public static final int[] SIZES = {100, 1_000, 10_000, 100_000};

    public static final String W1 = "W1_random_access";
    public static final String W2 = "W2_search";
    public static final String W3 = "W3_insert_remove";
    public static final String W4 = "W4_priority_processing";
    public static final String NO_VARIANT = "-";
    public static final String HEAD = "head";
    public static final String MIDDLE = "middle";

    private static final int WARMUP_RUNS = 1;
    private static final int MEASURED_RUNS = 5;
    private static final int W1_GET_CALLS = 10_000;
    private static final int W2_QUERIES = 1_000;
    private static final int W3_OPERATIONS = 1_000;

    private Benchmark() {
    }

    /**
     * Runs every case of W1-W4 for every size.
     *
     * @param report {@code false} for the JIT warm-up pass, which measures the same cases but
     *               prints nothing, so that the recorded pass starts on compiled code
     */
    public static List<Result> runAll(boolean report) {
        List<Result> results = new ArrayList<>();
        for (int n : SIZES) {
            int[] values = shuffledValues(n);

            results.add(measure(W1, NO_VARIANT, "DynamicArray", n,
                    randomAccessCase(DynamicArray::new, values, n), report));
            results.add(measure(W1, NO_VARIANT, "MyLinkedList", n,
                    randomAccessCase(MyLinkedList::new, values, n), report));

            results.add(measure(W2, NO_VARIANT, "DynamicArray", n,
                    searchCase(DynamicArray::new, values), report));
            results.add(measure(W2, NO_VARIANT, "MyLinkedList", n,
                    searchCase(MyLinkedList::new, values), report));

            results.add(measure(W3, HEAD, "DynamicArray", n, insertRemoveCase(DynamicArray::new, values, 0), report));
            results.add(measure(W3, HEAD, "MyLinkedList", n, insertRemoveCase(MyLinkedList::new, values, 0), report));
            results.add(measure(W3, MIDDLE, "DynamicArray", n, insertRemoveCase(DynamicArray::new, values, n / 2), report));
            results.add(measure(W3, MIDDLE, "MyLinkedList", n, insertRemoveCase(MyLinkedList::new, values, n / 2), report));

            results.add(measure(W4, NO_VARIANT, "MinHeap", n, priorityCase(values), report));
        }
        return results;
    }

    /** One workload: build and fill the structure, then run the operations that are timed. */
    private interface Case {

        /** Builds the structure and fills it; this phase is not timed and not counted. */
        void fill(Metrics metrics);

        /** The measured operations. */
        void run();

        /** Consumed results, so that the JIT cannot throw the workload away. */
        long checksum();
    }

    private static Result measure(String workload, String variant, String structure, int n, Case benchmarkCase,
                                  boolean report) {
        double[] times = new double[MEASURED_RUNS];
        long steps = 0;
        long moves = 0;
        long comparisons = 0;

        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Metrics metrics = new Metrics();
            benchmarkCase.fill(metrics);
            metrics.reset();

            long started = System.nanoTime();
            benchmarkCase.run();
            long elapsed = System.nanoTime() - started;

            if (run >= WARMUP_RUNS) {
                times[run - WARMUP_RUNS] = elapsed / 1_000_000.0;
                steps = metrics.steps();
                moves = metrics.moves();
                comparisons = metrics.comparisons();
            }
        }

        double median = median(times);
        if (report) {
            System.out.printf("  %-22s %-8s %-13s n=%-7d %10.3f ms  steps=%-12d moves=%-11d comparisons=%-12d (checksum %d)%n",
                    workload, variant, structure, n, median, steps, moves, comparisons, benchmarkCase.checksum());
        }
        return new Result(workload, variant, structure, n, median, steps, moves, comparisons);
    }

    /** W1: 10 000 random {@code get(index)} calls on the filled structure. */
    private static Case randomAccessCase(Function<Metrics, IntList> factory, int[] values, int n) {
        int[] indices = randomIndices(n);
        return new Case() {
            private IntList list;
            private long checksum;

            @Override
            public void fill(Metrics metrics) {
                list = factory.apply(metrics);
                for (int value : values) {
                    list.add(value);
                }
            }

            @Override
            public void run() {
                long total = 0;
                for (int index : indices) {
                    total += list.get(index);
                }
                checksum = total;
            }

            @Override
            public long checksum() {
                return checksum;
            }
        };
    }

    /** W2: 1 000 {@code contains(x)} queries, half of the values present and half absent. */
    private static Case searchCase(Function<Metrics, IntList> factory, int[] values) {
        int[] queries = searchQueries(values);
        return new Case() {
            private IntList list;
            private long checksum;

            @Override
            public void fill(Metrics metrics) {
                list = factory.apply(metrics);
                for (int value : values) {
                    list.add(value);
                }
            }

            @Override
            public void run() {
                long hits = 0;
                for (int query : queries) {
                    if (list.contains(query)) {
                        hits++;
                    }
                }
                checksum = hits;
            }

            @Override
            public long checksum() {
                return checksum;
            }
        };
    }

    /** W3: 1 000 insertions followed by 1 000 removals, all at the same index. */
    private static Case insertRemoveCase(Function<Metrics, IntList> factory, int[] values, int index) {
        int[] inserts = randomValues(W3_OPERATIONS);
        return new Case() {
            private IntList list;
            private long checksum;

            @Override
            public void fill(Metrics metrics) {
                list = factory.apply(metrics);
                for (int value : values) {
                    list.add(value);
                }
            }

            @Override
            public void run() {
                long total = 0;
                for (int value : inserts) {
                    list.add(index, value);
                    total += value;
                }
                for (int i = 0; i < inserts.length; i++) {
                    total += list.remove(index);
                }
                checksum = total;
            }

            @Override
            public long checksum() {
                return checksum;
            }
        };
    }

    /** W4: n insertions, then n {@code extractMin()} calls that must be non-decreasing. */
    private static Case priorityCase(int[] values) {
        return new Case() {
            private MinHeap heap;
            private long checksum;

            @Override
            public void fill(Metrics metrics) {
                heap = new MinHeap(metrics);
                for (int value : values) {
                    heap.insert(value);
                }
            }

            @Override
            public void run() {
                long total = 0;
                int previous = Integer.MIN_VALUE;
                for (int i = 0; i < values.length; i++) {
                    int value = heap.extractMin();
                    if (value < previous) {
                        throw new IllegalStateException("extractMin() returned " + value + " after " + previous);
                    }
                    previous = value;
                    total += value;
                }
                checksum = total;
            }

            @Override
            public long checksum() {
                return checksum;
            }
        };
    }

    /** Distinct values 0 .. n-1 in a random order, generated with {@code new Random(42)}. */
    public static int[] shuffledValues(int n) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = i;
        }
        Random random = new Random(SEED);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = values[i];
            values[i] = values[j];
            values[j] = swap;
        }
        return values;
    }

    private static int[] randomIndices(int n) {
        Random random = new Random(SEED);
        int[] indices = new int[W1_GET_CALLS];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = random.nextInt(n);
        }
        return indices;
    }

    private static int[] searchQueries(int[] values) {
        Random random = new Random(SEED);
        int n = values.length;
        int[] queries = new int[W2_QUERIES];
        for (int i = 0; i < queries.length; i++) {
            if (i % 2 == 0) {
                queries[i] = values[random.nextInt(n)];
            } else {
                queries[i] = n + random.nextInt(n);
            }
        }
        return queries;
    }

    private static int[] randomValues(int count) {
        Random random = new Random(SEED);
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = random.nextInt();
        }
        return values;
    }

    private static double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }
}
