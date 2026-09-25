package com.hetpatel.dispatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sorts a batch of inbound packages onto the dispatch lane for their destination zone.
 *
 * <p>Mirrors how a sortation belt actually behaves during a shift: packages are routed
 * to the lane matching their zone, loaded highest-priority-first so time-critical
 * freight doesn't get bumped by capacity, and anything that either arrives after the
 * lane's cutoff or can't fit once the lane is full is pulled out as an exception that
 * a Process Guide would have to re-route or escalate.
 */
public final class SortationEngine {

    private final List<DispatchLane> lanes;

    public SortationEngine(List<DispatchLane> lanes) {
        this.lanes = List.copyOf(lanes);
        Map<String, Long> zonesPerLane = this.lanes.stream()
                .collect(Collectors.groupingBy(DispatchLane::zone, Collectors.counting()));
        zonesPerLane.forEach((zone, count) -> {
            if (count > 1) {
                throw new IllegalArgumentException("Multiple lanes configured for zone " + zone
                        + "; each zone must map to exactly one lane");
            }
        });
    }

    /**
     * Runs the sort for one shift's worth of packages.
     *
     * <p>Packages are grouped by destination zone, then within each zone loaded in
     * order of highest priority first (ties broken by earliest arrival), so that if a
     * lane fills up, it's the lowest-priority packages that get bumped to overflow.
     */
    public DispatchResult run(List<Package> packages) {
        Map<String, DispatchLane> laneByZone = lanes.stream()
                .collect(Collectors.toMap(DispatchLane::zone, l -> l));

        List<Package> missedCutoff = new ArrayList<>();
        List<Package> overflow = new ArrayList<>();
        List<Package> unrouted = new ArrayList<>();

        Map<String, List<Package>> byZone = packages.stream()
                .collect(Collectors.groupingBy(Package::destinationZone));

        Comparator<Package> loadOrder = Comparator
                .comparingInt(Package::priority).reversed()
                .thenComparing(Package::arrivalTime);

        for (Map.Entry<String, List<Package>> entry : byZone.entrySet()) {
            DispatchLane lane = laneByZone.get(entry.getKey());
            List<Package> zonePackages = entry.getValue().stream().sorted(loadOrder).toList();

            if (lane == null) {
                unrouted.addAll(zonePackages);
                continue;
            }

            for (Package pkg : zonePackages) {
                if (pkg.arrivalTime().isAfter(lane.cutoffTime())) {
                    missedCutoff.add(pkg);
                    continue;
                }
                boolean loaded = lane.load(pkg);
                if (!loaded) {
                    overflow.add(pkg);
                }
            }
        }

        // Packages with no configured lane for their zone are a routing exception,
        // not a capacity one, but they still never made it onto a truck.
        overflow.addAll(unrouted);

        return new DispatchResult(lanes, missedCutoff, overflow);
    }
}
