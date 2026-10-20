# STATE.md — live project tracker (Claude updates after every step)

**Current phase:** P7 (presentation); P6 paper drafted, awaiting G5 review  
**Started:** 2026-10-17 · **Deadline target:** +24 h  · **Budget spent:** $0

## Phase board
| Phase | Name | Est. | Status |
|-------|------|------|--------|
| P0 | Environment & scaffolding | 1 h | DONE |
| P1 | Requirements & design | 2 h | DONE |
| P2 | Dataset (synthetic) | 1.5 h | DONE |
| P3 | Core implementation | 5 h | DONE |
| P4 | Testing & QA | 2 h | DONE |
| P5 | Experiments & findings | 2 h | DONE (30 seeds, Holm) |
| P6 | Research paper | 3 h | DONE (draft, G5 pending) |
| P7 | Presentation | 2 h | TODO (next) |
| P8 | Packaging & final QA | 1 h | TODO |
| — | Buffer | ~4 h | — |

Status values: TODO · IN_PROGRESS · BLOCKED(Gx) · DONE

## Assumptions (apply unless the human overrides; log overrides in Decision Log)
- A1: Java 17 language level (built with JDK 21 `--release 17`), plain `javac` scripts, JUnit 4 + Hamcrest bundled in `lib/` for tests; zero runtime dependencies.
- A2: UI = **Bloomberg-terminal-style text terminal** (amber-on-black ANSI, dashboard home, command line with mnemonic codes). Pure Java, zero dependencies. No GUI. Spec in `workflow/phases/P3_implementation.md` slice 3.7 and `docs/ui_spec.md`.
- A3: Persistence = CSV files via java.io/NIO (satisfies "file handling"); no database.
- A4: Data is 100% synthetic, seeded RNG (reproducible).
- A5: "Findings" for the paper come from a simulation comparing supply-source & vehicle-assignment strategies (Strategy pattern) on synthetic scenarios.
- A6: Paper = IEEE-style, built from Markdown via pandoc → .docx and .pdf. Deck = .pptx. **Paper and deck are built ONLY AFTER the local project (P0–P5) is DONE and the human accepts it (G6).**
- A7: Authors (in this order): Vedant Pathak (R045), Dhairya Shah (R057), Kanishak Thakran (R046). Institution: SVKM's NMIMS MPSTME, Mumbai. Course/faculty name still unknown → leave a clearly marked field on the title page.
- A8: Paper ends with a short AI-assistance disclosure paragraph as the very last item (after references and appendices).

## Decision Log
| # | Date | Decision | Why | Tag |
|---|------|----------|-----|-----|
| 2 | 2026-10-17 | Bloomberg-style terminal UI, no GUI | Human choice (G1) | HUMAN |
| 3 | 2026-10-17 | Paper/deck deferred until local project accepted (G6) | Human choice (G1) | HUMAN |
| 4 | 2026-10-17 | Authors + AI disclosure paragraph at very end | Human choice (G1) | HUMAN |
| 1 | 2026-10-17 | Build in cloud workdir, sync to folder per phase | Device shell has JRE only | FACT |
| 5 | 2026-10-17 | Dropped Maven/JaCoCo/JUnit5 for plain javac + JUnit 4 jars in lib/ | repo.maven.apache.org blocked by sandbox allowlist; user then needs only a JDK | FACT |
| 8 | 2026-10-17 | Added user manual + UML-sync steps (human request) | Human asked for UML class diagram + user manual | HUMAN |
| 7 | 2026-10-17 | Domain rules fixed in docs/design.md §4 (donor floor, request splitting across donors/vehicles, hour-based SimClock, reservation on plan) | Makes P3 and P5 deterministic and testable | ASSUMED |
| 6 | 2026-10-17 | Target Java 17 (not 21) | PC-side JRE is 11; 17 is a common LTS and keeps records/switch exprs | ASSUMED |

## Step log
(Claude appends: `YYYY-MM-DD HH:MM  Pn.step  note`)
- 2026-10-17 P2 DONE: gen_data.py/validate_data.py; data/{small,medium,large}; results/dataset_summary.csv; docs/dataset.md. Reproducible (SHA-256), validator negative-tested. Also: docs/user_manual.md drafted from ui_spec (to be verified in P3.12/P4.6b/P8.3b).
- 2026-10-17 P1 DONE: docs/{requirements,design,ui_spec,class_diagram,sequence_demo}.md + rendered PNGs in docs/img (mermaid-cli via npm + local Chromium works, reuse in P6). Traceability class column filled for F1–F6, O1–O11.
- 2026-10-17 P0 DONE: javac+JUnit4 scaffold green; synced to `OOP/dlscms/`. Course/faculty = placeholder (reminder queued in HUMAN_INPUT.md).
- 2026-10-17 P3 DONE: full model, services, strategies, CSV persistence, terminal UI (MON/BASE/INV/REQ/FLT/DLV/TICK/RPT/STRAT/AUTO/SAVE/LOAD/DEMO), launchers run.bat/run.sh, 85 JUnit tests green, UML + user manual re-synced. Synced to dlscms/. Next: P4.
- 2026-10-18 team/ROADMAP.md created; repo layout prepared in OOP/github_repo (push pending: Claude has no GitHub write access).
- 2026-10-18 P4 DONE: 111 tests/20 classes, results/test_matrix.csv (59/59 types), scripts/smoke_test.sh, scripts/verify_manual.sh, docs/lessons.md; fixed bug: refused request burned an id.
- 2026-10-18 P5 DONE: sim package (Scenario/Simulator/Experiment, random baselines), scripts/run_experiments.sh + analyze.py + check_repro.sh, 360 runs, results/{raw/runs.csv,summary.csv,stats.txt}, 6 figures in paper/figures, docs/findings.md (6 findings + threats). 118 tests. Sim-clock check: header clock moves only on TICK (now labelled SIM, documented). PC jar refreshed. NEXT: gate G6 - user acceptance, then P6 paper, P7 slides.

## Update 2026-10-18 (Phase 6)

**Phase board:** P4 DONE · P5 DONE (rerun with 30 seeds + Holm, 2026-10-18) · P6 DONE as draft (awaiting G5 review) · P7 TODO (next) · P8 TODO.
**Current phase:** P7 (presentation).

**Decision log additions**
| # | Date | Decision | Why | Who |
|---|------|----------|-----|-----|
| 14 | 2026-10-18 | G6 treated as passed: user said "Proceed with Phase 6" after trying the model | Explicit instruction to start the paper | HUMAN |
| 15 | 2026-10-18 | Experiments rerun with 30 seeds and Holm-Bonferroni correction (93 tests) | 10 seeds cannot reach p below 0.00195 and cannot be corrected | HUMAN request, Claude executed |
| 16 | 2026-10-18 | ~2% unfulfilled requests diagnosed: stock-limited (reorder floor), fuel-truck backlog, end of horizon; not a bug; listed under "Known inconsistencies" at the end of the paper | User: fix the data or note the inconsistency at the end of the report | HUMAN |
| 17 | 2026-10-18 | Bring-your-own-data requirement B1 + guide/template/checker kept OUT of paper and slides | User instruction | HUMAN |
| 18 | 2026-10-18 | Paper built with a Node/docx script from paper/paper.md (two-column A4, IEEE-like), PDF via LibreOffice; 9 verified references | Pandoc cannot do two-column layout; unverifiable references dropped (Holguín-Veras et al. 2012 omitted: title could not be confirmed) | ASSUMED |

**Step log**
- 2026-10-18 P5b DONE: sim records stock_limited/vehicle_limited/in_flight; 1,080 runs; Holm correction; results/unfulfilled_*.csv; findings rewritten; 119 tests green.
- 2026-10-18 P6 DONE (draft): paper/paper.md → paper.docx + paper.pdf (12 pages); `python3 scripts/paper/check_numbers.py` passes (52 checks); AI disclosure is the last item; Paper § column of the traceability matrix filled.
- Bring-your-own-data: docs/bring_your_own_data.md, data/template, data/example_sme, scripts/check_my_data.py.

**HUMAN_INPUT.md — still open:** (1) course + faculty name (placeholder `[COURSE NAME — FACULTY NAME]` in the title block of the paper; REMIND at the end); (2) G5: read the paper, especially "Known inconsistencies", and check your university's AI-disclosure rule; (3) install JDK 17+.

## Submission checklist from faculty (Dr. Sakshi Ranjan), presentations 27 and 29 Oct 2026
1 Project report (hard copy, stick file) · 2 UML docs on GitHub, link in report and ppt · 3 Java source on GitHub, link in report and ppt · 4 Executable on laptop (needs JDK 17+) · 5 PPT 15 min on smartboard + live demo. Graded on the day only.
