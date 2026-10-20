# Defence Logistics and Supply Chain Management System (DLSCMS)

Object-oriented Java (17) application that manages stock across a network of bases, turns supply **requests** into **deliveries** with the right donor base and vehicle, and tracks everything on a simulated clock. It runs as a Bloomberg-style amber-on-black **terminal**. No external libraries, no database, no internet needed.

**Authors:** Vedant Pathak (R045), Dhairya Shah (R057), Kanishak Thakran (R046) — SVKM's NMIMS MPSTME, Mumbai.
All data is synthetic (generated with a fixed seed); no real operational data is used.

## Quick start
Requires **JDK 17 or newer** (`java -version`).

| Goal | Windows | Linux / macOS |
|---|---|---|
| Run the terminal | `run.bat` | `./run.sh` |
| No colours (old terminals) | `run.bat --plain` | `./run.sh --plain` |
| Scripted demo, then exit | `run.bat --demo` | `./run.sh --demo` |
| Other dataset | `run.bat --data data\medium` | `./run.sh --data data/medium` |
| Build and run all tests | `scripts\build.bat test` | `scripts/build.sh test` |

The launchers build `dist/dlscms.jar` automatically on first run. Type `HELP` inside the program; the full guide is [docs/user_manual.md](docs/user_manual.md).

## Repository layout
| Path | Contents |
|---|---|
| `src/main/java/com/dlscms/` | Source: `model`, `model/vehicle`, `service`, `strategy`, `persistence`, `ui`, `util`, `exception` |
| `src/test/java/com/dlscms/` | JUnit 4 tests (run with `scripts/build.sh test`) |
| `data/{small,medium,large}/` | Synthetic CSV datasets (seed 42) |
| `scripts/` | Build scripts, dataset generator/validator, test matrix, smoke test, manual verifier, experiment runner (`run_experiments.sh`) and analysis (`analyze.py`) |
| `lib/` | JUnit and Hamcrest jars used only for tests |
| `docs/` | Requirements, design, UI spec, UML diagrams (`docs/img`), dataset notes, user manual |
| `results/` | Dataset summary, test matrix, raw experiment runs (`raw/`), `summary.csv`, `stats.txt` |
| `paper/`, `slides/` | Research paper and presentation (added after the model is accepted) |
| `team/` | Team-only notes: [ROADMAP](team/ROADMAP.md), project state, workflow files, course brief |

## Design in one paragraph
Domain classes (`Base`, `Supply`, `InventoryItem`, `SupplyRequest`, `Delivery`, and a `Vehicle` hierarchy of `Truck`, `FuelTruck`, `MedicalVan`, `Helicopter`) are coordinated by the `LogisticsManager` facade. Donor-base and vehicle choice use the Strategy pattern and can be switched at run time. Persistence is generic (`CsvRepository<T extends Persistable>`). Errors are checked exceptions from a custom hierarchy and are shown as a red `ERR` line, never a stack trace. See [docs/design.md](docs/design.md) and the UML diagrams in [docs/class_diagram.md](docs/class_diagram.md).

## Status
Phases 0–5 done (requirements, design, datasets, implementation, 118 passing tests, QA scripts, 360-run experiment grid with findings in `docs/findings.md`). Next: your acceptance of the working model, then paper and slides. Progress lives in [team/STATE.md](team/STATE.md).
