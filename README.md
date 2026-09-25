# Warehouse Dispatch Scheduler

A Java simulation of a warehouse **sortation and dispatch** operation — the same kind of
process I run every shift as a Sortation Associate / Process Guide in Amazon's DDU
Dispatch operation, modeled as software.

Inbound packages arrive on a belt, get routed to a dispatch lane for their destination
zone, and have to be loaded onto that lane's truck before its cutoff time and within its
weight capacity. Anything that doesn't fit or arrives too late becomes an exception that
has to be re-routed or escalated — exactly the calls a Process Guide makes on the floor.

## Why I built this

I wanted a project that wasn't a generic tutorial app — something that models a process I
actually understand from three years of doing it in person, using the software skills
from my CS degree. The logic here (zone routing, capacity checks, cutoff enforcement,
priority ordering, on-time-rate reporting) mirrors the real constraints of a dispatch
shift.

## How it works

- **`Package`** — an inbound package: id, destination zone, weight, priority, arrival time.
- **`DispatchLane`** — one truck loading position dedicated to a zone, with a weight
  capacity and a cutoff time. Refuses to load a package that doesn't fit or that arrived
  after cutoff.
- **`SortationEngine`** — routes each package to the lane for its zone, loading
  highest-priority packages first so time-critical freight isn't bumped by capacity.
  Packages that miss cutoff, overflow a full lane, or have no configured lane at all are
  collected as exceptions.
- **`DispatchResult`** — the shift's outcome: which lanes shipped what, the on-time
  dispatch rate, and average lane utilization — the numbers a shift report would show.
- **`PackageCsvLoader`** — reads a shift's packages from a CSV file.
- **`Main`** — CLI entry point; prints an end-of-shift report to the console.

## Running it

Requires Java 21 and Maven.

```bash
mvn package
java -jar target/dispatch-scheduler.jar                      # runs the bundled sample shift
java -jar target/dispatch-scheduler.jar sample-data/packages.csv   # or point it at your own CSV
```

CSV format:

```
id,destinationZone,weightLbs,priority,arrivalTime
PKG001,NJ-08817,12.5,1,11:05
```

Sample output:

```
=== End-of-Shift Dispatch Report ===

Lane NJ-08817   truck TRK-101    2 pkgs     20.5 /  2000.0 lbs  (  1.0% full)  cutoff 14:00
Lane NJ-07001   truck TRK-102    1 pkgs     20.0 /  1500.0 lbs  (  1.3% full)  cutoff 14:00
Lane NY-10001   truck TRK-103    0 pkgs      0.0 /  1800.0 lbs  (  0.0% full)  cutoff 13:30

Packages dispatched on time : 3
Packages missed cutoff       : 1
Packages overflowed capacity : 2
On-time dispatch rate        : 50.0%
Average lane utilization     : 0.8%
```

## Testing

```bash
mvn test
```

Unit tests (JUnit 5) cover lane capacity/cutoff enforcement, priority-based load
ordering, overflow/unrouted handling, and CSV parsing edge cases. Tests run
automatically on every push via GitHub Actions (see `.github/workflows/ci.yml`).

## Tech

Java 21 · Maven · JUnit 5
