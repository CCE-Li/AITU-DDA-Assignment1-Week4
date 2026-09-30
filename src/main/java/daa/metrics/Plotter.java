package daa.metrics;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the report charts. Both axes are logarithmic, tick labels are powers of ten
 * ({@code 10²}, {@code 10³}, ...), the grid is light grey and the legend sits inside the plot.
 */
public final class Plotter {

    private static final double X_MIN = 100.0;
    private static final double X_MAX = 100_000.0;

    /** Time (ms) axis of the report standard: 10^-2 up to 10^2. */
    private static final double TIME_MIN_MS = 0.01;
    private static final double TIME_MAX_MS = 100.0;

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 640;

    private static final Color GRID = new Color(215, 215, 215);
    private static final Color AXIS = new Color(90, 90, 90);
    private static final Color DYNAMIC_ARRAY = new Color(31, 119, 180);
    private static final Color MY_LINKED_LIST = new Color(255, 127, 14);
    private static final Color MIN_HEAP = new Color(44, 160, 44);
    private static final Color EXTRA = new Color(148, 103, 189);

    private static final float[] SOLID = null;
    private static final float[] DASHED = {10.0f, 5.0f};
    private static final float[] DOTTED = {2.0f, 4.0f};

    private static final Shape CIRCLE = new Ellipse2D.Double(-3.5, -3.5, 7.0, 7.0);
    private static final Shape SQUARE = new Rectangle2D.Double(-3.5, -3.5, 7.0, 7.0);
    private static final Shape TRIANGLE = triangle();

    private Plotter() {
    }

    /** Draws the {@code time vs n} and the {@code steps / moves / comparisons vs n} chart per workload. */
    public static void writeWorkloadCharts(List<Result> results, Path directory) throws IOException {
        Map<String, List<Result>> groups = new LinkedHashMap<>();
        for (Result result : results) {
            String key = result.variant().equals("-")
                    ? result.workload()
                    : result.workload() + "_" + result.variant();
            groups.computeIfAbsent(key, unused -> new ArrayList<>()).add(result);
        }
        for (Map.Entry<String, List<Result>> group : groups.entrySet()) {
            List<Result> rows = group.getValue();
            String caption = caption(rows.get(0));
            write(timeChart(caption, rows), directory.resolve(group.getKey() + "_time_vs_n.png"));
            JFreeChart operations = operationsChart(caption, rows);
            if (operations != null) {
                write(operations, directory.resolve(group.getKey() + "_ops_vs_n.png"));
            }
        }
    }

    /** Time (ms) against n on a log-log chart, both structures of the workload in one plot. */
    private static JFreeChart timeChart(String caption, List<Result> rows) {
        Map<String, List<Result>> byStructure = groupByStructure(rows);
        XYSeriesCollection dataset = new XYSeriesCollection();
        List<Color> colors = new ArrayList<>();
        List<float[]> dashes = new ArrayList<>();
        for (Map.Entry<String, List<Result>> entry : byStructure.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey());
            for (Result row : entry.getValue()) {
                series.add(row.n(), row.timeMs());
            }
            dataset.addSeries(series);
            colors.add(colorOf(entry.getKey()));
            dashes.add(dashOf(entry.getKey()));
        }

        JFreeChart chart = baseChart("Time vs n - " + caption, dataset);
        XYPlot plot = chart.getXYPlot();
        plot.setDomainAxis(logAxis("n (input size)", X_MIN, X_MAX));
        plot.setRangeAxis(logAxis("Time (ms)", TIME_MIN_MS, upperTimeBound(rows)));

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < colors.size(); i++) {
            style(renderer, i, colors.get(i), dashes.get(i), CIRCLE);
        }
        return chart;
    }

    /**
     * Steps, moves and comparisons against n. The colour encodes the structure, the line style
     * encodes the counter. Counters that stay at zero on every size are left out: a logarithmic
     * axis cannot show a zero value.
     */
    private static JFreeChart operationsChart(String caption, List<Result> rows) {
        Map<String, List<Result>> byStructure = groupByStructure(rows);
        boolean single = byStructure.size() == 1;

        XYSeriesCollection dataset = new XYSeriesCollection();
        List<Color> colors = new ArrayList<>();
        List<float[]> dashes = new ArrayList<>();
        List<Shape> markers = new ArrayList<>();

        for (Map.Entry<String, List<Result>> entry : byStructure.entrySet()) {
            List<Result> sizes = entry.getValue();
            String prefix = single ? "" : entry.getKey() + " ";
            Color color = colorOf(entry.getKey());
            addCounter(dataset, colors, dashes, markers, prefix + "steps", sizes, Counter.STEPS, color);
            addCounter(dataset, colors, dashes, markers, prefix + "moves", sizes, Counter.MOVES, color);
            addCounter(dataset, colors, dashes, markers, prefix + "comparisons", sizes, Counter.COMPARISONS, color);
        }
        if (dataset.getSeriesCount() == 0) {
            return null;
        }

        JFreeChart chart = baseChart("Steps / moves / comparisons vs n - " + caption, dataset);
        XYPlot plot = chart.getXYPlot();
        plot.setDomainAxis(logAxis("n (input size)", X_MIN, X_MAX));
        plot.setRangeAxis(autoLogAxis("operations"));

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < colors.size(); i++) {
            style(renderer, i, colors.get(i), dashes.get(i), markers.get(i));
        }
        return chart;
    }

    /**
     * Generic chart for the bonus measurements. {@code logarithmicY} picks a log or a linear
     * value axis; the x axis is log whenever every x value is positive.
     */
    public static void writeLineChart(String title, String yLabel, boolean logarithmicY,
                                      List<Series> series, Path file) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Series curve : series) {
            XYSeries xy = new XYSeries(curve.label());
            for (int i = 0; i < curve.x().length; i++) {
                xy.add(curve.x()[i], curve.y()[i]);
            }
            dataset.addSeries(xy);
        }

        JFreeChart chart = baseChart(title, dataset);
        XYPlot plot = chart.getXYPlot();
        boolean logarithmicX = allPositive(series);
        plot.setDomainAxis(logarithmicX
                ? logAxis("n (input size)", xMinOf(series), X_MAX)
                : new NumberAxis("n (input size)"));
        plot.setRangeAxis(logarithmicY ? autoLogAxis(yLabel) : new NumberAxis(yLabel));
        if (!logarithmicY) {
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setAutoRangeIncludesZero(true);
            yAxis.setTickUnit(new NumberTickUnit(niceStep(series)));
        }
        if (logarithmicX) {
            style(plot.getDomainAxis());
        }
        style(plot.getRangeAxis());

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            style(renderer, i, palette(i), curveDash(i), curveMarker(i));
        }
        write(chart, file);
    }

    private enum Counter {
        STEPS, MOVES, COMPARISONS
    }

    private static Map<String, List<Result>> groupByStructure(List<Result> rows) {
        Map<String, List<Result>> byStructure = new LinkedHashMap<>();
        for (Result row : rows) {
            byStructure.computeIfAbsent(row.structure(), unused -> new ArrayList<>()).add(row);
        }
        return byStructure;
    }

    private static void addCounter(XYSeriesCollection dataset, List<Color> colors, List<float[]> dashes,
                                   List<Shape> markers, String label, List<Result> rows, Counter counter,
                                   Color color) {
        long maximum = 0;
        for (Result row : rows) {
            maximum = Math.max(maximum, valueOf(row, counter));
        }
        if (maximum == 0) {
            return;
        }
        XYSeries series = new XYSeries(label);
        for (Result row : rows) {
            series.add(row.n(), valueOf(row, counter));
        }
        dataset.addSeries(series);

        colors.add(color);
        switch (counter) {
            case STEPS -> {
                dashes.add(SOLID);
                markers.add(CIRCLE);
            }
            case MOVES -> {
                dashes.add(DASHED);
                markers.add(SQUARE);
            }
            case COMPARISONS -> {
                dashes.add(DOTTED);
                markers.add(TRIANGLE);
            }
        }
    }

    private static long valueOf(Result row, Counter counter) {
        return switch (counter) {
            case STEPS -> row.steps();
            case MOVES -> row.moves();
            case COMPARISONS -> row.comparisons();
        };
    }

    private static JFreeChart baseChart(String title, XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(title, "n (input size)", "", dataset,
                PlotOrientation.VERTICAL, true, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));
        chart.getLegend().setFrame(new BlockBorder(0, 0, 0, 0));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);
        plot.setOutlinePaint(AXIS);
        plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        return chart;
    }

    private static LogAxis logAxis(String label, double minimum, double maximum) {
        LogAxis axis = autoLogAxis(label);
        axis.setRange(minimum, maximum);
        return axis;
    }

    private static LogAxis autoLogAxis(String label) {
        LogAxis axis = new LogAxis(label);
        axis.setBase(10.0);
        // one labelled tick per decade, so the axis reads 10², 10³, 10⁴ ...
        axis.setTickUnit(new NumberTickUnit(1.0), false, true);
        axis.setNumberFormatOverride(new SuperscriptNumberFormat());
        axis.setMinorTickMarksVisible(false);
        axis.setAutoRange(true);
        style(axis);
        return axis;
    }

    private static void style(org.jfree.chart.axis.ValueAxis axis) {
        axis.setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
        axis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        axis.setTickLabelPaint(Color.DARK_GRAY);
        axis.setLabelPaint(Color.BLACK);
    }

    private static void style(XYLineAndShapeRenderer renderer, int index, Color color, float[] dash, Shape marker) {
        renderer.setSeriesPaint(index, color);
        renderer.setSeriesStroke(index, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, dash, 0.0f));
        renderer.setSeriesShape(index, marker);
        renderer.setSeriesShapesVisible(index, true);
        renderer.setSeriesLinesVisible(index, true);
    }

    private static void write(JFreeChart chart, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            ChartUtils.writeChartAsPNG(out, chart, WIDTH, HEIGHT);
        }
    }

    /**
     * Keeps the 10^-2 .. 10^2 standard of the report and only widens it when a measurement is
     * larger, so that no data point is clipped by the axis.
     */
    private static double upperTimeBound(List<Result> rows) {
        double bound = TIME_MAX_MS;
        for (Result row : rows) {
            while (row.timeMs() > bound) {
                bound *= 10.0;
            }
        }
        return bound;
    }

    private static String caption(Result row) {
        String workload = row.workload().replace('_', ' ');
        return row.variant().equals("-") ? workload : workload + " (" + row.variant() + ")";
    }

    private static Color colorOf(String structure) {
        return switch (structure) {
            case "DynamicArray" -> DYNAMIC_ARRAY;
            case "MyLinkedList" -> MY_LINKED_LIST;
            case "MinHeap" -> MIN_HEAP;
            default -> EXTRA;
        };
    }

    private static float[] dashOf(String structure) {
        return switch (structure) {
            case "DynamicArray" -> SOLID;
            case "MyLinkedList" -> DASHED;
            default -> DOTTED;
        };
    }

    /** Line style of the i-th curve of a bonus chart: solid, dashed, dotted, repeating. */
    private static float[] curveDash(int index) {
        return switch (index % 3) {
            case 1 -> DASHED;
            case 2 -> DOTTED;
            default -> SOLID;
        };
    }

    private static Shape curveMarker(int index) {
        return switch (index % 3) {
            case 1 -> SQUARE;
            case 2 -> TRIANGLE;
            default -> CIRCLE;
        };
    }

    private static Color palette(int index) {
        return switch (index % 4) {
            case 0 -> DYNAMIC_ARRAY;
            case 1 -> MY_LINKED_LIST;
            case 2 -> MIN_HEAP;
            default -> EXTRA;
        };
    }

    /** A 1-2-5 step that keeps a linear axis at roughly six labels instead of dozens. */
    private static double niceStep(List<Series> series) {
        double maximum = 0.0;
        for (Series curve : series) {
            for (double y : curve.y()) {
                maximum = Math.max(maximum, y);
            }
        }
        if (maximum <= 0.0) {
            return 1.0;
        }
        double raw = maximum / 6.0;
        double magnitude = Math.pow(10.0, Math.floor(Math.log10(raw)));
        double normalized = raw / magnitude;
        double step = normalized <= 1.0 ? 1.0 : normalized <= 2.0 ? 2.0 : normalized <= 5.0 ? 5.0 : 10.0;
        return step * magnitude;
    }

    private static boolean allPositive(List<Series> series) {
        for (Series curve : series) {
            for (double x : curve.x()) {
                if (x <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double xMinOf(List<Series> series) {
        double minimum = Double.MAX_VALUE;
        for (Series curve : series) {
            for (double x : curve.x()) {
                minimum = Math.min(minimum, x);
            }
        }
        return minimum;
    }

    private static Shape triangle() {
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo(0.0, -4.5);
        shape.lineTo(4.5, 4.0);
        shape.lineTo(-4.5, 4.0);
        shape.closePath();
        return shape;
    }

    /** Renders {@code 10²}, {@code 10³}, ... instead of the default {@code 10^2} tick labels. */
    private static final class SuperscriptNumberFormat extends NumberFormat {

        private static final char[] SUPERSCRIPT = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition position) {
            int exponent = (int) Math.round(Math.log10(number));
            toAppendTo.append("10");
            if (exponent < 0) {
                toAppendTo.append('⁻');
            }
            for (char digit : Integer.toString(Math.abs(exponent)).toCharArray()) {
                toAppendTo.append(SUPERSCRIPT[digit - '0']);
            }
            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition position) {
            return format((double) number, toAppendTo, position);
        }

        @Override
        public Number parse(String source, ParsePosition position) {
            throw new UnsupportedOperationException("tick labels are only rendered, never parsed");
        }
    }
}
