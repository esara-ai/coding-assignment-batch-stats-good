package io.batchstats.api;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("batch-stats")
public record BatchLimits(@Positive int maxNumbersPerRequest) {}
