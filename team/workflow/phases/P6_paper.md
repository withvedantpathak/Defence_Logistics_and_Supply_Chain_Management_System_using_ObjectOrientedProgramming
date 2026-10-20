# P6 — Research paper (≈3 h)
**Goal:** a complete IEEE-style paper (6–10 pages) from `workflow/templates/paper_outline.md`.
**Needs human:** G6 must be resolved ("local model accepted") BEFORE starting. Authors: Vedant Pathak (R045), Dhairya Shah (R057), Kanishak Thakran (R046); course/faculty = placeholder field.

## Steps
- [ ] 6.1 Research for related work: search for 8–15 REAL sources (logistics/supply-chain information systems, OOP design patterns for logistics, inventory-routing heuristics, military/humanitarian logistics software). Fetch each page to verify title/authors/year/venue before adding to `paper/refs.bib`. Drop anything unverifiable. Use Gamma et al. (Design Patterns) only if verified.
- [ ] 6.2 Draft in Markdown `paper/paper.md` following the outline; pull tables/figures only from `results/` and `paper/figures/`.
- [ ] 6.3 Sections: Abstract · Introduction · Related Work · System Requirements · Design (class diagram, patterns, OOP mapping table) · Dataset (from `docs/dataset.md`) · Implementation · Testing · Experiments & Results · Discussion/Limitations/Threats to validity · Conclusion & Future Work · References · Appendix (traceability matrix, how to run).
- [ ] 6.4 Build: `pandoc paper/paper.md --citeproc --bibliography paper/refs.bib -o paper/paper.docx` (+ reference docx styled two-column if feasible) and PDF via LibreOffice (`soffice --headless --convert-to pdf`).
- [ ] 6.5 Verification pass: (a) every number ↔ results file, (b) every reference resolvable, (c) every figure referenced in text, (d) no placeholder text, (e) short AI-assistance disclosure paragraph is the VERY LAST item of the paper, after references and appendices (human-approved).
- [ ] 6.6 Render the PDF to images and eyeball pages 1, middle, last for layout problems.

## Exit checks
`paper/paper.pdf` and `.docx` exist · verification checklist (6.5) all ticked in STATE.md.
