package com.loyalty.service_engine.application.dto;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Result of fidelity classification for a client.
 * Supports 3-path logic:
 * 1. Exact match: client points fall within a range
 * 2. Fallthrough: client in gap, assign nearest lower level
 * 3. NONE: client does not qualify (points below minimum entry level)
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassificationResult {
    public static final ClassificationResult NONE = new ClassificationResult(null);

    private final FidelityRangeDTO range;

    /**
     * Factory method to create a classified result.
     * Returns NONE if range is null.
     */
    public static ClassificationResult of(FidelityRangeDTO range) {
        return range != null ? new ClassificationResult(range) : NONE;
    }

    /**
     * Check if client is classified to a level.
     */
    public boolean isClassified() {
        return range != null;
    }

    /**
     * Get the fidelity range if classified, or empty Optional if NONE.
     */
    public Optional<FidelityRangeDTO> asOptional() {
        return Optional.ofNullable(range);
    }

    @Override
    public String toString() {
        return isClassified() 
            ? "ClassificationResult(level=%s, points=%d-%d)".formatted(
                range.name(), range.minPoints(), range.maxPoints())
            : "ClassificationResult(NONE)";
    }
}
