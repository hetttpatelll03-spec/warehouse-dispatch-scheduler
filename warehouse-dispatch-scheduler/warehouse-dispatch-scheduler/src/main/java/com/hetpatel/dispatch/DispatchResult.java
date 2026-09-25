package com.hetpatel.dispatch;

import java.util.Collections;
import java.util.List;

/**
 * The outcome of running a {@link SortationEngine} simulation for a shift: which
 * packages made it onto a truck on time, and which missed their lane (either because
 * the lane was already full, or because the package arrived after cutoff).
 */
public record DispatchResult(List<DispatchLane> lanes, List<Package> missedCutoff, List<Package> overflow) {

    public DispatchResult {
        lanes = List.copyOf(lanes);
        missedCutoff = List.copyOf(missedCutoff);
        overflow = List.copyOf(overflow);
    }

    public int totalDispatched() {
        return lanes.stream().mapToInt(l -> l.loadedPackages().size()).sum();
    }

    public int totalMissed() {
        return missedCutoff.size() + overflow.size();
    }

    public int totalProcessed() {
        return totalDispatched() + totalMissed();
    }

    /** Fraction of processed packages that made it onto a truck, in [0, 1]. */
    public double onTimeDispatchRate() {
        int processed = totalProcessed();
        return processed == 0 ? 1.0 : (double) totalDispatched() / processed;
    }

    public double averageLaneUtilization() {
        if (lanes.isEmpty()) {
            return 0.0;
        }
        return lanes.stream().mapToDouble(DispatchLane::utilization).average().orElse(0.0);
    }

    public static List<Package> emptyList() {
        return Collections.emptyList();
    }
}
