# P2 — Dataset (synthetic) (≈1.5 h)
**Goal:** reproducible datasets for the demo, the tests, and the experiments; fully documented for the paper.
**Needs human:** G3 only if they want a real public dataset.

## Steps
- [x] 2.1 Write `scripts/gen_data.py` (stdlib + random.Random(seed)). Seed default 42. Outputs to `data/`:
  - `bases.csv` — 8–12 bases at real public city coordinates (Pune, Mumbai, Delhi, Jaipur, Leh, Guwahati, Chennai, Kolkata, Bengaluru, Nagpur…). Fields: id,name,lat,lon,capacity_units.
  - `supplies.csv` — catalogue: id,name,category,unit_weight_kg. (Medical kits, rations, fuel drums, spare parts, tents.) No munitions/weapons.
  - `inventory.csv` — base_id,supply_id,qty,reorder_level. Deliberately create shortages at some bases and surplus at others.
  - `vehicles.csv` — id,type,home_base,capacity_kg,speed_kmph,available.
  - `requests_scenarios.csv` — N requests over simulated days for experiments (id,base,supply,qty,priority,day).
- [x] 2.2 Scale variants: `small` (demo), `medium`, `large` (stress/sim) via CLI flag.
- [x] 2.3 Document in `docs/dataset.md`: schema tables, generation rules, parameters, seed, limitations, "no real operational data" statement. Cite public sources ONLY if fetched & verified (e.g., city coordinates from a public gazetteer; vehicle capacity ranges from manufacturer public specs).
- [x] 2.4 Write `scripts/validate_data.py`: referential integrity, non-negative qty, supply/demand imbalance summary → `results/dataset_summary.csv`.

## Exit checks
`python3 scripts/gen_data.py --scale small && python3 scripts/validate_data.py` exit 0 · same seed ⇒ identical files (hash check) · `docs/dataset.md` complete.

**Result (2026-10-17):** data/{small,medium,large} generated (seed 42); validator OK on all three; reproducibility verified by SHA-256; validator negative-tested. See docs/dataset.md.
