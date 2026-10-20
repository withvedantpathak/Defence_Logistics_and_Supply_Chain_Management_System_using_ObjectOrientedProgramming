# P1 — Requirements & design (≈2 h)
**Goal:** locked design that makes every brief item traceable. No code beyond stubs.
**Needs human:** none.

## Steps
- [x] 1.1 Fill `docs/requirements.md`: functional reqs F1–F6 (from "Main features"), OOP reqs O1–O11 (from "How it demonstrates OOP"), non-functional (offline, deterministic, <1s ops, CSV persistence).
- [x] 1.2 Design the domain (suggested; refine as needed):
  - `model`: `Base`, `Supply` (+ `SupplyCategory` enum: non-weapon categories only: MEDICAL, FOOD, FUEL, SPARES, SHELTER), `InventoryItem`, `SupplyRequest` (+ `Priority`, `RequestStatus` enums), `Delivery` (+ `DeliveryStatus` enum).
  - `model.vehicle`: `abstract Vehicle implements Trackable` → `Truck`, `FuelTruck`, `MedicalVan`, optionally `Helicopter` (different speed/capacity/`capacityFor(SupplyCategory)` overrides = polymorphism).
  - Interfaces: `Trackable` (id, location/status), `Deliverable` (`dispatch()`, `complete()`), `Persistable<T>` (CSV round-trip, generics).
  - `service`: `LogisticsManager` (facade/coordinator), `InventoryService`, `RequestService`, `FleetService`, `DeliveryService`, `ReportService`.
  - `strategy`: `SourceSelectionStrategy` (NearestFirst, MostSurplus, Balanced) and `VehicleAssignmentStrategy` (SmallestFit, FastestFit) — interface-driven polymorphism and the basis of P5 experiments.
  - `exception`: `LogisticsException` (checked base) → `InvalidIdException`, `InsufficientStockException`, `NoVehicleAvailableException`, `InvalidStateTransitionException`.
  - `persistence`: generic `CsvRepository<T extends Persistable<T>>`.
  - `util`: `Distance` (haversine), `IdGenerator`, `Validator`.
  - `ui`: `ConsoleMenu`.
- [x] 1.3 Write Mermaid class diagram → `docs/class_diagram.md`; PNGs rendered with mermaid-cli (npm i @mermaid-js/mermaid-cli, PUPPETEER_EXECUTABLE_PATH=/opt/pw-browsers/chromium, --no-sandbox).
- [x] 1.4 Write sequence for the demo scenario (Pune medical kits) → `docs/sequence_demo.md`.
- [x] 1.5 Fill the **Traceability Matrix** in `docs/requirements.md` (rows = F/O reqs; columns = class(es), test, paper §, slide #). Leave test/paper/slide cells empty for now.
- [x] 1.7 Write `docs/ui_spec.md`: screen list with ASCII mock-ups (MON dashboard, BASE, INV, REQ, FLT, DLV, RPT, HELP), full command grammar table, colour palette (amber #FFB000 text, green/red for up/down/alerts, dim grey borders), minimum terminal size 100x30, Windows notes (Windows Terminal supported; legacy cmd.exe → `--plain`).
- [x] 1.6 Define state machines: Request (NEW→APPROVED→FULFILLING→FULFILLED | REJECTED | CANCELLED), Delivery (PLANNED→IN_TRANSIT→DELIVERED | FAILED).

## Exit checks
Every O1–O11 maps to ≥1 class · diagram files exist · no TODO left in requirements.md except test/paper/slide cells.
