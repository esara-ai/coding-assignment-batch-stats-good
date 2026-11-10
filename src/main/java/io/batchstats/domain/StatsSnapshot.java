package io.batchstats.domain;

/**
 * Point-in-time view of a batch.
 *
 * <p>Fields that are undefined for the current sample size are {@code null} rather than 0 or NaN,
 * so clients can't mistake "no data" for a real value.
 */
public record StatsSnapshot(
        long count,
        Double min,
        Double max,
        Double mean,
        Double median,
        Double variance,
        Double stddev) {

    static StatsSnapshot empty() {
        return new StatsSnapshot(0, null, null, null, null, null, null);
    }
}
