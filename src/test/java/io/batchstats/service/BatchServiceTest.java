package io.batchstats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.batchstats.domain.BatchNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class BatchServiceTest {

    private final BatchService service = new BatchService();

    @Test
    void appendUpdatesExistingBatch() {
        UUID id = service.create(List.of(1.0)).batchId();

        service.append(id, List.of(3.0));

        assertThat(service.get(id).stats().mean()).isEqualTo(2.0);
    }

    @Test
    void unknownBatchIsReported() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(BatchNotFoundException.class);
        assertThatThrownBy(() -> service.append(id, List.of(1.0))).isInstanceOf(BatchNotFoundException.class);
    }

    @Test
    void concurrentAppendsAreNotLost() throws Exception {
        int threads = 8;
        int appendsPerThread = 1_000;
        UUID id = service.create(List.of()).batchId();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < appendsPerThread; i++) {
                        service.append(id, List.of(1.0));
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(service.get(id).stats().count()).isEqualTo((long) threads * appendsPerThread);
        assertThat(service.get(id).stats().variance()).isZero();
    }
}
