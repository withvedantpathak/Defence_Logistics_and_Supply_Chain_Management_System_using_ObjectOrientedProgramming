# DLSCMS Design (P1) — binding for P3

## 1. Packages (`com.dlscms.*`)
| Package | Contents |
|---|---|
| `model` | `Base`, `Location`(record), `Supply`, `SupplyCategory`(enum), `InventoryItem`, `Inventory`, `SupplyRequest`, `Priority`(enum), `RequestStatus`(enum), `Delivery`, `DeliveryStatus`(enum), interfaces `Trackable`, `Deliverable`, `Persistable` |
| `model.vehicle` | `abstract Vehicle`, `Truck`, `FuelTruck`, `MedicalVan`, `Helicopter`, `VehicleStatus`(enum) |
| `service` | `SimClock`, `InventoryService`, `FleetService`, `RequestService`, `DeliveryService`, `ReportService`, `LogisticsManager` (facade), `Registry<T>` (generic id→entity store) |
| `strategy` | `SourceSelectionStrategy` + `NearestFirst`, `MostSurplus`, `Balanced`; `VehicleAssignmentStrategy` + `SmallestFit`, `FastestFit`; `SourceCandidate`(record) |
| `exception` | `LogisticsException`(checked) → `InvalidIdException`, `InsufficientStockException`, `NoVehicleAvailableException`, `InvalidStateTransitionException`, `ValidationException`, `DataFormatException` |
| `persistence` | `CsvRepository<T extends Persistable>`, `DataStore` (loads/saves a whole world to a folder) |
| `ui` | `Terminal` (ANSI/plain output), `CommandParser`, `Command`(record), `Screens` (renderers returning String), `TerminalApp` (REPL loop) |
| `util` | `Distance` (haversine), `IdGenerator`, `Validator`, `Table` (text tables) |
| `sim` | `Scenario`, `ScenarioRequest`, `Simulator`, `SimResult`, `Experiment`, `RandomSource`/`RandomVehicle` (baselines) — see docs/findings.md |

## 2. Entities
- **Base**: `id` (3–4 letter code, e.g. `PUN`), `name`, `Location(city, lat, lon)`, `storageCapacityUnits`.
- **Supply**: `id` (e.g. `MED-KIT`), `name`, `SupplyCategory` {MEDICAL, FOOD, FUEL, SPARES, SHELTER}, `unitWeightKg`. Non-weapon categories only.
- **InventoryItem** (per base × supply): `qty`, `reserved`, `reorderLevel`. `available() = qty − reserved`; `surplus() = max(0, available − reorderLevel)`; `shortage() = max(0, reorderLevel − available)`.
- **Vehicle** (abstract): `id`, `homeBaseId`, `currentBaseId`, `capacityKg`, `speedKmph`, `VehicleStatus` {AVAILABLE, ASSIGNED, IN_TRANSIT, MAINTENANCE}.
  Abstract/overridden behaviour: `typeName()`, `canCarry(SupplyCategory)`, `capacityFor(SupplyCategory)` (kg), `effectiveSpeedKmph(SupplyCategory)`, `estimateHours(km, cat)` (default `km / effectiveSpeed + loadingHours()`), `loadingHours()`.
  - `Truck`: carries all non-fuel categories, full capacity. Speed ≈ 45 km/h road.
  - `FuelTruck`: carries FUEL only; capacity = rated; slower loading (1.5 h).
  - `MedicalVan`: MEDICAL + FOOD; 60 km/h; 0.5 h loading.
  - `Helicopter`: any category except FUEL; capacity 1/6 of truck; 180 km/h, ignores road factor; 0.25 h loading.
- **SupplyRequest**: `id`, `requesterBaseId`, `supplyId`, `qty`, `fulfilledQty`, `Priority` {ROUTINE, URGENT, CRITICAL}, `RequestStatus`, `createdAtHour`. `Comparable`: priority desc, then createdAt asc, then id.
- **Delivery**: `id`, `requestId`, `sourceBaseId`, `destBaseId`, `supplyId`, `qty`, `vehicleId`, `DeliveryStatus`, `plannedAtHour`, `dispatchedAtHour`, `etaHour`, `deliveredAtHour`, `distanceKm`, `failureReason`. Implements `Deliverable` + `Trackable`.

## 3. Interfaces (O6, O10)
- `Trackable { String id(); String statusLabel(); String locationLabel(); }` — Vehicle and Delivery.
- `Deliverable { void dispatch(long hour) throws InvalidStateTransitionException; void complete(long hour) throws ...; void fail(String reason, long hour) throws ...; }` — Delivery.
- `Persistable { String csvHeader(); String toCsvRow(); }` — all saved entities. `CsvRepository<T extends Persistable>` takes a `Function<String[],T>` row parser (generics).
- Generic helpers: `Registry<T extends Trackable>`-style id stores; `static <T extends Trackable> List<T> withStatus(Collection<T>, String)`.

## 4. Business rules (MVP)
1. **Time** is simulated in whole hours (`SimClock`). `TICK n` advances the clock and auto-completes deliveries whose ETA ≤ now.
2. **Donor floor**: a base may only give away its `surplus()` (stock above reorder level). For CRITICAL requests the floor is halved (`reorderLevel / 2`).
3. **Request lifecycle**: `NEW → APPROVED → FULFILLING → FULFILLED`, or `REJECTED` / `CANCELLED` (only from NEW/APPROVED). Fulfil can be called repeatedly on an APPROVED/FULFILLING request until `fulfilledQty == qty`.
4. **fulfil(requestId)**: loop while remaining > 0: (a) ask `SourceSelectionStrategy` to pick a donor from candidates with surplus > 0 (excluding requester); (b) ask `VehicleAssignmentStrategy` to pick an AVAILABLE vehicle at/near that donor that `canCarry` the category; (c) trip qty = min(remaining, donor surplus, vehicle capacity in units); (d) create `Delivery(PLANNED)`, reserve stock at donor, mark vehicle ASSIGNED. If nothing could be planned in a pass → stop; throw `InsufficientStockException` if *no* delivery was planned at all, `NoVehicleAvailableException` if stock existed but no vehicle did. A request with some deliveries planned goes `FULFILLING`.
5. **Delivery lifecycle**: `PLANNED → IN_TRANSIT → DELIVERED`, or `FAILED` from PLANNED/IN_TRANSIT. `dispatch`: source `qty−=`, `reserved−=`, vehicle IN_TRANSIT, ETA = now + `vehicle.estimateHours(km, category)`. `complete`: destination `qty+=`, vehicle AVAILABLE at destination (`currentBaseId`), request `fulfilledQty+=`; request becomes FULFILLED when complete. `fail`: if PLANNED release the reservation; if IN_TRANSIT return stock to source; vehicle AVAILABLE at source; request goes back to APPROVED.
6. **Vehicle location**: a vehicle serves from `currentBaseId`; distance for ETA = haversine(source→dest) plus empty positioning leg (vehicle current → source) if different. Road factor 1.25× for ground vehicles.
7. **Conservation invariant** (tested in P4): total stock (sum of qty + in-transit qty) is constant under planning, dispatch, complete, fail.
8. **IDs**: requests `R-0001`, deliveries `D-0001` via `IdGenerator`; base/supply/vehicle ids come from data files.

## 5. Strategies (O4 polymorphism; basis of P5)
- `SourceSelectionStrategy.select(SupplyRequest, List<SourceCandidate>) → Optional<SourceCandidate>`; `SourceCandidate(Base base, int surplus, double distanceKm)`.
  - `NearestFirst`: min distance.
  - `MostSurplus`: max surplus (tie → nearer).
  - `Balanced`: score = 0.5·(1 − dist/maxDist) + 0.5·(surplus/maxSurplus).
- `VehicleAssignmentStrategy.select(List<Vehicle> eligible, SupplyCategory, int qty, double unitWeightKg, double distanceKm) → Optional<Vehicle>`.
  - `SmallestFit`: smallest capacity that carries the whole trip qty, else largest available.
  - `FastestFit`: minimum `estimateHours`.
- Strategies are injected into `LogisticsManager` via constructor (dependency injection) and swappable at runtime (`STRAT`).

## 6. Persistence (O9)
Folder of CSVs: `bases.csv, supplies.csv, inventory.csv, vehicles.csv, requests.csv, deliveries.csv, meta.csv` (clock hour, id counters, active strategies). `SAVE`/`LOAD` in terminal; auto-load of `data/` when present. Malformed rows → `DataFormatException` naming file + line.

## 7. OOP concept → code map (summary; full matrix in requirements.md)
O1 all entity classes · O2 private fields, validated constructors/setters, immutable `Location`/records · O3 `Truck/FuelTruck/MedicalVan/Helicopter extends Vehicle` · O4 `Vehicle` overrides + strategy interfaces · O5 `abstract Vehicle`, `SourceSelectionStrategy` · O6 `Trackable/Deliverable/Persistable` · O7 `Map`, `List`, `PriorityQueue<SupplyRequest>`, `Comparator`, `EnumMap` · O8 exception hierarchy · O9 `CsvRepository`, `DataStore` · O10 `CsvRepository<T>`, `Registry<T>`, bounded generic methods · O11 package layout above.

## 8. Patterns
Strategy (source/vehicle selection), Facade (`LogisticsManager`), Dependency Injection (constructor), Repository (`CsvRepository`), State-machine enums for lifecycles, Template Method (`Vehicle.estimateHours` uses overridable hooks).
