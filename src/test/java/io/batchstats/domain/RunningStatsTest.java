package io.batchstats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RunningStatsTest {

    private static final double EPSILON = 1e-9;

    @Test
    void emptyBatchHasOnlyACount() {
        assertThat(new RunningStats().snapshot()).isEqualTo(new StatsSnapshot(0, null, null, null, null, null, null));
    }

    @Test
    void singleValueHasNoDispersion() {
        RunningStats stats = new RunningStats();
        stats.add(-7.5);

        StatsSnapshot snapshot = stats.snapshot();

        assertThat(snapshot.min()).isEqualTo(-7.5);
        assertThat(snapshot.max()).isEqualTo(-7.5);
        assertThat(snapshot.median()).isEqualTo(-7.5);
        assertThat(snapshot.variance()).isNull();
        assertThat(snapshot.stddev()).isNull();
    }

    @Test
    void computesKnownValues() {
        RunningStats stats = new RunningStats();
        stats.addAll(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));

        StatsSnapshot snapshot = stats.snapshot();

        assertThat(snapshot.count()).isEqualTo(8);
        assertThat(snapshot.min()).isEqualTo(2.0);
        assertThat(snapshot.max()).isEqualTo(9.0);
        assertThat(snapshot.mean()).isEqualTo(5.0);
        assertThat(snapshot.median()).isEqualTo(4.5);
        assertThat(snapshot.variance()).isCloseTo(32.0 / 7, within(EPSILON));
        assertThat(snapshot.stddev()).isCloseTo(Math.sqrt(32.0 / 7), within(EPSILON));
    }

    @Test
    void allNegativeValuesKeepCorrectMax() {
        RunningStats stats = new RunningStats();
        stats.addAll(List.of(-3.0, -1.0, -2.0));

        assertThat(stats.snapshot().max()).isEqualTo(-1.0);
        assertThat(stats.snapshot().min()).isEqualTo(-3.0);
    }

    @Test
    void medianMatchesSortingForRandomInput() {
        Random random = new Random(42);
        RunningStats stats = new RunningStats();
        double[] values = new double[1001];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextGaussian() * 100;
            stats.add(values[i]);
            double[] sorted = Arrays.copyOf(values, i + 1);
            Arrays.sort(sorted);
            int n = sorted.length;
            double expected = n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
            assertThat(stats.snapshot().median()).isCloseTo(expected, within(EPSILON));
        }
    }

    @Test
    void varianceStaysAccurateWithLargeOffset() {
        double offset = 1e9;
        RunningStats stats = new RunningStats();
        stats.addAll(List.of(offset + 4, offset + 7, offset + 13, offset + 16));

        assertThat(stats.snapshot().variance()).isCloseTo(30.0, within(EPSILON));
    }

    @Test
    void medianOfHugeValuesDoesNotOverflow() {
        RunningStats stats = new RunningStats();
        stats.addAll(List.of(Double.MAX_VALUE, Double.MAX_VALUE));

        assertThat(stats.snapshot().median()).isEqualTo(Double.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteValues(double value) {
        RunningStats stats = new RunningStats();

        assertThatThrownBy(() -> stats.add(value)).isInstanceOf(IllegalArgumentException.class);
        assertThat(stats.snapshot().count()).isZero();
    }
}
