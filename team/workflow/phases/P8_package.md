# P8 — Packaging & final QA (≈1 h)
**Needs human:** G2 (JDK on their PC), G5 (final review).

## Steps
- [ ] 8.1 `README.md`: what it is, requirements (JDK 17+), build, run, demo, tests, repo map, data note.
- [ ] 8.2 Clean-room test: fresh clone → `scripts/build.sh test` → `java -jar dist/dlscms.jar --demo`. Fix anything that fails.
- [ ] 8.3 Windows helpers: `run.bat` (demo + menu), `build.bat`.
- [ ] 8.3b User manual: finalize `docs/user_manual.md` with real screenshots (render `--plain` or colour terminal output to PNG), build `User_Manual.pdf` (pandoc → docx → LibreOffice PDF), run `scripts/verify_manual.sh`.
- [ ] 8.4 Assemble `submission/`: `dlscms.jar`, `dlscms-source.zip`, `paper.pdf`, `paper.docx`, `DLSCMS.pptx`, `User_Manual.pdf`, UML diagrams (`docs/img/class_*.png`), `README.md`, `demo_script.md`.
- [ ] 8.5 Final traceability matrix review: every cell filled.
- [ ] 8.6 Sync everything to the device folder; list final tree to the human.
- [ ] 8.7 Ask G2/G5 questions (batched). Produce 5-line handover summary.

## Exit checks
`submission/` complete · STATE.md all DONE · Definition of Done in CLAUDE.md fully ticked.
