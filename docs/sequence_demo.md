# Demo scenario sequence — "Pune needs medical kits"
```mermaid
sequenceDiagram
actor Op as Logistics officer
participant UI as TerminalApp
participant LM as LogisticsManager
participant RS as RequestService
participant IS as InventoryService
participant SS as SourceSelectionStrategy
participant FS as FleetService
participant VS as VehicleAssignmentStrategy
participant DS as DeliveryService
Op->>UI: REQ NEW PUN MED-KIT 200 URGENT
UI->>LM: createRequest(PUN, MED-KIT, 200, URGENT)
LM->>RS: add(NEW)
Op->>UI: REQ APPROVE R-0001
UI->>LM: requests().approve(R-0001)
Op->>UI: REQ FULFIL R-0001
UI->>LM: fulfil(R-0001)
loop until remaining = 0 or nothing plannable
  LM->>IS: candidates(MED-KIT, excluding PUN)
  LM->>SS: select(request, candidates)
  SS-->>LM: donor base MUM
  LM->>FS: eligible vehicles (AVAILABLE, canCarry MEDICAL)
  LM->>VS: select(options, qty)
  VS-->>LM: e.g. Helicopter HE-01 (FASTEST)
  LM->>DS: plan(donor, PUN, qty, vehicle)  reserve stock and set vehicle ASSIGNED
end
Op->>UI: DLV DISPATCH D-0001
UI->>LM: dispatch(D-0001)  [donor stock decreases, vehicle IN_TRANSIT, ETA set]
Op->>UI: TICK 12
UI->>LM: tick(12) -> auto-complete when ETA reached
LM->>IS: Pune stock increases, request fulfilledQty increases, vehicle AVAILABLE at PUN
Op->>UI: RPT KPI
```
## State machines
Request: `NEW → APPROVED → FULFILLING → FULFILLED`; `NEW|APPROVED → REJECTED|CANCELLED`; `FULFILLING → APPROVED` (when a delivery fails and nothing else in flight).
Delivery: `PLANNED → IN_TRANSIT → DELIVERED`; `PLANNED|IN_TRANSIT → FAILED`.
Vehicle: `AVAILABLE → ASSIGNED → IN_TRANSIT → AVAILABLE`; `AVAILABLE ↔ MAINTENANCE`.
