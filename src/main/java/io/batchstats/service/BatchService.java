package io.batchstats.service;

import io.batchstats.domain.BatchNotFoundException;
import io.batchstats.domain.RunningStats;
import io.batchstats.domain.StatsSnapshot;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory batch registry. State is lost on restart, which the assignment allows.
 *
 * <p>Each batch is locked on its own, so appends to different batches never wait for each other.
 */
@Service
public class BatchService {

    private final Map<UUID, RunningStats> batches = new ConcurrentHashMap<>();

    public BatchSnapshot create(List<Double> initialNumbers) {
        RunningStats stats = new RunningStats();
        stats.addAll(initialNumbers);
        UUID id = UUID.randomUUID();
        batches.put(id, stats);
        return new BatchSnapshot(id, stats.snapshot());
    }

    public BatchSnapshot append(UUID id, List<Double> numbers) {
        RunningStats stats = find(id);
        synchronized (stats) {
            stats.addAll(numbers);
            return new BatchSnapshot(id, stats.snapshot());
        }
    }

    public BatchSnapshot get(UUID id) {
        RunningStats stats = find(id);
        synchronized (stats) {
            return new BatchSnapshot(id, stats.snapshot());
        }
    }

    private RunningStats find(UUID id) {
        RunningStats stats = batches.get(id);
        if (stats == null) {
            throw new BatchNotFoundException(id);
        }
        return stats;
    }

    public record BatchSnapshot(UUID batchId, StatsSnapshot stats) {}
}
