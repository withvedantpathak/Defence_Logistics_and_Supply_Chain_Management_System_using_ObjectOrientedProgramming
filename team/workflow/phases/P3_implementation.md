# P3 — Core implementation (≈5 h)
**Goal:** all six features working through the console UI with CSV persistence.
**Needs human:** none. Build in vertical slices; keep the build green.

## Slices (commit after each)
- [ ] 3.1 **Models + enums + interfaces + exceptions** (encapsulation: private fields, validation in constructors/setters, immutability where sensible, `equals/hashCode/toString`).
- [ ] 3.2 **Vehicle hierarchy**: abstract `Vehicle`; `Truck`, `FuelTruck`, `MedicalVan` (+ optional `Helicopter`). Override `capacityFor(category)`, `estimateHours(distanceKm)`, `canCarry(supply)`.
- [ ] 3.3 **Persistence**: `Persistable<T>` + generic `CsvRepository<T>`; load/save bases, inventory, vehicles, requests, deliveries. Graceful errors on malformed rows.
- [ ] 3.4 **Services**: InventoryService (add/remove/transfer, reorder alerts), FleetService (list available, assign/release), RequestService (create/approve/reject/cancel), DeliveryService (plan→dispatch→complete/fail, updates inventory atomically), ReportService (stock by base, shortages/surplus, delivery status summary, fulfilment rate, avg lead time).
- [ ] 3.5 **Strategies**: implement the 3 source-selection + 2 vehicle-assignment strategies behind interfaces; `LogisticsManager` takes them via constructor (dependency injection).
- [ ] 3.6 **LogisticsManager** facade: `fulfil(requestId)` = check surplus elsewhere → choose source → choose vehicle → create delivery → reserve/decrement stock → on completion increment destination. Throw domain exceptions for all failure paths.
- [ ] 3.7 **Terminal UI (Bloomberg-style)** — implement exactly per `docs/ui_spec.md` (write that file in P1 step 1.7): amber-on-black ANSI, header/ticker/status bars, dashboard home (MON), command line with mnemonic function codes (`BASE`, `INV`, `REQ`, `FLT`, `DLV`, `RPT`, `DEMO`, `SAVE`, `HELP`), arguments inline (`INV PUNE`, `REQ NEW PUNE MED-KIT 200 HIGH`), `?` for per-command help, numbered shortcuts as fallback, `--plain` flag (no ANSI) for terminals that lack VT support. Redraw-after-each-command (no raw-mode / no JLine). Input validation; never crash on bad input; every domain exception shows as a red status-bar message, not a stack trace. Unit-test the command parser and screen renderers (render to String).
- [ ] 3.8 **Demo scenario** command (`--demo` and terminal `DEMO`): scripted Pune medical-kits flow with narrated output.
- [ ] 3.9 Use generics/collections deliberately: `Map<String, Base>`, `List<Vehicle>`, `Map<String, Map<String,Integer>>` inventory, `PriorityQueue<SupplyRequest>`, `Comparator`s; add one bounded generic method (e.g., `<T extends Trackable> List<T> available(List<T>)`).

- [ ] 3.10 **Launchers**: `run.bat` and `run.sh` (call `java -jar dist/dlscms.jar %*`, default `--data data/small`) exactly as documented in `docs/user_manual.md` §3.
- [ ] 3.11 **Re-sync UML with the real code**: compare `docs/diagrams/*.mmd` against actual classes/methods (use `javap -p -cp out/classes <class>`), fix drift in the .mmd files, re-render PNGs to `docs/img/` (mmdc recipe in P1 step 1.3).
- [ ] 3.12 **Manual sync**: run every example command from `docs/user_manual.md` through `--script`; update outputs/screens and any renamed commands in the manual; fix the code instead if the manual (spec) is right. Record in STATE.md.

## Exit checks
`scripts/build.sh test` green · `java -jar dist/dlscms.jar --demo` completes with a DELIVERED status and correct stock deltas · each of O1–O11 has a code pointer filled in the traceability matrix · sync to device.
