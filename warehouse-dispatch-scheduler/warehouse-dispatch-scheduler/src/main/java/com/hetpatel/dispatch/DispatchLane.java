package com.hetpatel.dispatch;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A dispatch lane represents one outbound truck loading position, dedicated to a
 * single destination zone for the shift. Packages are staged here until the lane's
 * cutoff time, at which point the truck must physically leave whether or not the
 * lane is full.
 */
public final class DispatchLane {

    private final String zone;
    private final String truckId;
    private final double capacityLbs;
    private final LocalTime cutoffTime;
    private final List<Package> loaded = new ArrayList<>();
    private double loadedWeightLbs = 0.0;

    public DispatchLane(String zone, String truckId, double capacityLbs, LocalTime cutoffTime) {
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
        this.truckId = Objects.requireNonNull(truckId, "truckId must not be null");
        this.cutoffTime = Objects.requireNonNull(cutoffTime, "cutoffTime must not be null");
        if (capacityLbs <= 0) {
            throw new IllegalArgumentException("capacityLbs must be positive, got " + capacityLbs);
        }
        this.capacityLbs = capacityLbs;
    }

    public String zone() {
        return zone;
    }

    public String truckId() {
        return truckId;
    }

    public double capacityLbs() {
        return capacityLbs;
    }

    public LocalTime cutoffTime() {
        return cutoffTime;
    }

    public double loadedWeightLbs() {
        return loadedWeightLbs;
    }

    public double remainingCapacityLbs() {
        return capacityLbs - loadedWeightLbs;
    }

    public double utilization() {
        return loadedWeightLbs / capacityLbs;
    }

    public List<Package> loadedPackages() {
        return Collections.unmodifiableList(loaded);
    }

    /**
     * Attempts to load a package onto this lane.
     *
     * @return true if the package fit and was loaded, false if it would exceed capacity
     * @throws IllegalStateException if the package arrived after this lane's cutoff time
     * @throws IllegalArgumentException if the package is not addressed to this lane's zone
     */
    public boolean load(Package pkg) {
        if (!pkg.destinationZone().equals(zone)) {
            throw new IllegalArgumentException(
                    "Package " + pkg.id() + " is addressed to zone " + pkg.destinationZone()
                            + " but this lane serves " + zone);
        }
        if (pkg.arrivalTime().isAfter(cutoffTime)) {
            throw new IllegalStateException(
                    "Package " + pkg.id() + " arrived at " + pkg.arrivalTime()
                            + ", after lane cutoff " + cutoffTime);
        }
        if (pkg.weightLbs() > remainingCapacityLbs()) {
            return false;
        }
        loaded.add(pkg);
        loadedWeightLbs += pkg.weightLbs();
        return true;
    }
}
