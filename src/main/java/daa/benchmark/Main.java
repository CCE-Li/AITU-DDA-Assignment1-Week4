package daa.benchmark;

import daa.metrics.CsvWriter;
import daa.metrics.Plotter;
import daa.metrics.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * One command produces every deliverable of the benchmark:
 * {@code results/results.csv}, the charts in {@code results/plots/} and the two bonus tables.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path resultsDirectory = Path.of("results");
        Path csv = resultsDirectory.resolve("results.csv");
        Path plots = resultsDirectory.resolve("plots");

        System.out.println("DAA Assignment 2 - in-memory workload engine");
        System.out.println("seed 42, n = 100, 1 000, 10 000, 100 000, 1 warm-up run + median of 5 runs");

        System.out.println();
        System.out.println("Warm-up pass, nothing is recorded:");
        Benchmark.runAll(false);

        System.out.println();
        System.out.println("Measured pass:");
        List<Result> results = Benchmark.runAll(true);
        CsvWriter.write(csv, results);
        System.out.println();
        System.out.println("Saved " + results.size() + " rows to " + csv.toAbsolutePath());

        Plotter.writeWorkloadCharts(results, plots);
        System.out.println("Saved workload charts to " + plots.toAbsolutePath());

        MemoryFootprint.measure(resultsDirectory);
        BuildHeapBenchmark.measure(resultsDirectory);
    }
}
