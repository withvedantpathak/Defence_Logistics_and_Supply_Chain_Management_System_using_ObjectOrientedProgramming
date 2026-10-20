# Class diagrams (Mermaid source; rendered PNGs in docs/img/)
Sources live in `docs/diagrams/*.mmd`. Re-render: `mmdc -p pp.json -i docs/diagrams/X.mmd -o docs/img/class_X.png -s 2 -b white`.

## 1. Domain model
```mermaid
classDiagram
direction TB
class Trackable {
  <<interface>>
  +id() String
  +statusLabel() String
  +locationLabel() String
}
class Deliverable {
  <<interface>>
  +dispatch(hour)
  +complete(hour)
  +fail(reason, hour)
}
class Persistable {
  <<interface>>
  +csvHeader() String
  +toCsvRow() String
}
class Vehicle {
  <<abstract>>
  -id : String
  -currentBaseId : String
  -capacityKg : int
  -speedKmph : int
  -status : VehicleStatus
  +typeName()* String
  +canCarry(cat)* boolean
  +loadingHours() double
  +unitsPerTrip(cat, unitKg) int
  +estimateHours(km, cat) double  «template method»
}
class Truck
class FuelTruck
class MedicalVan
class Helicopter
class Base {
  -id : String
  -name : String
  -location : Location
}
class Supply {
  -id : String
  -category : SupplyCategory
  -unitWeightKg : double
}
class InventoryItem {
  -qty : int
  -reserved : int
  -reorderLevel : int
  +available() int
  +surplus() int
  +shortage() int
}
class SupplyRequest {
  -qty : int
  -fulfilledQty : int
  -priority : Priority
  -status : RequestStatus
}
class Delivery {
  -qty : int
  -status : DeliveryStatus
  -etaHour : long
  -distanceKm : double
}
Vehicle <|-- Truck
Vehicle <|-- FuelTruck
Vehicle <|-- MedicalVan
Vehicle <|-- Helicopter
Trackable <|.. Vehicle
Trackable <|.. Delivery
Deliverable <|.. Delivery
Persistable <|.. Vehicle
Persistable <|.. Delivery
Persistable <|.. Base
Persistable <|.. Supply
Persistable <|.. SupplyRequest
Persistable <|.. InventoryItem
Base "1" o-- "*" InventoryItem : stocks
InventoryItem "*" --> "1" Supply
SupplyRequest "*" --> "1" Base : requester
SupplyRequest "*" --> "1" Supply
Delivery "*" --> "1" SupplyRequest : serves
Delivery "*" --> "1" Vehicle : uses
Delivery "*" --> "2" Base : source and destination
```

## 2. Services, strategies and persistence
```mermaid
classDiagram
direction LR
class Main {
  +main(args)
  +run(args, in, out) int
}
class TerminalApp {
  +run()
  +execute(line)
}
class Screens {
  +dashboard() String
  +requests() String
  +report(kind) String
}
class Terminal {
  +paint(text, style) String
  +table(table, indent) String
}
class LogisticsManager {
  +createRequest(base, supply, qty, prio)
  +fulfil(requestId) List~Delivery~
  +planOne(requestId, vehicleId)
  +dispatch(id)
  +complete(id)
  +fail(id, reason)
  +tick(hours)
  +useSourceStrategy(name)
  +useVehicleStrategy(name)
}
class World {
  +bases : Registry~Base~
  +supplies : Registry~Supply~
  +vehicles : Registry~Vehicle~
  +requests : Registry~SupplyRequest~
  +deliveries : Registry~Delivery~
  +clock : SimClock
}
class Registry~T~ {
  +add(item)
  +get(id) T
  +all() List~T~
}
class InventoryService
class FleetService
class RequestService
class DeliveryService
class ReportService
class SourceSelectionStrategy {
  <<interface>>
  +select(request, candidates) Optional
}
class VehicleAssignmentStrategy {
  <<interface>>
  +select(options, qtyWanted) Optional
}
class NearestFirst
class MostSurplus
class Balanced
class SmallestFit
class FastestFit
class CsvRepository~T extends Persistable~ {
  +save(dir, items)
  +load(dir, optional) List~T~
}
class DataStore {
  +save(world, dir)
  +load(dir) World
}
Main --> TerminalApp
TerminalApp --> LogisticsManager
TerminalApp --> Screens
Screens --> Terminal
Screens --> LogisticsManager
LogisticsManager --> World
World o-- Registry
LogisticsManager --> InventoryService
LogisticsManager --> FleetService
LogisticsManager --> RequestService
LogisticsManager --> DeliveryService
LogisticsManager --> ReportService
LogisticsManager --> SourceSelectionStrategy : injected
LogisticsManager --> VehicleAssignmentStrategy : injected
SourceSelectionStrategy <|.. NearestFirst
SourceSelectionStrategy <|.. MostSurplus
SourceSelectionStrategy <|.. Balanced
VehicleAssignmentStrategy <|.. SmallestFit
VehicleAssignmentStrategy <|.. FastestFit
DataStore --> CsvRepository
TerminalApp ..> DataStore : SAVE and LOAD
```

## 3. Exception hierarchy
```mermaid
classDiagram
direction LR
class LogisticsException {
  <<checked>>
}
LogisticsException <|-- InvalidIdException
LogisticsException <|-- InsufficientStockException
LogisticsException <|-- NoVehicleAvailableException
LogisticsException <|-- InvalidStateTransitionException
LogisticsException <|-- ValidationException
LogisticsException <|-- DataFormatException
```

