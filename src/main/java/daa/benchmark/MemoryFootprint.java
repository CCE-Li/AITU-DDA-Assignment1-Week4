package daa.benchmark;

import daa.ds.DynamicArray;
import daa.ds.MinHeap;
import daa.ds.MyLinkedList;
import daa.metrics.CsvWriter;
import daa.metrics.Metrics;
import daa.metrics.Plotter;
import daa.metrics.Series;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bonus A: real memory footprint of the three structures, measured with JOL
 * ({@link GraphLayout#parseInstance(Object)} walks the whole object graph, including the
 * backing array, every node object and every reference).
 */
public final class MemoryFootprint {

    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};

    private MemoryFootprint() {
    }

    private interface StructureFactory {
        Object create(int n);
    }

    public static void measure(Path resultsDirectory) throws IOException {
        Map<String, StructureFactory> structures = new LinkedHashMap<>();
        structures.put("DynamicArray", n -> {
            DynamicArray array = new DynamicArray(new Metrics());
            for (int value : Benchmark.shuffledValues(n)) {
                array.add(value);
            }
            return array;
        });
        structures.put("MyLinkedList", n -> {
            MyLinkedList list = new MyLinkedList(new Metrics());
            for (int value : Benchmark.shuffledValues(n)) {
                list.add(value);
            }
            return list;
        });
        structures.put("MinHeap", n -> {
            MinHeap heap = new MinHeap(new Metrics());
            for (int value : Benchmark.shuffledValues(n)) {
                heap.insert(value);
            }
            return heap;
        });

        List<String> csvRows = new ArrayList<>();
        List<Series> curves = new ArrayList<>();
        System.out.println();
        System.out.println("Bonus A - memory footprint (JOL)");

        for (Map.Entry<String, StructureFactory> entry : structures.entrySet()) {
            double[] x = new double[SIZES.length];
            double[] megabytes = new double[SIZES.length];
            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i];
                long bytes = GraphLayout.parseInstance(entry.getValue().create(n)).totalSize();
                x[i] = n;
                megabytes[i] = bytes / (1024.0 * 1024.0);
                csvRows.add(String.join(",",
                        entry.getKey(),
                        Integer.toString(n),
                        Long.toString(bytes),
                        String.format(Locale.ROOT, "%.2f", (double) bytes / n),
                        String.format(Locale.ROOT, "%.4f", megabytes[i])));
                System.out.printf("  %-13s n=%-7d %10d bytes  %8.2f bytes/element  %7.4f MB%n",
                        entry.getKey(), n, bytes, (double) bytes / n, megabytes[i]);
            }
            curves.add(new Series(entry.getKey(), x, megabytes));
        }

        CsvWriter.writeTable(resultsDirectory.resolve("memory.csv"),
                "structure,n,bytes,bytes_per_element,mb", csvRows);
        Plotter.writeLineChart("Memory vs n", "Memory (MB)", false, curves,
                resultsDirectory.resolve("plots/memory_vs_n.png"));
        System.out.println("  saved results/memory.csv and results/plots/memory_vs_n.png");
    }
}
