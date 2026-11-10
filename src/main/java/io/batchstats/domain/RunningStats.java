package io.batchstats.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Incremental statistics for a stream of finite doubles.
 *
 * <p>Mean and variance use Welford's algorithm, which stays accurate when values share a large
 * offset (e.g. timestamps). The median uses two heaps, so each insert is O(log n) and reading it is
 * O(1). The heaps still hold every value: an exact median can't be computed in less than O(n) memory.
 *
 * <p>Not thread-safe. Callers must synchronise access.
 */
public final class RunningStats {

    // Max-heap holding the smaller half; may be one element larger than upper.
    private final PriorityQueue<Double> lower = new PriorityQueue<>(Collections.reverseOrder());
    private final PriorityQueue<Double> upper = new PriorityQueue<>();

    private long count;
    private double mean;
    private double m2;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public void addAll(Collection<Double> values) {
        for (Double value : values) {
            add(value);
        }
    }

    public void add(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite: " + value);
        }
        count++;
        double delta = value - mean;
        mean += delta / count;
        m2 += delta * (value - mean);
        min = Math.min(min, value);
        max = Math.max(max, value);
        addToMedianHeaps(value);
    }

    private void addToMedianHeaps(double value) {
        if (lower.isEmpty() || value <= lower.peek()) {
            lower.add(value);
        } else {
            upper.add(value);
        }
        if (lower.size() > upper.size() + 1) {
            upper.add(lower.poll());
        } else if (upper.size() > lower.size()) {
            lower.add(upper.poll());
        }
    }

    public StatsSnapshot snapshot() {
        if (count == 0) {
            return StatsSnapshot.empty();
        }
        // Sample variance (n - 1): a batch is treated as a sample, and it is undefined for one value.
        Double variance = count > 1 ? m2 / (count - 1) : null;
        Double stddev = variance != null ? Math.sqrt(variance) : null;
        return new StatsSnapshot(count, min, max, mean, median(), variance, stddev);
    }

    private double median() {
        if (lower.size() > upper.size()) {
            return lower.peek();
        }
        // Halve before adding so two values near Double.MAX_VALUE don't overflow.
        return lower.peek() / 2 + upper.peek() / 2;
    }
}
