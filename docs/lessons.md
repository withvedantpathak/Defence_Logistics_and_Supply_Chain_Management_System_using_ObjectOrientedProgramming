# Lessons learned (material for the paper's "Challenges" section)

| # | Problem | How it was found | Fix | Lesson |
|---|---|---|---|---|
| 1 | Maven Central unreachable in the build sandbox | `mvn` returned HTTP 403 on first dependency download | Dropped Maven/JaCoCo; plain `javac`/`jar` scripts with JUnit 4 jars bundled in `lib/`; coverage proxy `scripts/test_matrix.py` | Keep the build dependency-free; a zero-download build also works offline for graders |
| 2 | Java 11 on the team PC vs class files built for Java 21 | `UnsupportedClassVersionError` | Compile with `--release 17`, document "JDK 17+" in README and manual | Pin the target release, not just the JDK |
| 3 | A refused request consumed an id (R-0001 skipped) | Stdin smoke test with hostile input, then reading the next created id | `RequestService.create` restores the id counter if construction fails; regression test `refusedRequestsDoNotBurnIds` | Generate ids only after validation, or roll back; fuzz-style input finds what unit tests miss |
| 4 | Saved CSVs were re-ordered compared with the generator output | Round-trip byte comparison test failed | Registries are sorted by id on purpose (deterministic output); the test now compares content, not order | Decide what is the contract (content vs order) before writing the equality test |
| 5 | Mermaid sequence diagram refused to render | Parse error on `;` and `+=` inside message text | Reworded the messages | Keep diagram text plain; render diagrams in CI/scripts so they cannot rot |
| 6 | Class diagram lost its `<<interface>>` markers | Visual check of the rendered PNG | Split into three multi-line diagrams | Check rendered output, not only the source |
| 7 | Manual and program drifted (shortage-hours KPI, error wording) | `scripts/verify_manual.sh` and manual review | Manual corrected; the script now fails if a documented command or ERR message is not produced | Verify documentation mechanically |
| 8 | Stock could be created or lost if a delivery failed mid-way | Designed in; checked by seeded random-operation tests (`ConservationTest`) | Reserve on plan, remove on dispatch, add on completion, return on failure | State machines plus invariants checked after every step beat example-based tests for this kind of bug |
| 9 | Unzipping the sync bundle on the PC was blocked ("Operation not permitted" on delete) | Error during file sync | Requested folder delete permission once | Plan for tool permissions when automating file sync |
| 10 | Many paired tests with only 10 seeds: the smallest possible Wilcoxon p-value is 0.00195, so p-values saturate and no multiple-comparison correction is possible | Reading `results/stats.txt` | Reran with 30 seeds (smallest attainable p about 1.9e-9) and added Holm-Bonferroni correction over all 93 paired tests (74 remain significant) | Decide the number of seeds and the correction rule before running the analysis |
| 11 | Figure legend covered bars (vehicle-strategy chart) | Viewing the rendered PNG | Moved the legend below the plots | Always look at generated figures |
| 12 | Scenario replay time grew 14x for 5x more requests | Scalability numbers | Documented as super-linear; suggested indexing waiting requests (future work) | Report measured behaviour, do not assume linear scaling |
| 13 | About 2% of requests stayed unfulfilled in every run and the cause was unknown | Reading Phase 5 results; one affected run inspected with a throw-away diagnostic, then `scripts/diagnose_unfulfilled.sh` over 30 seeds | Not a defect: donors keep their reorder level, the fuel-truck fleet (about 15% of vehicles) builds a backlog of FUEL requests, and runs stop at a fixed horizon. The simulator now records the reason per run (`stock_limited`, `vehicle_limited`, `in_flight`) | An unexplained residual in results is a question to answer with data, not a footnote; add the instrumentation that would have answered it |
| 14 | 244 requests looked like "waiting although a suitable vehicle is idle" | The diagnostic checked vehicle availability after the final clock tick | Re-running one planning pass at the horizon end made all of them disappear: an end-of-horizon artefact, not a planning defect | Test the explanation with an intervention (replan) before calling something a bug or a feature |

## Testing summary (Phase 4)
- 119 JUnit tests in 21 test classes; every one of the 66 types is referenced by at least one test class (`results/test_matrix.csv`).
- Seeded random sequences (small x5 seeds x250 steps, medium x1 seed x400 steps) assert after **every** step that total stock per supply is conserved, no stock is negative, reserved stock equals PLANNED deliveries, and no vehicle has two open deliveries.
- `scripts/smoke_test.sh` feeds 56 hostile or malformed input lines through stdin: exit code 0, no stack trace, 39 refusals shown as `ERR` lines.
- `scripts/verify_manual.sh` runs the manual's tutorial and error table against the program.
- Limitation: no line-coverage figure (JaCoCo needs a download); the matrix is a name-based proxy, not proof of line coverage.

## Performance check
`MON, INV, REQ, RPT KPI, RPT STOCK, DLV` on the **large** dataset (12 bases, 12 supplies, 60 vehicles, 1000 requests) complete in 0.9 s wall time including JVM start; planning a 300-unit request across donors takes 0.4 s. The non-functional target (each command < 1 s) is met.
