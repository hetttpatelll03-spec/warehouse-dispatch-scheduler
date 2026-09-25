package com.hetpatel.dispatch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackageCsvLoaderTest {

    @Test
    void parsesWellFormedCsv() throws IOException {
        String csv = """
                id,destinationZone,weightLbs,priority,arrivalTime
                PKG001,NJ-08817,12.5,1,11:05
                PKG002,NY-10001,8.0,2,11:20
                """;

        List<Package> packages = PackageCsvLoader.load(new StringReader(csv));

        assertEquals(2, packages.size());
        assertEquals(new Package("PKG001", "NJ-08817", 12.5, 1, LocalTime.of(11, 5)), packages.get(0));
        assertEquals(new Package("PKG002", "NY-10001", 8.0, 2, LocalTime.of(11, 20)), packages.get(1));
    }

    @Test
    void skipsBlankLines() throws IOException {
        String csv = """
                id,destinationZone,weightLbs,priority,arrivalTime
                PKG001,NJ-08817,12.5,1,11:05

                PKG002,NY-10001,8.0,2,11:20
                """;

        List<Package> packages = PackageCsvLoader.load(new StringReader(csv));

        assertEquals(2, packages.size());
    }

    @Test
    void throwsIOExceptionWithLineNumberOnMalformedRow() {
        String csv = """
                id,destinationZone,weightLbs,priority,arrivalTime
                PKG001,NJ-08817,not-a-number,1,11:05
                """;

        IOException ex = assertThrows(IOException.class, () -> PackageCsvLoader.load(new StringReader(csv)));
        assertTrue(ex.getMessage().contains("line 2"));
    }
}
