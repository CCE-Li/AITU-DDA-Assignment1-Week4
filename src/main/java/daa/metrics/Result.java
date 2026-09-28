package daa.metrics;

import java.util.Locale;

/**
 * One row of {@code results/results.csv}: the median time of one workload, one data structure,
 * one input size, together with the physical operations counted during the measured phase.
 */
public record Result(String workload, String variant, String structure, int n,
                     double timeMs, long steps, long moves, long comparisons) {

    public static final String CSV_HEADER = "workload,variant,structure,n,time_ms,steps,moves,comparisons";

    public String toCsv() {
        return String.join(",",
                workload,
                variant,
                structure,
                Integer.toString(n),
                String.format(Locale.ROOT, "%.3f", timeMs),
                Long.toString(steps),
                Long.toString(moves),
                Long.toString(comparisons));
    }
}
