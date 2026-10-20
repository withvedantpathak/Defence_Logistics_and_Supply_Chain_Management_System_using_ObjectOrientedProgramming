#!/usr/bin/env python3
"""Verification pass for paper/paper.md: every number in the result tables and every quoted statistic is recomputed from
results/ and must appear in the paper. Also checks figure references, placeholders and that the AI disclosure is last."""
import csv, math, re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
md = (ROOT / "paper/paper.md").read_text(encoding="utf-8")
stats = (ROOT / "results/stats.txt").read_text(encoding="utf-8")
rows = list(csv.DictReader((ROOT / "results/summary.csv").open()))
raw = list(csv.DictReader((ROOT / "results/raw/runs.csv").open()))
fails, checks = [], 0

def cell(sc, s, v, m):
    for r in rows:
        if r["scale"] == sc and r["source_strategy"] == s and r["vehicle_strategy"] == v:
            return float(r[m + "_mean"]), float(r[m + "_ci95"])
def need(label, text):
    global checks
    checks += 1
    if text not in md:
        fails.append(f"MISSING in paper: {label}: '{text}'")
def p_of(line_prefix, scale, metric, field="p_holm"):
    for l in stats.splitlines():
        if l.startswith(line_prefix) and f"| {scale} | {metric}:" in l:
            return float(re.search(field + r"=([0-9.e+-]+)", l).group(1)) if field == "p_holm" else None
    raise KeyError((line_prefix, scale, metric))
def diff_of(line_prefix, scale, metric):
    for l in stats.splitlines():
        if l.startswith(line_prefix) and f"| {scale} | {metric}:" in l:
            return float(re.search(r"diff=([+-][0-9.]+)", l).group(1))
    raise KeyError

# Table IV (lead time and distance by donor strategy, vehicle FASTEST)
for s in ["NEAREST", "SURPLUS", "BALANCED", "RANDOM"]:
    parts = []
    for m, fmt in [("avg_lead_h", lambda a, b: f"{a:.1f} ± {b:.1f}")]:
        for sc in ["small", "medium", "large"]:
            parts.append(fmt(*cell(sc, s, "FASTEST", m)))
    for sc in ["small", "medium", "large"]:
        a, b = cell(sc, s, "FASTEST", "total_km")
        parts.append(f"{a/1000:.1f} ± {b/1000:.1f}" if sc == "small" else f"{a/1000:,.0f} ± {b/1000:.0f}")
    for p in parts:
        need(f"Table IV {s}", p)
# Table V (vehicle strategies, donor NEAREST)
for v in ["SMALLEST", "FASTEST", "RANDOM"]:
    line = [f"{cell(sc,'NEAREST',v,'avg_lead_h')[0]:.1f}" for sc in ["small", "medium", "large"]]
    c24 = f"{cell('large','NEAREST',v,'critical_within_24h')[0]*100:.1f}%"
    ut = f"{cell('large','NEAREST',v,'utilisation')[0]:.3f}"
    need(f"Table V {v}", f"| {v} | " + " | ".join(line) + f" | {c24} | {ut} |")
# Table VI (coordinated vs random)
for sc in ["small", "medium", "large"]:
    for name, (s, v) in [("coordinated", ("NEAREST", "FASTEST")), ("random", ("RANDOM", "RANDOM"))]:
        km = cell(sc, s, v, "total_km")[0] / 1000
        kms = f"{km:.1f}" if sc == "small" else f"{km:,.0f}"
        need(f"Table VI {sc} {name}", f"| {sc}, {name} | {cell(sc,s,v,'avg_lead_h')[0]:.1f} | {cell(sc,s,v,'critical_within_24h')[0]*100:.1f}% | {kms} | {cell(sc,s,v,'fulfilment_rate')[0]*100:.1f}% |")
# fulfilment range
cm = [float(r["fulfilment_rate_mean"]) for r in rows]
need("fulfilment cell-mean range", f"between {min(cm)*100:.1f}% and {max(cm)*100:.1f}%")
fr = [float(r["fulfilment_rate"]) for r in raw]
need("fulfilment run range", f"{min(fr)*100:.1f}% and {max(fr)*100:g}%")
# percentages derived
def pct(a, b): return (a / b - 1) * 100
n, bal, sur, rnd = (cell("large", s, "FASTEST", "total_km")[0] for s in ["NEAREST", "BALANCED", "SURPLUS", "RANDOM"])
need("BALANCED +2.6% km", f"{pct(bal,n):.1f}%"); need("SURPLUS +41.9% km", f"{pct(sur,n):.1f}%"); need("RANDOM +64.8% km", f"{pct(rnd,n):.1f}%")
for sc, exp in zip(["small", "medium", "large"], ["8.8%", "7.0%", "4.9%"]):
    a = cell(sc, "NEAREST", "FASTEST", "total_km")[0]; b = cell(sc, "NEAREST", "SMALLEST", "total_km")[0]
    assert f"{pct(a,b):.1f}%" == exp, (sc, pct(a, b))
for sc, exp in zip(["small", "medium", "large"], ["35%", "38%", "42%"]):
    a = cell(sc, "NEAREST", "FASTEST", "total_km")[0]; b = cell(sc, "RANDOM", "RANDOM", "total_km")[0]
    assert f"{-pct(a,b):.0f}%" == exp, (sc, pct(a, b))
for sc, exp in zip(["small", "medium", "large"], ["47%", "56%", "59%"]):
    a = cell(sc, "NEAREST", "FASTEST", "avg_lead_h")[0]; b = cell(sc, "RANDOM", "RANDOM", "avg_lead_h")[0]
    assert f"{(1-a/b)*100:.0f}%" == exp, (sc, (1 - a / b) * 100)
# selected adjusted p-values and differences quoted in text
q = [("NEAREST vs BALANCED", "medium", "avg_lead_h", "0.012", 0.01165), ("NEAREST vs BALANCED", "small", "avg_lead_h", "0.76", 0.7627),
     ("NEAREST vs SURPLUS", "small", "avg_lead_h", "0.047", 0.04735), ("NEAREST vs BALANCED", "large", "total_km", "0.003", 0.00316),
     ("NEAREST vs BALANCED", "large", "fulfilment_rate", "0.001", 0.0009846), ("BALANCED vs SURPLUS", "large", "fulfilment_rate", "0.003", 0.003449)]
for pre, sc, m, txt, val in q:
    got = p_of(pre, sc, m)
    assert abs(got - val) < 1e-4, (pre, sc, m, got)
    need(f"p {pre} {sc} {m}", txt)
assert abs(diff_of("NEAREST vs BALANCED", "large", "avg_lead_h") - 6.333) < 1e-3
assert abs(diff_of("NEAREST vs BALANCED", "medium", "avg_lead_h") - 2.965) < 1e-3
need("test counts", "93 tests"); need("81 uncorrected", "81 are below 0.05 uncorrected"); need("74 corrected", "74 remain below 0.05")
assert "93 paired tests: 81 below 0.05 uncorrected, 74 below 0.05 after correction" in stats
# unfulfilled totals
for sc, exp in [("small", (320, 14400)), ("medium", (1211, 72000)), ("large", (7686, 360000))]:
    sel = [r for r in raw if r["scale"] == sc]
    unf = sum(int(r["requests"]) - int(r["fulfilled"]) for r in sel); tot = sum(int(r["requests"]) for r in sel)
    assert (unf, tot) == exp, (sc, unf, tot)
    need(f"unfulfilled {sc}", f"{unf:,} of {tot:,}")
# scalability
for sc, ms in [("small", 3), ("medium", 72), ("large", 912)]:
    assert f"{sc}: requests=" in stats and f"median wall={ms} ms" in stats, sc
need("scalability", "3 ms for 40 requests, 72 ms for 200 requests and 912 ms for 1,000 requests")
need("runs", "1,080 runs"); assert len(raw) == 1080
# figure and table references
for n in range(1, 10):
    if not re.search(rf"Figs?\.[^.]*\b{n}\b|Figs\. 6–9", md.split("## !APPENDIX C")[0] if n < 6 else md):
        fails.append(f"figure {n} never referenced in text")
for mt in ["I", "II", "III", "IV", "V", "VI", "VII"]:
    if not re.search(rf"Table {mt}\b", md): fails.append(f"Table {mt} not referenced")
# placeholders
for bad in ["TODO", "TBD", "XXX", "lorem", "FIXME"]:
    if bad.lower() in md.lower(): fails.append(f"placeholder text: {bad}")
if "[COURSE NAME]" in md: fails.append("course placeholder still present")
# AI disclosure must be the very last item
last_h = [l for l in md.splitlines() if l.startswith("## ")][-1]
if last_h != "## !AI-ASSISTANCE DISCLOSURE": fails.append("AI disclosure is not the last section")
tail = md.split("## !AI-ASSISTANCE DISCLOSURE")[1].strip().splitlines()
if len([t for t in tail if t.strip()]) != 1: fails.append("AI disclosure should be a single short paragraph")
# every [n] citation resolves
refs = set(re.findall(r"^\[(\d+)\] ", md, flags=re.M))
cited = set(re.findall(r"\[(\d+)\]", md.split("## !REFERENCES")[0]))
if cited - refs: fails.append(f"cited but not listed: {cited - refs}")
if refs - cited: fails.append(f"listed but never cited: {refs - cited}")
print(f"{checks} string checks + derived assertions run")
if fails:
    print("FAILURES:"); [print(" -", f) for f in fails]; sys.exit(1)
print("ALL PAPER CHECKS PASSED")
