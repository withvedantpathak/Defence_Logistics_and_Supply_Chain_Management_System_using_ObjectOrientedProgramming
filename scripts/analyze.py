#!/usr/bin/env python3
"""Aggregates results/raw/runs.csv -> results/summary.csv, results/stats.txt and figures in paper/figures/.
Every number quoted in docs/findings.md comes from these outputs."""
import csv
import math
from collections import defaultdict
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "results/raw/runs.csv"
FIG = ROOT / "paper/figures"
FIG.mkdir(parents=True, exist_ok=True)
SCALES = ["small", "medium", "large"]
SRC = ["NEAREST", "SURPLUS", "BALANCED", "RANDOM"]
VEH = ["SMALLEST", "FASTEST", "RANDOM"]
METRICS = ["fulfilment_rate", "stock_limited", "vehicle_limited", "in_flight", "unit_fill_rate", "avg_lead_h", "avg_lead_critical_h", "critical_within_24h",
           "utilisation", "deliveries", "total_km", "wall_ms", "used_mb"]

rows = []
with RAW.open(encoding="utf-8") as f:
    for r in csv.DictReader(f):
        d = dict(r)
        d["seed"] = int(r["seed"])
        for m in METRICS + ["requests", "fulfilled", "end_hour"]:
            d[m] = float(r[m]) if r[m] != "" else float("nan")
        rows.append(d)

def cell(scale, s, v):
    return sorted([r for r in rows if r["scale"] == scale and r["source_strategy"] == s and r["vehicle_strategy"] == v],
                  key=lambda r: r["seed"])

def mean_ci(x):
    x = np.array([v for v in x if not math.isnan(v)])
    if len(x) == 0:
        return float("nan"), float("nan"), float("nan"), 0
    m = x.mean()
    sd = x.std(ddof=1) if len(x) > 1 else 0.0
    h = stats.t.ppf(0.975, len(x) - 1) * sd / math.sqrt(len(x)) if len(x) > 1 else 0.0
    return m, sd, h, len(x)

# ---- summary.csv ----
with (ROOT / "results/summary.csv").open("w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["scale", "source_strategy", "vehicle_strategy", "n_seeds"] +
               [f"{m}_{k}" for m in METRICS for k in ("mean", "sd", "ci95")])
    for sc in SCALES:
        for s in SRC:
            for v in VEH:
                c = cell(sc, s, v)
                line = [sc, s, v, len(c)]
                for m in METRICS:
                    mu, sd, h, n = mean_ci([r[m] for r in c])
                    line += [f"{mu:.4f}", f"{sd:.4f}", f"{h:.4f}"]
                w.writerow(line)

def holm(pvals):
    """Holm-Bonferroni adjusted p-values (NaN-safe), same order as input."""
    idx = [i for i, p in enumerate(pvals) if not math.isnan(p)]
    order = sorted(idx, key=lambda i: pvals[i])
    adj = [float("nan")] * len(pvals)
    m = len(order)
    run = 0.0
    for rank, i in enumerate(order):
        run = max(run, min(1.0, (m - rank) * pvals[i]))
        adj[i] = run
    return adj

# ---- paired tests (same scale+seed+vehicle strategy, different donor strategy; and vice versa) ----
out = []
tests = []   # (index in out, p) for the Holm family
def paired(label, a, b, metric, scale):
    xa = [r[metric] for r in a]
    xb = [r[metric] for r in b]
    pairs = [(p, q) for p, q in zip(xa, xb) if not (math.isnan(p) or math.isnan(q))]
    if len(pairs) < 3:
        return
    xa, xb = zip(*pairs)
    diff = np.array(xa) - np.array(xb)
    if np.allclose(diff, 0):
        out.append(f"{label} | {scale} | {metric}: identical in all {len(diff)} pairs")
        return
    try:
        p = stats.wilcoxon(xa, xb).pvalue
    except ValueError:
        p = float("nan")
    tests.append((len(out), p))
    out.append(f"{label} | {scale} | {metric}: mean A={np.mean(xa):.3f} B={np.mean(xb):.3f} diff={diff.mean():+.3f} "
               f"Wilcoxon p={p:.4g} n={len(diff)}")

out.append("# Paired comparisons (Wilcoxon signed-rank, two-sided, paired by seed; vehicle strategy FASTEST for donor tests)")
for sc in SCALES:
    for metric in ["avg_lead_h", "avg_lead_critical_h", "fulfilment_rate", "total_km"]:
        for a, b in [("NEAREST", "SURPLUS"), ("NEAREST", "BALANCED"), ("NEAREST", "RANDOM"), ("BALANCED", "SURPLUS")]:
            paired(f"{a} vs {b}", cell(sc, a, "FASTEST"), cell(sc, b, "FASTEST"), metric, sc)
out.append("")
out.append("# Vehicle strategy comparisons (donor strategy NEAREST)")
for sc in SCALES:
    for metric in ["avg_lead_h", "avg_lead_critical_h", "critical_within_24h", "utilisation", "total_km"]:
        for a, b in [("FASTEST", "SMALLEST"), ("FASTEST", "RANDOM")]:
            paired(f"{a} vs {b}", cell(sc, "NEAREST", a), cell(sc, "NEAREST", b), metric, sc)
out.append("")
out.append("# Coordinated (NEAREST+FASTEST) versus uncoordinated baseline (RANDOM+RANDOM)")
for sc in SCALES:
    for metric in ["avg_lead_h", "fulfilment_rate", "unit_fill_rate", "total_km", "critical_within_24h"]:
        paired("NEAREST+FASTEST vs RANDOM+RANDOM", cell(sc, "NEAREST", "FASTEST"), cell(sc, "RANDOM", "RANDOM"), metric, sc)
out.append("")
# Holm correction over the whole family of paired tests above
_adj = holm([p for _, p in tests])
for (i, p), q in zip(tests, _adj):
    out[i] += f" p_holm={q:.4g}"
n_tests = len(tests)
n_sig_raw = sum(1 for _, p in tests if p < 0.05)
n_sig_holm = sum(1 for q in _adj if q < 0.05)
out.append(f"# Holm-Bonferroni over {n_tests} paired tests: {n_sig_raw} below 0.05 uncorrected, {n_sig_holm} below 0.05 after correction")
out.append("")
out.append("# Why requests stay unfulfilled (all runs; reasons are exclusive and sum to the unfulfilled total)")
for sc in SCALES:
    for label, cond in [("all strategies", lambda r: True),
                        ("coordinated (non-RANDOM donor, non-RANDOM vehicle)", lambda r: r["source_strategy"] != "RANDOM" and r["vehicle_strategy"] != "RANDOM"),
                        ("baseline (RANDOM+RANDOM)", lambda r: r["source_strategy"] == "RANDOM" and r["vehicle_strategy"] == "RANDOM")]:
        sel = [r for r in rows if r["scale"] == sc and cond(r)]
        unf = sum(r["requests"] - r["fulfilled"] for r in sel)
        parts = {k: sum(r[k] for r in sel) for k in ("stock_limited", "vehicle_limited", "in_flight")}
        tot_req = sum(r["requests"] for r in sel)
        pct = lambda v: (v / unf * 100 if unf else float("nan"))
        out.append(f"{sc} | {label}: runs={len(sel)} requests={tot_req:.0f} unfulfilled={unf:.0f} ({unf / tot_req * 100:.2f}%) "
                   f"stock-limited={parts['stock_limited']:.0f} ({pct(parts['stock_limited']):.1f}%) "
                   f"waiting-for-vehicle={parts['vehicle_limited']:.0f} ({pct(parts['vehicle_limited']):.1f}%) "
                   f"in-flight-at-horizon={parts['in_flight']:.0f} ({pct(parts['in_flight']):.1f}%) "
                   f"unexplained={unf - sum(parts.values()):.0f}")
out.append("")
out.append("# Scalability (median over all 12 strategy pairs x seeds per scale)")
for sc in SCALES:
    sel = [r for r in rows if r["scale"] == sc]
    wall = np.median([r["wall_ms"] for r in sel])
    req = np.median([r["requests"] for r in sel])
    mem = np.median([r["used_mb"] for r in sel])
    out.append(f"{sc}: requests={req:.0f} median wall={wall:.0f} ms -> {req / (wall / 1000):.0f} requests/s; "
               f"median heap in use={mem:.0f} MB; simulated hours={np.median([r['end_hour'] for r in sel]):.0f}")
out.append("")
out.append(f"runs={len(rows)}  seeds={len(set(r['seed'] for r in rows))}")
(ROOT / "results/stats.txt").write_text("\n".join(out) + "\n", encoding="utf-8")

# ---- figures (grey-safe: distinct hatches + markers, direct labels) ----
plt.rcParams.update({"font.size": 9, "axes.spines.top": False, "axes.spines.right": False, "figure.dpi": 100})
HATCH = ["", "//", "xx", ".."]
COLORS = ["#1b6ca8", "#d98c1f", "#3f9d6b", "#8a8a8a"]

def grouped(metric, ylabel, fname, title, veh="FASTEST", scales=SCALES, ylim=None):
    fig, ax = plt.subplots(figsize=(6.4, 3.4))
    width = 0.2
    for i, s in enumerate(SRC):
        means, errs = [], []
        for sc in scales:
            mu, sd, h, n = mean_ci([r[metric] for r in cell(sc, s, veh)])
            means.append(mu)
            errs.append(h)
        ax.bar(np.arange(len(scales)) + (i - 1.5) * width, means, width, yerr=errs, capsize=2, label=s,
               color=COLORS[i], edgecolor="black", hatch=HATCH[i], linewidth=0.6)
    ax.set_xticks(range(len(scales)))
    ax.set_xticklabels(scales)
    ax.set_ylabel(ylabel)
    ax.set_title(title, fontsize=10)
    if ylim:
        ax.set_ylim(*ylim)
    ax.legend(title="donor strategy", ncol=4, fontsize=8, frameon=False, loc="upper left")
    fig.tight_layout()
    fig.savefig(FIG / fname, dpi=300)
    plt.close(fig)

grouped("avg_lead_h", "mean lead time (hours)", "fig_lead_time_by_source.png",
        "Lead time by donor strategy (vehicle: FASTEST; mean, 95% CI over seeds)")
grouped("fulfilment_rate", "requests fully delivered (share)", "fig_fulfilment_by_source.png",
        "Fulfilment rate by donor strategy (vehicle: FASTEST)", ylim=(0.5, 1.05))
grouped("total_km", "total distance of delivered trips (km)", "fig_distance_by_source.png",
        "Transport effort by donor strategy (vehicle: FASTEST)")

# vehicle strategy: lead time + utilisation
fig, axes = plt.subplots(1, 2, figsize=(6.8, 3.2))
for ax, metric, yl in [(axes[0], "avg_lead_critical_h", "mean lead time, CRITICAL (h)"),
                       (axes[1], "utilisation", "fleet utilisation (share of hours)")]:
    width = 0.25
    for i, v in enumerate(VEH):
        means, errs = [], []
        for sc in SCALES:
            mu, sd, h, n = mean_ci([r[metric] for r in cell(sc, "NEAREST", v)])
            means.append(mu)
            errs.append(h)
        ax.bar(np.arange(3) + (i - 1) * width, means, width, yerr=errs, capsize=2, label=v, color=COLORS[i],
               edgecolor="black", hatch=HATCH[i], linewidth=0.6)
    ax.set_xticks(range(3))
    ax.set_xticklabels(SCALES)
    ax.set_ylabel(yl)
h, l = axes[0].get_legend_handles_labels()
fig.legend(h, l, title="vehicle strategy", ncol=3, fontsize=8, frameon=False, loc="lower center")
fig.suptitle("Vehicle assignment strategy (donor: NEAREST; mean, 95% CI over seeds)", fontsize=10)
fig.tight_layout(rect=(0, 0.12, 1, 1))
fig.savefig(FIG / "fig_vehicle_strategy.png", dpi=300)
plt.close(fig)

# coordination vs baseline
fig, ax = plt.subplots(figsize=(6.4, 3.2))
labels = ["NEAREST+FASTEST", "RANDOM+RANDOM"]
for i, (s, v) in enumerate([("NEAREST", "FASTEST"), ("RANDOM", "RANDOM")]):
    means, errs = [], []
    for sc in SCALES:
        mu, sd, h, n = mean_ci([r["avg_lead_h"] for r in cell(sc, s, v)])
        means.append(mu)
        errs.append(h)
    ax.bar(np.arange(3) + (i - 0.5) * 0.35, means, 0.35, yerr=errs, capsize=2, label=labels[i], color=COLORS[i * 3],
           edgecolor="black", hatch=HATCH[i * 2], linewidth=0.6)
ax.set_xticks(range(3))
ax.set_xticklabels(SCALES)
ax.set_ylabel("mean lead time (hours)")
ax.set_title("Coordinated strategies versus uncoordinated random baseline", fontsize=10)
ax.legend(frameon=False, fontsize=8)
fig.tight_layout()
fig.savefig(FIG / "fig_baseline_comparison.png", dpi=300)
plt.close(fig)

# scalability
fig, ax = plt.subplots(figsize=(5.2, 3.2))
xs = [np.median([r["requests"] for r in rows if r["scale"] == sc]) for sc in SCALES]
ys = [np.median([r["wall_ms"] for r in rows if r["scale"] == sc]) / 1000 for sc in SCALES]
ax.plot(xs, ys, "ko-", markersize=6)
for x, y, sc in zip(xs, ys, SCALES):
    ax.annotate(sc, (x, y), textcoords="offset points", xytext=(6, -10), fontsize=8)
ax.set_ylim(bottom=-0.08)
ax.set_xlabel("requests in scenario")
ax.set_ylabel("simulation wall time (s, median)")
ax.set_title("Scalability: whole scenario replay", fontsize=10)
fig.tight_layout()
fig.savefig(FIG / "fig_scalability.png", dpi=300)
plt.close(fig)
print("wrote results/summary.csv, results/stats.txt and", len(list(FIG.glob('fig_*.png'))), "figures")
