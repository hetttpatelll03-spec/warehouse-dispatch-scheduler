package com.hetpatel.dispatch;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SortationEngineTest {

    private final LocalTime cutoff = LocalTime.of(14, 0);

    @Test
    void routesEachPackageToItsZoneLane() {
        DispatchLane njLane = new DispatchLane("NJ-08817", "TRK-101", 1000, cutoff);
        DispatchLane nyLane = new DispatchLane("NY-10001", "TRK-102", 1000, cutoff);
        SortationEngine engine = new SortationEngine(List.of(njLane, nyLane));

        List<Package> packages = List.of(
                new Package("PKG1", "NJ-08817", 10, 1, LocalTime.of(10, 0)),
                new Package("PKG2", "NY-10001", 10, 1, LocalTime.of(10, 0))
        );

        DispatchResult result = engine.run(packages);

        assertEquals(2, result.totalDispatched());
        assertEquals(1, njLane.loadedPackages().size());
        assertEquals(1, nyLane.loadedPackages().size());
    }

    @Test
    void higherPriorityPackagesAreLoadedBeforeCapacityRunsOut() {
        // Lane can only hold one 60lb package.
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 60, cutoff);
        SortationEngine engine = new SortationEngine(List.of(lane));

        Package lowPriority = new Package("LOW", "NJ-08817", 60, 1, LocalTime.of(9, 0));
        Package highPriority = new Package("HIGH", "NJ-08817", 60, 5, LocalTime.of(10, 0));

        DispatchResult result = engine.run(List.of(lowPriority, highPriority));

        assertEquals(1, result.totalDispatched());
        assertTrue(lane.loadedPackages().contains(highPriority));
        assertTrue(result.overflow().contains(lowPriority));
    }

    @Test
    void packagesArrivingAfterCutoffAreReportedAsMissed() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 1000, cutoff);
        SortationEngine engine = new SortationEngine(List.of(lane));

        Package late = new Package("LATE", "NJ-08817", 10, 1, LocalTime.of(14, 30));

        DispatchResult result = engine.run(List.of(late));

        assertEquals(0, result.totalDispatched());
        assertEquals(1, result.missedCutoff().size());
        assertTrue(result.missedCutoff().contains(late));
    }

    @Test
    void packagesWithNoMatchingLaneAreReportedAsOverflow() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 1000, cutoff);
        SortationEngine engine = new SortationEngine(List.of(lane));

        Package unrouted = new Package("UNROUTED", "PA-19019", 10, 1, LocalTime.of(10, 0));

        DispatchResult result = engine.run(List.of(unrouted));

        assertEquals(1, result.overflow().size());
        assertTrue(result.overflow().contains(unrouted));
    }

    @Test
    void onTimeDispatchRateAccountsForAllOutcomes() {
        DispatchLane lane = new DispatchLane("NJ-08817", "TRK-101", 60, cutoff);
        SortationEngine engine = new SortationEngine(List.of(lane));

        List<Package> packages = List.of(
                new Package("FITS", "NJ-08817", 60, 5, LocalTime.of(10, 0)),
                new Package("OVERFLOWS", "NJ-08817", 10, 1, LocalTime.of(10, 0)),
                new Package("TOO_LATE", "NJ-08817", 5, 5, LocalTime.of(15, 0))
        );

        DispatchResult result = engine.run(packages);

        assertEquals(1, result.totalDispatched());
        assertEquals(2, result.totalMissed());
        assertEquals(3, result.totalProcessed());
        assertEquals(1.0 / 3.0, result.onTimeDispatchRate(), 1e-9);
    }

    @Test
    void constructorRejectsDuplicateLanesForTheSameZone() {
        DispatchLane a = new DispatchLane("NJ-08817", "TRK-101", 100, cutoff);
        DispatchLane b = new DispatchLane("NJ-08817", "TRK-102", 100, cutoff);

        assertThrows(IllegalArgumentException.class, () -> new SortationEngine(List.of(a, b)));
    }
}
