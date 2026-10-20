# Human Input Queue
Claude: append new items under **Open**, move to **Resolved** when answered. Always include a default.

## Standing gates (known up-front)
| ID | When | What I need from you | Default if you're unavailable |
|----|------|----------------------|-------------------------------|
| G1 | Before P0 | Confirm kickoff answers (team names, course/faculty, UI choice, deck format) | See `STATE.md` → Assumptions |
| G6 | End of P5 | Review the working local model (run the demo, try the terminal) and say "accepted" — only then does P6 (paper) and P7 (slides) start | None: Claude does NOT start P6/P7 without it |
| G2 | Before P8 | Install JDK 17 or newer on your machine (Adoptium Temurin, free) so the demo runs locally; run the one-liner and tell me pass/fail | Claude ships a jar + a Windows `run.bat` and flags it untested on your PC |
| G3 | P2 (only if wanted) | If you want a *real public* dataset instead of synthetic, point me to it / download it | Synthetic data generator |
| G4 | P7 | Record/perform the live demo (or approve a scripted recording) | Provide demo script + screenshots |
| G5 | P8 | Final read-through; check your university's rules on AI assistance / disclosure and report format | Claude adds an AI-assistance disclosure paragraph to the paper |

## Open
- **REMIND AT END (before P6 title page / final handover):** ask the human for the course name + faculty name. Until then the title page uses the placeholder `[COURSE NAME — FACULTY NAME]`. Default: leave placeholder and flag it in the final summary.

## Resolved
- G1 (2026-10-17): UI = Bloomberg-style terminal; deck = .pptx, built after local project; authors = Vedant Pathak R045, Dhairya Shah R057, Kanishak Thakran R046; AI-disclosure paragraph at very end of paper = yes. Course/faculty name not given (placeholder).

- RESOLVED 2026-10-18: faculty name = Dr. Sakshi Ranjan. STILL OPEN: course name (placeholder [COURSE NAME] in paper title block). REMIND at the end.
