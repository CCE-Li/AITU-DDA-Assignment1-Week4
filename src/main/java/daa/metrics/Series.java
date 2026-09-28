package daa.metrics;

/**
 * A named curve on a chart: {@code y[i]} is the value measured at {@code x[i]}.
 * Used by the bonus measurements, which do not follow the workload CSV layout.
 */
public record Series(String label, double[] x, double[] y) {
}
