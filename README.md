# batch-stats

A small REST service that keeps running statistics for batches of numbers.

## Run

Needs JDK 21.

```bash
./gradlew bootRun      # listens on :8080
./gradlew test
```

## API

| Method | Path                    | Body                     | Success |
|--------|-------------------------|--------------------------|---------|
| POST   | `/batches`              | `{"numbers":[...]}` (optional) | `201` + `Location` |
| POST   | `/batches/{id}/numbers` | `{"numbers":[...]}`      | `200`   |
| GET    | `/batches/{id}`         | –                        | `200`   |

Every success response looks like this:

```json
{
  "batchId": "0d6c6f0e-3f0b-4a59-9d1e-4b8f0e5f7c21",
  "stats": { "count": 4, "min": 1.0, "max": 4.0, "mean": 2.5, "median": 2.5, "variance": 1.6666666666666667, "stddev": 1.2909944487358056 }
}
```

Errors use [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details (`application/problem+json`):

- `400`: malformed JSON, missing `numbers`, a `null` or non-finite element, a malformed id, or more than `batch-stats.max-numbers-per-request` values (10 000 by default).
- `404`: unknown batch.

A rejected request never changes the batch. All values are checked before any is added.

## Decisions

- **Mean and variance**: Welford's online algorithm. It is O(1) per value and doesn't lose precision the way `sum(x²) - n·mean²` does when values share a large offset.
- **Median**: two heaps, O(log n) per insert. An exact median needs every value, so memory is O(n) per batch. If an approximation were acceptable, I'd use a t-digest instead.
- **Variance is the sample variance** (`n - 1`). It and `stddev` are `null` when `count < 2`; `min`/`max`/`mean`/`median` are `null` for an empty batch. I chose `null` over `0` because `0` is a valid result.
- **Concurrency**: the registry is a `ConcurrentHashMap`, and each batch is locked on its own, so appends to different batches never block each other.
- **Storage** is in memory, as the assignment allows. Batches are never evicted. That's the first thing I'd fix before running this for real (a TTL, or a store like Redis).

## Not done

- Persistence and eviction (see above).
- Auth and rate limiting.
- Metrics; Actuator would be the next step.
