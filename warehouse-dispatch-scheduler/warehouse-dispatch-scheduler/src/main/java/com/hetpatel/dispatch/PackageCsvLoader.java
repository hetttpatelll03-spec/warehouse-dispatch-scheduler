package com.hetpatel.dispatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads packages from a simple CSV format: {@code id,destinationZone,weightLbs,priority,arrivalTime}.
 * The first line is expected to be a header and is skipped.
 */
public final class PackageCsvLoader {

    private PackageCsvLoader() {
    }

    public static List<Package> load(Path csvFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            return load(reader);
        }
    }

    public static List<Package> load(Reader source) throws IOException {
        List<Package> packages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(source)) {
            String line = reader.readLine(); // header
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    packages.add(parseLine(line));
                } catch (RuntimeException e) {
                    throw new IOException("Failed to parse CSV line " + lineNumber + ": " + line, e);
                }
            }
        }
        return packages;
    }

    private static Package parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "Expected 5 columns (id,destinationZone,weightLbs,priority,arrivalTime), got " + parts.length);
        }
        String id = parts[0].trim();
        String zone = parts[1].trim();
        double weight = Double.parseDouble(parts[2].trim());
        int priority = Integer.parseInt(parts[3].trim());
        LocalTime arrival = LocalTime.parse(parts[4].trim());
        return new Package(id, zone, weight, priority, arrival);
    }
}
