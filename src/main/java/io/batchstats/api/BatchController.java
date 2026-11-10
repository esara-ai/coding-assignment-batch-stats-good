package io.batchstats.api;

import io.batchstats.service.BatchService;
import io.batchstats.service.BatchService.BatchSnapshot;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;
    private final BatchLimits limits;

    public BatchController(BatchService batchService, BatchLimits limits) {
        this.batchService = batchService;
        this.limits = limits;
    }

    @PostMapping
    public ResponseEntity<BatchSnapshot> create(@Valid @RequestBody(required = false) NumbersRequest request) {
        List<Double> numbers = request == null ? List.of() : checked(request.numbers());
        BatchSnapshot created = batchService.create(numbers);
        return ResponseEntity.created(URI.create("/batches/" + created.batchId())).body(created);
    }

    @PostMapping("/{id}/numbers")
    public BatchSnapshot append(@PathVariable UUID id, @Valid @RequestBody NumbersRequest request) {
        return batchService.append(id, checked(request.numbers()));
    }

    @GetMapping("/{id}")
    public BatchSnapshot get(@PathVariable UUID id) {
        return batchService.get(id);
    }

    // Checked here, before any value is added, so a bad request never leaves a batch half-updated.
    private List<Double> checked(List<Double> numbers) {
        if (numbers.size() > limits.maxNumbersPerRequest()) {
            throw new InvalidNumbersException(
                    "at most " + limits.maxNumbersPerRequest() + " numbers are allowed per request");
        }
        if (!numbers.stream().allMatch(Double::isFinite)) {
            throw new InvalidNumbersException("numbers must be finite");
        }
        return numbers;
    }
}
