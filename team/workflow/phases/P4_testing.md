# P4 — Testing & QA (≈2 h)
**Goal:** prove correctness; produce evidence for the paper.

## Steps
- [ ] 4.1 JUnit 4 unit tests: each model's validation; each vehicle subtype's polymorphic behaviour; each exception path (invalid ID, insufficient stock, no vehicle, bad state transition); CSV round-trip (save→load equals).
- [ ] 4.2 Integration tests: full demo scenario; multi-request contention; partial fulfilment; persistence across restart.
- [ ] 4.3 Property-style checks (plain JUnit loops, seeded): total stock conserved across transfers; no negative stock; every DELIVERED request has a matching delivery.
- [ ] 4.4 Coverage proxy (no JaCoCo): `scripts/test_matrix.py` lists every public class in model/service/strategy/exception/persistence and its test class(es) → `results/test_matrix.csv`; target 100% have ≥1 test; also report test counts.
- [ ] 4.5 Manual CLI smoke test with scripted stdin (`java -jar ... < scripts/smoke_input.txt`), including garbage input.
- [ ] 4.6 Fix bugs found; record notable ones in `docs/lessons.md` (paper "Challenges" section material).
- [ ] 4.6b Create `scripts/verify_manual.sh`: runs the manual's quick-start (§5) and error examples (§8) through `java -jar dist/dlscms.jar --plain --script ...` and diffs against expected key lines; wire into the P8 clean-room test.
- [ ] 4.7 Fill the "test" column of the traceability matrix.

## Exit checks
All tests pass · test_matrix.csv shows no untested class · smoke test exits 0 with no stack traces.
