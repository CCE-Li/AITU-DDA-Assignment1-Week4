# Assignment 2 — In-Memory Workload Engine

Three data structures written from scratch, an operation counter that is honest to the last
physical read, four benchmark workloads, JUnit 5 tests and the charts of the report.

`DynamicArray`, `MyLinkedList` and `MinHeap` use `int` values only — no generics, no
`java.util` collections, no hidden boxing. The benchmark writes `results/results.csv`, the
report charts and the two bonus tables with a single command.

## Requirements

* JDK 21
* Maven 3.9 or newer

## Build and test

```bash
mvn -q package        # compiles, runs the JUnit 5 tests and builds target/daa-assignment2.jar
mvn test              # tests only
```

## Run the benchmark

```bash
mvn -q compile exec:java
```

The command produces:

| File | Content |
| --- | --- |
| `results/results.csv` | 36 rows: W1–W4 × n = 100, 1 000, 10 000, 100 000 × structure |
| `results/plots/*.png` | `time vs n` and `steps / moves / comparisons vs n` per workload |
| `results/memory.csv`, `results/plots/memory_vs_n.png` | Bonus A, measured with JOL |
| `results/buildheap.csv`, `results/plots/buildheap_*.png` | Bonus B, Floyd's `buildHeap` against n inserts |

The same run can be started from the jar:

```bash
mvn -q package
java -jar target/daa-assignment2.jar
```

## Layout

```
src/main/java/daa/ds          IntList, DynamicArray, MyLinkedList, MinHeap
src/main/java/daa/metrics     Metrics, Result, CsvWriter, Plotter, Series
src/main/java/daa/benchmark   Benchmark, Main, MemoryFootprint, BuildHeapBenchmark
src/test/java/daa             JUnit 5 tests (ds and metrics packages)
results/                      results.csv, memory.csv, buildheap.csv, plots/
REPORT.md                     complexity table, loop invariant proofs, plots, discussion
```

## What the counters mean

All three structures report into `daa.metrics.Metrics`, and every tick is one physical
operation performed inside the method that performs it:

* **steps** — one read of an array cell, or one hop to the next node of a list;
* **moves** — one write of an array cell (element shift, placement, grow-copy), or one link
  update in a list (`head`, `tail` or a `next` field);
* **comparisons** — one comparison between two stored elements.

Bookkeeping such as `i < size` compares an index with a length, not two elements, and is not
counted. Elements are copied with explicit loops instead of `System.arraycopy`, so that the
grow-copy of `DynamicArray` and `MinHeap` shows up in the numbers.

## Benchmark methodology

* every size uses the same data: the values `0 … n-1` shuffled with `new Random(42)`;
* sizes `n = 100, 1 000, 10 000, 100 000`;
* the whole benchmark is executed once without recording, so the JIT has compiled every code
  path before the measured runs start;
* every case then runs once more for warm-up and five times for the record; the reported time
  is the median of the five measured runs, and the counters come from the last measured run;
* the counters are reset after the untimed fill phase, so they describe the workload itself
  and not the setup;
* the workloads are the ones required by the assignment: W1 10 000 random `get(index)` calls,
  W2 1 000 `contains(x)` queries (half present, half absent), W3 1 000 insertions plus
  1 000 removals at index `0` (`head`) or `n / 2` (`middle`), W4 n `insert(x)` calls followed
  by n `extractMin()` calls that must come out in non-decreasing order.
