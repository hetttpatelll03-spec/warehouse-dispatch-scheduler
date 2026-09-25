package com.hetpatel.dispatch;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Command-line entry point. Loads a shift's worth of packages and a lane configuration
 * (or falls back to a bundled sample shift), runs the sort, and prints a dispatch report
 * to stdout similar to an end-of-shift summary.
 *
 * <p>Usage: {@code java -jar dispatch-scheduler.jar [path/to/packages.csv]}
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        List<Package> packages;
        if (args.length >= 1) {
            packages = PackageCsvLoader.load(Path.of(args[0]));
        } else {
            System.out.println("No CSV given, running the bundled sample shift (see sample-data/packages.csv).\n");
            packages = SampleData.samplePackages();
        }

        List<DispatchLane> lanes = SampleData.sampleLanes();
        SortationEngine engine = new SortationEngine(lanes);
        DispatchResult result = engine.run(packages);

        printReport(result);
    }

    private static void printReport(DispatchResult result) {
        System.out.println("=== End-of-Shift Dispatch Report ===\n");

        for (DispatchLane lane : result.lanes()) {
            System.out.printf(Locale.US,
                    "Lane %-10s truck %-6s  %3d pkgs  %7.1f / %7.1f lbs  (%5.1f%% full)  cutoff %s%n",
                    lane.zone(), lane.truckId(), lane.loadedPackages().size(),
                    lane.loadedWeightLbs(), lane.capacityLbs(), lane.utilization() * 100, lane.cutoffTime());
        }

        System.out.println();
        System.out.printf(Locale.US, "Packages dispatched on time : %d%n", result.totalDispatched());
        System.out.printf(Locale.US, "Packages missed cutoff       : %d%n", result.missedCutoff().size());
        System.out.printf(Locale.US, "Packages overflowed capacity : %d%n", result.overflow().size());
        System.out.printf(Locale.US, "On-time dispatch rate        : %.1f%%%n", result.onTimeDispatchRate() * 100);
        System.out.printf(Locale.US, "Average lane utilization     : %.1f%%%n", result.averageLaneUtilization() * 100);

        if (!result.missedCutoff().isEmpty()) {
            System.out.println("\nMissed cutoff (arrived too late for their lane):");
            for (Package pkg : result.missedCutoff()) {
                System.out.printf(Locale.US, "  %-8s zone %-10s arrived %s%n", pkg.id(), pkg.destinationZone(), pkg.arrivalTime());
            }
        }

        if (!result.overflow().isEmpty()) {
            System.out.println("\nOverflow / unrouted (needs re-lane or escalation):");
            for (Package pkg : result.overflow()) {
                System.out.printf(Locale.US, "  %-8s zone %-10s %.1f lbs%n", pkg.id(), pkg.destinationZone(), pkg.weightLbs());
            }
        }
    }

    /** Small bundled dataset so the jar runs out of the box with no arguments. */
    private static final class SampleData {
        static List<DispatchLane> sampleLanes() {
            return List.of(
                    new DispatchLane("NJ-08817", "TRK-101", 2000, LocalTime.of(14, 0)),
                    new DispatchLane("NJ-07001", "TRK-102", 1500, LocalTime.of(14, 0)),
                    new DispatchLane("NY-10001", "TRK-103", 1800, LocalTime.of(13, 30))
            );
        }

        static List<Package> samplePackages() {
            return List.of(
                    new Package("PKG001", "NJ-08817", 12.5, 1, LocalTime.of(11, 5)),
                    new Package("PKG002", "NJ-08817", 8.0, 2, LocalTime.of(11, 20)),
                    new Package("PKG003", "NJ-07001", 20.0, 1, LocalTime.of(12, 0)),
                    new Package("PKG004", "NY-10001", 15.0, 3, LocalTime.of(13, 45)), // after NY cutoff
                    new Package("PKG005", "NJ-08817", 1990.0, 1, LocalTime.of(12, 30)), // will overflow the lane
                    new Package("PKG006", "PA-19019", 5.0, 1, LocalTime.of(10, 0)) // no lane configured
                );
        }
    }
}
