# Terminal UI spec — "DLSCMS Terminal" (Bloomberg-style)
Pure Java, no libraries. Screen is redrawn after every command (clear screen + print). Min size 100×30.

## Look
- Black background, **amber (#FFB000) text**, white for emphasis, dim grey borders; **green** = healthy/up/delivered, **red** = shortage/failed/error, **cyan** = ids and command hints. 24-bit/256-colour ANSI escapes; `--plain` prints the same layout with no colour (and ASCII borders) for legacy terminals and for tests.
- Every screen = 3 zones: **header bar** (title · screen code · sim clock `DAY dd HH:00` · active strategies), **body**, **footer** (alert/status line + command prompt `DLSCMS ▸ `).
- Function codes are typed at the prompt and executed on Enter (Bloomberg `<GO>`). Case-insensitive. `?` after any code shows usage. Empty Enter refreshes the current screen. `MON` always returns home.

## Command grammar
| Code | Forms | Effect |
|------|-------|--------|
| `MON` | `MON` | dashboard (home) |
| `BASE` | `BASE` · `BASE <id>` · `BASE ADD <id> <name> <lat> <lon> [cap]` · `BASE REMOVE <id>` | list / detail / add / remove (F1) |
| `INV` | `INV` · `INV <base>` · `INV ADJ <base> <supply> <±qty>` · `INV SET <base> <supply> <qty> [reorder]` | stock views and changes (F2) |
| `SUP` | `SUP` | supply catalogue |
| `REQ` | `REQ` · `REQ <id>` · `REQ NEW <base> <supply> <qty> [ROUTINE\|URGENT\|CRITICAL]` · `REQ APPROVE\|REJECT\|CANCEL <id>` · `REQ FULFIL <id>` | requests (F3) |
| `FLT` | `FLT` · `FLT AVAIL [category]` · `FLT <id>` · `FLT MAINT <id> ON\|OFF` | fleet (F4) |
| `DLV` | `DLV` · `DLV <id>` · `DLV PLAN <req> [VEH <vehicle>]` · `DLV DISPATCH\|COMPLETE <id>` · `DLV FAIL <id> [reason]` | deliveries (F4, F5) |
| `TICK` | `TICK <hours>` | advance simulated clock; auto-complete arrivals |
| `RPT` | `RPT STOCK\|SHORT\|DLV\|KPI` · `RPT <kind> EXPORT` | reports (F6), export writes CSV to `reports/` |
| `STRAT` | `STRAT` · `STRAT SRC NEAREST\|SURPLUS\|BALANCED` · `STRAT VEH SMALLEST\|FASTEST` | view / switch strategies |
| `SAVE` / `LOAD` | `SAVE [folder]` · `LOAD [folder]` | CSV persistence (O9) |
| `DEMO` | `DEMO` | run the scripted Pune medical-kits scenario step by step (press Enter between steps) |
| `HELP` | `HELP` · `<code> ?` | command reference |
| `EXIT` | `EXIT` · `QUIT` | save prompt, then exit |

Errors never print stack traces: domain exceptions appear as a red footer line `ERR ▸ <message>`; unknown codes suggest the nearest valid code (edit distance ≤ 2).

## Screens (ASCII mock-ups; widths are 100 columns)
### MON — dashboard
```
 DLSCMS ▸ DEFENCE LOGISTICS TERMINAL         [MON]        DAY 03 14:00        SRC:NEAREST  VEH:FASTEST
 ─────────────────────────────────────────────────────────────────────────────────────────────────────
  SHORTAGES (below reorder)        │ OPEN REQUESTS                  │ FLEET
  PUN  MED-KIT   ▼  40 / 120       │ R-0007 URGENT  PUN MED-KIT 200 │ Truck        6 / 9 free
  LEH  FUEL      ▼ 600 / 2000      │ R-0008 ROUTINE JAI RATION  500 │ FuelTruck    2 / 4 free
  GUW  SPARES    ▼  12 / 50        │ R-0009 CRITICAL LEH FUEL  1200 │ MedicalVan   3 / 3 free
 ─────────────────────────────────────────────────────────────────────────────────────────────────────
  ACTIVE DELIVERIES                                               │ KPI (today)
  D-0012 MUM→PUN MED-KIT 200  MV-01  IN_TRANSIT  ETA 03 20:00     │ Fulfilment rate   82%
  D-0013 DEL→LEH FUEL    600  FT-02  PLANNED     —                │ Avg lead time     14.2 h
 ─────────────────────────────────────────────────────────────────────────────────────────────────────
  ALERT ▸ PUN: MED-KIT below reorder level (40 < 120)
 DLSCMS ▸ _
```
### INV <base> — stock ledger
```
 [INV PUN] PUNE BASE (18.52N 73.86E)  cap 5000                       DAY 03 14:00
  SUPPLY     NAME             CAT      QTY  RSVD  AVAIL  REORDER  STATUS
  MED-KIT    Medical kit      MEDICAL   40     0     40      120  ▼ SHORT  -80
  RATION     Ration pack      FOOD    1800   200   1600      900  ▲ OK     +700
```
### REQ — request book (priority-sorted)
```
 [REQ]  ID      PRIO      BASE SUPPLY    QTY  DONE  STATUS       AGE
        R-0009  CRITICAL  LEH  FUEL     1200     0  APPROVED     4h
        R-0007  URGENT    PUN  MED-KIT   200   200  FULFILLED    9h
```
### FLT / DLV / BASE / RPT
Same column-table style (`util.Table`): header row in white, ids in cyan, status coloured (green AVAILABLE/DELIVERED, amber ASSIGNED/PLANNED/IN_TRANSIT, red FAILED/MAINTENANCE). `DLV <id>` detail screen shows route, distance, vehicle, timeline (planned → dispatched → ETA → delivered). `RPT KPI` shows fulfilment rate, avg lead time by priority, vehicle utilisation, shortage-hours.
### HELP — one page: the grammar table above, plus 5 example commands.

## Behaviour rules
1. Tokens split on whitespace; ids are case-insensitive and normalised to upper case; quantities must be positive integers (`INV ADJ` signed).
2. Destructive or irreversible commands (`BASE REMOVE`, `LOAD`, `EXIT` with unsaved changes) ask `CONFIRM (Y/N)`.
3. `--plain` disables ANSI; `--demo` runs the demo non-interactively and exits; `--script <file>` executes commands from a file (used for smoke tests and for the paper's screenshots); `--data <folder>` selects data folder (default `data/`).
4. All renderers are pure functions `String render(Model, width)` so they can be unit-tested and reused by the report exports.
5. Windows: Windows Terminal (and Windows 10+ conhost with VT) show colour; if the first escape is not honoured the user runs `run.bat --plain`. UTF-8 box characters; plain mode uses `+ - |`.
