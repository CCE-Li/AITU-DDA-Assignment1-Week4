package daa.benchmark;

import daa.ds.MinHeap;
import daa.metrics.CsvWriter;
import daa.metrics.Metrics;
import daa.metrics.Plotter;
import daa.metrics.Series;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Bonus B: Floyd's bottom-up {@code buildHeap} in O(n) against n separate {@code insert(x)}
 * calls in O(n log n).
 *
 * <p>Both are measured on two input orders. Random values are the best case for single inserts,
 * because a new value stops bubbling after 1.6 levels on average; a descending sequence is the
 * worst case, because every new value is smaller than all of its predecessors and has to reach
 * the root. Each case is timed as an empty structure that is filled once, with one warm-up run
 * and the median of five measured runs, and reports the operations counted while building.
 */
public final class BuildHeapBenchmark {

    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static final int WARMUP_RUNS = 1;
    private static final int MEASURED_RUNS = 5;

    private BuildHeapBenchmark() {
    }

    public static void measure(Path resultsDirectory) throws IOException {
        System.out.println();
        System.out.println("Bonus B - O(n) buildHeap vs n inserts");

        List<String> csvRows = new ArrayList<>();
        for (Order order : Order.values()) {
            double[] sizes = new double[SIZES.length];
            double[] insertTimes = new double[SIZES.length];
            double[] buildTimes = new double[SIZES.length];
            double[] insertComparisons = new double[SIZES.length];
            double[] buildComparisons = new double[SIZES.length];

            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i];
                int[] values = order.values(n);
                sizes[i] = n;

                Measurement inserts = measure(() -> {
                    Metrics metrics = new Metrics();
                    MinHeap heap = new MinHeap(metrics);
                    for (int value : values) {
                        heap.insert(value);
                    }
                    return metrics;
                });
                Measurement build = measure(() -> {
                    Metrics metrics = new Metrics();
                    MinHeap heap = new MinHeap(metrics);
                    heap.buildHeap(values);
                    return metrics;
                });

                insertTimes[i] = inserts.timeMs();
                buildTimes[i] = build.timeMs();
                insertComparisons[i] = inserts.comparisons();
                buildComparisons[i] = build.comparisons();

                csvRows.add(row("n_inserts", order, n, inserts));
                csvRows.add(row("buildHeap", order, n, build));
                System.out.printf("  %-10s n=%-7d n_inserts %9.3f ms comparisons=%-10d | buildHeap %8.3f ms comparisons=%-10d%n",
                        order.label, n, inserts.timeMs(), inserts.comparisons(),
                        build.timeMs(), build.comparisons());
            }

            List<Series> timeCurves = List.of(
                    new Series("n inserts (O(n log n))", sizes, insertTimes),
                    new Series("buildHeap (O(n))", sizes, buildTimes));
            List<Series> comparisonCurves = List.of(
                    new Series("n inserts (O(n log n))", sizes, insertComparisons),
                    new Series("buildHeap (O(n))", sizes, buildComparisons));

            Plotter.writeLineChart("buildHeap vs n inserts - " + order.label + " input - Time vs n",
                    "Time (ms)", true, timeCurves,
                    resultsDirectory.resolve("plots/buildheap_" + order.label + "_time_vs_n.png"));
            Plotter.writeLineChart("buildHeap vs n inserts - " + order.label + " input - Comparisons vs n",
                    "comparisons", true, comparisonCurves,
                    resultsDirectory.resolve("plots/buildheap_" + order.label + "_comparisons_vs_n.png"));
        }

        CsvWriter.writeTable(resultsDirectory.resolve("buildheap.csv"),
                "method,order,n,time_ms,steps,moves,comparisons", csvRows);
        System.out.println("  saved results/buildheap.csv and 4 charts in results/plots/");
    }

    /** The two input orders of the bonus experiment. */
    private enum Order {
        RANDOM("random") {
            @Override
            int[] values(int n) {
                return Benchmark.shuffledValues(n);
            }
        },
        DESCENDING("descending") {
            @Override
            int[] values(int n) {
                int[] values = new int[n];
                for (int i = 0; i < n; i++) {
                    values[i] = n - 1 - i;
                }
                return values;
            }
        };

        private final String label;

        Order(String label) {
            this.label = label;
        }

        abstract int[] values(int n);
    }

    private record Measurement(double timeMs, long steps, long moves, long comparisons) {
    }

    private static String row(String method, Order order, int n, Measurement measurement) {
        return String.join(",",
                method,
                order.label,
                Integer.toString(n),
                String.format(Locale.ROOT, "%.3f", measurement.timeMs()),
                Long.toString(measurement.steps()),
                Long.toString(measurement.moves()),
                Long.toString(measurement.comparisons()));
    }

    private static Measurement measure(Supplier<Metrics> build) {
        double[] times = new double[MEASURED_RUNS];
        Metrics last = null;
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            long started = System.nanoTime();
            last = build.get();
            long elapsed = System.nanoTime() - started;
            if (run >= WARMUP_RUNS) {
                times[run - WARMUP_RUNS] = elapsed / 1_000_000.0;
            }
        }
        double[] sorted = Arrays.copyOf(times, times.length);
        Arrays.sort(sorted);
        return new Measurement(sorted[sorted.length / 2], last.steps(), last.moves(), last.comparisons());
    }
}
