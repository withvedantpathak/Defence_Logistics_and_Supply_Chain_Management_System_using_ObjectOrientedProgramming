# Demo script (about 5 minutes, during the slide "Live demo")

Before the session: laptop has JDK 17 or newer (`java -version`), the project folder is unzipped, the terminal is open in it, font size is large. Backup: `run.bat --demo` auto-plays a scripted version and leaves the program live.

| Step | Type | Say | Expect on screen |
|---|---|---|---|
| 1 | `run.bat` (Linux/Mac: `./run.sh`) | "The example network has bases, stock and a fleet." | MON dashboard |
| 2 | `REQ NEW PUN MED-KIT 200 URGENT` | "Pune asks for 200 medical kits." | Request R-0001, status NEW |
| 3 | `REQ APPROVE R-0001` | "A commander approves it." | Status APPROVED |
| 4 | `REQ FULFIL R-0001` | "The donor strategy picks a base, the vehicle strategy picks a vehicle; stock is reserved." | Planned delivery D-0001, donor and vehicle shown |
| 5 | `DLV DISPATCH D-0001` | "Stock leaves the donor and the vehicle is in transit." | IN_TRANSIT, ETA hour |
| 6 | `TICK 12` | "Time only moves on TICK." | Delivery DELIVERED, Pune stock up |
| 7 | `RPT KPI` | "Lead time, utilisation, fulfilment." | KPI table |
| 8 | `STRAT SRC BALANCED` (and `STRAT VEH SMALLEST`), then repeat steps 2-4 for another request | "Swap the rule without changing code." | Different donor may be chosen |

If something goes wrong: `LOAD` the saved demo folder, or restart with `run.bat --demo`.
Likely questions: why BALANCED wins (it avoids draining one donor); why about 2% stay open (donor reorder floor and few fuel trucks, see paper section on known inconsistencies); loading real data (`docs/bring_your_own_data.md`, kept out of the paper by design).
