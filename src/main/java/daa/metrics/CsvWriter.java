package daa.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes benchmark rows to CSV. */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void write(Path path, List<Result> results) throws IOException {
        Path directory = path.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        StringBuilder csv = new StringBuilder(Result.CSV_HEADER).append('\n');
        for (Result result : results) {
            csv.append(result.toCsv()).append('\n');
        }
        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    /** Writes a header plus one line per table row; used by the bonus measurements. */
    public static void writeTable(Path path, String header, List<String> rows) throws IOException {
        Path directory = path.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        StringBuilder csv = new StringBuilder(header).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }
}
