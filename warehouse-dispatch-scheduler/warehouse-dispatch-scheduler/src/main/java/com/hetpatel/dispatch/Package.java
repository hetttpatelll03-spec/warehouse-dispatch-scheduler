package com.hetpatel.dispatch;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A single inbound package moving through the sortation process.
 *
 * @param id            unique tracking id
 * @param destinationZone the delivery zone this package is bound for (e.g. "NJ-08817")
 * @param weightLbs     package weight in pounds, used for load-capacity checks
 * @param priority      dispatch priority; higher values are loaded first when a lane is tight
 * @param arrivalTime   the time the package arrived at the sortation belt
 */
public record Package(String id, String destinationZone, double weightLbs, int priority, LocalTime arrivalTime) {

    public Package {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(destinationZone, "destinationZone must not be null");
        Objects.requireNonNull(arrivalTime, "arrivalTime must not be null");
        if (weightLbs <= 0) {
            throw new IllegalArgumentException("weightLbs must be positive, got " + weightLbs);
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative, got " + priority);
        }
    }
}
