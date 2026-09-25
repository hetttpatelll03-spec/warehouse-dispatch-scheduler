package com.hetpatel.dispatch;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DispatchLaneTest {

    private final LocalTime cutoff = LocalTime.of(14, 0);

    @Test
    void loadsPackageWithinCapacity() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 100, cutoff);
        Package pkg = new Package("PKG1", "NJ-08817", 40, 1, LocalTime.of(10, 0));

        assertTrue(lane.load(pkg));
        assertEquals(40, lane.loadedWeightLbs());
        assertEquals(60, lane.remainingCapacityLbs());
        assertEquals(1, lane.loadedPackages().size());
    }

    @Test
    void rejectsPackageThatWouldExceedCapacity() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 100, cutoff);
        Package pkg = new Package("PKG1", "NJ-08817", 150, 1, LocalTime.of(10, 0));

        assertFalse(lane.load(pkg));
        assertEquals(0, lane.loadedWeightLbs());
        assertTrue(lane.loadedPackages().isEmpty());
    }

    @Test
    void throwsWhenPackageArrivesAfterCutoff() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 100, cutoff);
        Package pkg = new Package("PKG1", "NJ-08817", 10, 1, LocalTime.of(14, 1));

        assertThrows(IllegalStateException.class, () -> lane.load(pkg));
    }

    @Test
    void throwsWhenPackageIsForADifferentZone() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 100, cutoff);
        Package pkg = new Package("PKG1", "NY-10001", 10, 1, LocalTime.of(10, 0));

        assertThrows(IllegalArgumentException.class, () -> lane.load(pkg));
    }

    @Test
    void utilizationReflectsLoadedFractionOfCapacity() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 200, cutoff);
        lane.load(new Package("PKG1", "NJ-08817", 50, 1, LocalTime.of(10, 0)));

        assertEquals(0.25, lane.utilization(), 1e-9);
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DispatchLane("NJ-08817", "TRK-101", 0, cutoff));
    }
}
