# Requirements & Traceability (distilled from Project Brief.docx)
## Functional
| ID | Requirement (from brief) | Acceptance criterion |
|----|--------------------------|----------------------|
| F1 | Add and manage defence bases | `BASE ADD/EDIT/REMOVE`; duplicate ids and bad coordinates rejected; base with stock/open deliveries cannot be removed |
| F2 | Track supplies and inventory at each base | `INV <base>` shows qty/reserved/available/reorder level with shortage & surplus flags; `INV ADJ` changes stock; low-stock alerts on dashboard |
| F3 | Create and manage supply requests | `REQ NEW/APPROVE/REJECT/CANCEL/FULFIL`; priority queue ordering; invalid transitions rejected with message |
| F4 | View available vehicles and assign one to a delivery | `FLT AVAIL [category]`; automatic assignment via strategy; manual override `DLV PLAN <req> VEH <id>` |
| F5 | Track deliveries and update their status | `DLV` list/detail; `DISPATCH/COMPLETE/FAIL`; `TICK` auto-completes at ETA; stock/vehicle/request updated consistently |
| F6 | Generate basic logistics reports | `RPT STOCK| IV-E |DLV|KPI` (stock by base, shortages/surplus, delivery status, fulfilment rate, avg lead time); export to CSV |

## OOP concepts (graded spine)
| ID | Concept | Brief's suggestion |
|----|---------|--------------------|
| O1 | Classes & objects | Base, Supply, Vehicle, SupplyRequest, Delivery |
| O2 | Encapsulation | private fields + accessors |
| O3 | Inheritance | Truck, FuelTruck extend Vehicle |
| O4 | Polymorphism | vehicle-specific delivery/capacity behaviour |
| O5 | Abstraction | abstract Vehicle |
| O6 | Interfaces | Trackable, Deliverable |
| O7 | Collections | ArrayList / HashMap |
| O8 | Exception handling | invalid IDs, insufficient stock, no vehicle |
| O9 | File handling | save inventory & delivery records |
| O10 | Generics | typed collections |
| O11 | Packages | models, services, utils |

## Non-functional
Offline · $0 · deterministic with seed · each command < 1 s on `large` data · CSV persistence · Java 17+ (zero runtime deps) · never crashes on bad input · terminal UI works in Windows Terminal; `--plain` fallback.

## Requirement for organisations' own data (added 2026-10-18; documented outside the paper and slides)
**B1 – Bring your own data.** An organisation can load its existing inventory held in CSV files: the program accepts any folder in the documented CSV format (`LOAD <folder>` or `--data <folder>`), reports malformed or inconsistent data with the file name and line number and without crashing, and ships with a blank template folder (`data/template/`), a worked example (`data/example_sme/`), a plain-English checker (`scripts/check_my_data.py`) and a step-by-step guide (`docs/bring_your_own_data.md`). Verified by: loading `data/example_sme/` through a scripted session and `--sim`; three malformed-folder cases; checker run on all of these. Not part of the paper or slides.

## Traceability matrix (Claude fills Test/Paper/Slide as work proceeds)
| Req | Class(es) | Test(s) | Paper § | Slide # | Done |
|-----|-----------|---------|---------|---------|------|
| F1 | `Base`, `Location`, `Registry<Base>`, `LogisticsManager`, `ui.TerminalApp` | `ManagerRulesTest.basesCanBeAddedAndSafelyRemoved`, `BaseAndSupplyTest`, `ServicesTest.registry…`, `TerminalTest` | IV-A, IV-E | | ☐ |
| F2 | `InventoryItem`, `Inventory`, `InventoryService`, `Supply` | `InventoryItemTest`, `ManagerRulesTest.inventoryAdjustRules`, `ServicesTest.inventoryServiceGuards`, `ScreensTest` | IV-B | | ☐ |
| F3 | `SupplyRequest`, `Priority`, `RequestStatus`, `RequestService` | `RequestAndDeliveryTest`, `ServicesTest.requestLifecycleAndOrdering`, `ManagerRulesTest`, `IntegrationTest.contendingRequests…` | IV-D | | ☐ |
| F4 | `Vehicle` hierarchy, `FleetService`, `VehicleAssignmentStrategy` impls | `VehicleTest`, `ServicesTest.fleet…`, `StrategyTest`, `StrategyInterfaceTest`, `ManagerRulesTest.manualVehicleOverride` | IV-B, IV-C | | ☐ |
| F5 | `Delivery`, `DeliveryStatus`, `DeliveryService`, `SimClock` | `PuneScenarioTest`, `RequestAndDeliveryTest`, `ConservationTest`, `IntegrationTest`, `ManagerRulesTest.fail…` | IV-D, VII | | ☐ |
| F6 | `ReportService`, `util.Table`, `ui.Screens` | `PuneScenarioTest.kpisAfterScenario`, `ScreensTest`, `TerminalTest.everyScreenRenders`, `TableAndIdGeneratorTest` | IV-E | | ☐ |
| O1 | `Base`, `Supply`, `Vehicle`, `SupplyRequest`, `Delivery`, `InventoryItem` | `BaseAndSupplyTest`, `InventoryItemTest`, `RequestAndDeliveryTest`, `VehicleTest` | IV-B | | ☐ |
| O2 | all model classes (private fields, validated ctors), `Location` record | `BaseAndSupplyTest` (validated constructors), `ValidatorTest`, `DistanceTest` | IV-B | | ☐ |
| O3 | `Truck`, `FuelTruck`, `MedicalVan`, `Helicopter` extend `Vehicle` | `VehicleTest.factoryBuildsRightSubclassFromCsv`, `VehicleTest.carryRulesDifferByType` | IV-B | | ☐ |
| O4 | `Vehicle.capacityFor/estimateHours` overrides; `SourceSelectionStrategy`, `VehicleAssignmentStrategy` impls | `VehicleTest.polymorphicCapacityAndTime` (same call, different results), `StrategyTest`, `StrategyInterfaceTest` | IV-B, IV-C | | ☐ |
| O5 | `abstract Vehicle`; strategy interfaces | `VehicleTest` (abstract type used through reference), `StrategyInterfaceTest` | IV-B, IV-C | | ☐ |
| O6 | `Trackable`, `Deliverable`, `Persistable` | `ServicesTest.interfacesWorkAcrossUnrelatedClasses` | IV-B | | ☐ |
| O7 | `Map`/`List`/`PriorityQueue`/`EnumMap`/`Comparator` in services | `ServicesTest`, `ServicesTest.requestLifecycleAndOrdering` (priority queue order) | IV-B | | ☐ |
| O8 | `exception.*` hierarchy; UI catches and shows | `ManagerRulesTest`, `TerminalTest.errorsAreShownNotThrown`, `scripts/smoke_test.sh` | IV-D | | ☐ |
| O9 | `CsvRepository`, `DataStore`, `SAVE/LOAD` | `DataStoreTest`, `CsvRepositoryTest`, `IntegrationTest.stateSurvivesSaveAndRestartMidScenario` | VI | | ☐ |
| O10 | `CsvRepository<T>`, `Registry<T>`, bounded generic methods | `CsvRepositoryTest`, `ServicesTest.registry…`, `ServicesTest.fleet…` (withStatus) | VI | | ☐ |
| O11 | package layout in `docs/design.md` §1 | `scripts/test_matrix.py` (package → test mapping, `results/test_matrix.csv`) | IV-A | | ☐ |

Design details: `docs/design.md` · UI: `docs/ui_spec.md` · Diagrams: `docs/class_diagram.md`, `docs/sequence_demo.md`.
