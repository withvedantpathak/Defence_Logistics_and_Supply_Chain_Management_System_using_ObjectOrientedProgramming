# P5 — Experiments & findings (≈2 h)
**Goal:** generate real, reproducible findings for the paper (the "results" section).
**Needs human:** none.

## Research questions
- RQ1: Which source-selection strategy (NearestFirst / MostSurplus / Balanced) minimises mean delivery lead time and unfulfilled requests?
- RQ2: Does vehicle-assignment strategy (SmallestFit vs FastestFit) change fleet utilisation and priority-request lateness?
- RQ3: How does the system scale (requests/second, memory) from small → large datasets?
- RQ4 (optional): Does centralised coordination beat a "no-coordination" baseline (each base only uses its own stock/fleet) on shortage days?

## Steps
- [ ] 5.1 Implement `sim` package: discrete-day simulator driven by `requests_scenarios.csv`; headless entry `--sim --scale X --seed N --strategy ...`; emits CSV rows per run.
- [ ] 5.2 `scripts/run_experiments.sh`: full grid (3×2 strategies × 3 scales × 10 seeds) → `results/raw/*.csv`. Must finish in <15 min on the container.
- [ ] 5.3 `scripts/analyze.py`: aggregate (mean, std, 95% CI), simple paired comparison (e.g. Wilcoxon via stdlib-free approach or scipy if available) → `results/summary.csv`, `results/stats.txt`.
- [ ] 5.4 Figures (matplotlib, readable in B/W, labelled axes, 300 dpi) → `paper/figures/`: lead-time by strategy, fulfilment rate, fleet utilisation, scalability curve.
- [ ] 5.5 Write `docs/findings.md`: 5–8 bullet findings, each citing the exact results file/figure. Include negative/null results honestly and threats to validity (synthetic data, simplified travel model).

## Exit checks
`bash scripts/run_experiments.sh` reproducible (re-run diff = identical for same seeds) · figures exist · every finding in `docs/findings.md` cites a file.
