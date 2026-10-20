#!/usr/bin/env python3
"""Summarises results/unfulfilled_diagnosis*.csv -> results/unfulfilled_summary.txt (numbers quoted in docs/findings.md)."""
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
out = []
for label, fn in [("end of run as simulated", "unfulfilled_diagnosis.csv"),
                  ("after one extra planning pass at the horizon end", "unfulfilled_diagnosis_replan.csv")]:
    rows = [r for r in csv.DictReader((ROOT / "results" / fn).open(encoding="utf-8")) if r["category"] != "FLEET"]
    out.append(f"# {label}  ({fn}; configurations NEAREST+FASTEST and RANDOM+RANDOM, 30 seeds each)")
    for sc in ["small", "medium", "large"]:
        for cfg in [("NEAREST", "FASTEST"), ("RANDOM", "RANDOM")]:
            sel = [r for r in rows if r["scale"] == sc and (r["src"], r["veh"]) == cfg]
            by = defaultdict(int)
            fuel = defaultdict(int)
            for r in sel:
                by[r["reason"]] += int(r["count"])
                if r["category"] == "FUEL":
                    fuel[r["reason"]] += int(r["count"])
            tot = sum(by.values())
            parts = ", ".join(f"{k}={v} (FUEL {fuel[k]})" for k, v in sorted(by.items()))
            out.append(f"{sc} | {cfg[0]}+{cfg[1]}: unfulfilled={tot}: {parts}")
    out.append("")
(ROOT / "results/unfulfilled_summary.txt").write_text("\n".join(out), encoding="utf-8")
print("\n".join(out))
