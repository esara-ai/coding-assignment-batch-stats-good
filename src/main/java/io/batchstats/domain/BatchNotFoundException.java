package io.batchstats.domain;

import java.util.UUID;

public class BatchNotFoundException extends RuntimeException {

    private final UUID batchId;

    public BatchNotFoundException(UUID batchId) {
        super("Batch " + batchId + " not found");
        this.batchId = batchId;
    }

    public UUID batchId() {
        return batchId;
    }
}
