package daa.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsTest {

    @Test
    void countersStartAtZeroAndAccumulate() {
        Metrics metrics = new Metrics();
        assertEquals(0, metrics.steps());
        assertEquals(0, metrics.moves());
        assertEquals(0, metrics.comparisons());

        metrics.step();
        metrics.step();
        metrics.move();
        metrics.comparison();

        assertEquals(2, metrics.steps());
        assertEquals(1, metrics.moves());
        assertEquals(1, metrics.comparisons());
    }

    @Test
    void resetClearsEveryCounter() {
        Metrics metrics = new Metrics();
        metrics.step();
        metrics.move();
        metrics.comparison();

        metrics.reset();

        assertEquals(0, metrics.steps());
        assertEquals(0, metrics.moves());
        assertEquals(0, metrics.comparisons());

        metrics.step();
        assertEquals(1, metrics.steps());
    }

    @Test
    void csvUsesTheColumnLayoutOfTheAssignment(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("results.csv");
        CsvWriter.write(file, List.of(
                new Result("W1_random_access", "-", "DynamicArray", 100, 0.542, 10_000, 0, 0),
                new Result("W3_insert_remove", "middle", "MyLinkedList", 1_000, 1.5, 1_000, 1_000_999, 0)));

        assertEquals("""
                workload,variant,structure,n,time_ms,steps,moves,comparisons
                W1_random_access,-,DynamicArray,100,0.542,10000,0,0
                W3_insert_remove,middle,MyLinkedList,1000,1.500,1000,1000999,0
                """, Files.readString(file));
    }
}
