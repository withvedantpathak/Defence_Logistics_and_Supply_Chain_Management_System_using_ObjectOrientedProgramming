# STATE update after Phase 6 (apply to team/STATE.md and team/HUMAN_INPUT.md when the PC is reachable)

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
