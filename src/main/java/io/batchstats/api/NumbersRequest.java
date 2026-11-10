package io.batchstats.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NumbersRequest(@NotNull List<@NotNull Double> numbers) {}
