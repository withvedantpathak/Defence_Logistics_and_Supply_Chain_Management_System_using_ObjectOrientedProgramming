#!/usr/bin/env python3
"""Validate generated DLSCMS datasets and write results/dataset_summary.csv.

Checks: required files/headers, unique ids, referential integrity, non-negative quantities,
storage capacity, valid enums, network feasibility (surplus vs shortage per supply),
demo-scenario preconditions. Exit code 0 = all datasets valid.

Usage: python3 scripts/validate_data.py [--data data] [--scales small,medium,large] [--out results/dataset_summary.csv]
"""
import argparse
import csv
import os
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(__file__))
from gen_data import HEADERS  # noqa: E402  (single source of truth for headers)

CATEGORIES = {"MEDICAL", "FOOD", "FUEL", "SPARES", "SHELTER"}
VEHICLE_TYPES = {"Truck", "FuelTruck", "MedicalVan", "Helicopter"}
PRIORITIES = {"ROUTINE", "URGENT", "CRITICAL"}


def read(d, name):
    with open(os.path.join(d, name), newline="", encoding="utf-8") as f:
        r = csv.reader(f)
        header = next(r, None)
        rows = [row for row in r]
    return header, rows


def validate(d):
    errors = []
    tables = {}
    for name in list(HEADERS) + ["meta.csv"]:
        p = os.path.join(d, name)
        if not os.path.exists(p):
            errors.append(f"missing file {name}")
            continue
        header, rows = read(d, name)
        if name in HEADERS and header != HEADERS[name]:
            errors.append(f"{name}: header {header} != expected {HEADERS[name]}")
        for i, row in enumerate(rows, start=2):
            if name in HEADERS and len(row) != len(HEADERS[name]):
                errors.append(f"{name} line {i}: {len(row)} columns, expected {len(HEADERS[name])}")
        tables[name] = rows
    if errors:
        return errors, None

    bases = {r[0]: r for r in tables["bases.csv"]}
    supplies = {r[0]: r for r in tables["supplies.csv"]}
    for name, key_idx in [("bases.csv", 0), ("supplies.csv", 0), ("vehicles.csv", 0)]:
        ids = [r[key_idx] for r in tables[name]]
        if len(ids) != len(set(ids)):
            errors.append(f"{name}: duplicate ids")
    for r in tables["bases.csv"]:
        lat, lon = float(r[3]), float(r[4])
        if not (6 <= lat <= 38 and 68 <= lon <= 98):
            errors.append(f"base {r[0]}: coordinates ({lat},{lon}) outside India bounding box")
        if int(r[5]) <= 0:
            errors.append(f"base {r[0]}: non-positive capacity")
    for r in tables["supplies.csv"]:
        if r[2] not in CATEGORIES:
            errors.append(f"supply {r[0]}: bad category {r[2]}")
        if float(r[3]) <= 0:
            errors.append(f"supply {r[0]}: non-positive weight")

    inv = {}
    used = Counter()
    for r in tables["inventory.csv"]:
        b, s, qty, res, reorder = r[0], r[1], int(r[2]), int(r[3]), int(r[4])
        if b not in bases:
            errors.append(f"inventory: unknown base {b}")
        if s not in supplies:
            errors.append(f"inventory: unknown supply {s}")
        if qty < 0 or res < 0 or reorder <= 0 or res > qty:
            errors.append(f"inventory {b}/{s}: invalid qty/reserved/reorder ({qty},{res},{reorder})")
        if (b, s) in inv:
            errors.append(f"inventory: duplicate row {b}/{s}")
        inv[(b, s)] = (qty, reorder)
        used[b] += qty
    if len(inv) != len(bases) * len(supplies):
        errors.append(f"inventory rows {len(inv)} != bases x supplies {len(bases) * len(supplies)}")
    for b, r in bases.items():
        if used[b] > int(r[5]):
            errors.append(f"base {b}: stock {used[b]} exceeds capacity {r[5]}")

    vtypes = Counter()
    for r in tables["vehicles.csv"]:
        vtypes[r[1]] += 1
        if r[1] not in VEHICLE_TYPES:
            errors.append(f"vehicle {r[0]}: bad type {r[1]}")
        if r[2] not in bases or r[3] not in bases:
            errors.append(f"vehicle {r[0]}: unknown base")
        if int(r[4]) <= 0 or int(r[5]) <= 0:
            errors.append(f"vehicle {r[0]}: non-positive capacity/speed")
        if r[6] != "AVAILABLE":
            errors.append(f"vehicle {r[0]}: initial status must be AVAILABLE")
    for r in tables["scenario_requests.csv"]:
        if r[1] not in bases or r[2] not in supplies:
            errors.append(f"scenario {r[0]}: unknown base/supply")
        if int(r[3]) <= 0 or r[4] not in PRIORITIES:
            errors.append(f"scenario {r[0]}: bad qty/priority")
    ids = [r[0] for r in tables["scenario_requests.csv"]]
    if len(ids) != len(set(ids)):
        errors.append("scenario_requests.csv: duplicate ids")

    # network balance
    short_cells = sum(1 for (q, r) in inv.values() if q < r)
    surplus_cells = sum(1 for (q, r) in inv.values() if q > r)
    tot_short = sum(max(0, r - q) for (q, r) in inv.values())
    tot_surplus = sum(max(0, q - r) for (q, r) in inv.values())
    coverage = []
    for s in supplies:
        sh = sum(max(0, inv[(b, s)][1] - inv[(b, s)][0]) for b in bases)
        su = sum(max(0, inv[(b, s)][0] - inv[(b, s)][1]) for b in bases)
        coverage.append(su / sh if sh else float("inf"))
        if su < sh:
            errors.append(f"supply {s}: network surplus {su} < shortage {sh}")
    # demo preconditions
    if "PUN" in bases:
        q, r = inv[("PUN", "MED-KIT")]
        if not q < r:
            errors.append("demo: Pune must be short of MED-KIT")
        if not any(inv[(b, "MED-KIT")][0] > inv[(b, "MED-KIT")][1] for b in bases if b != "PUN"):
            errors.append("demo: no other base has spare MED-KIT")
        if not any(v[1] == "MedicalVan" for v in tables["vehicles.csv"]):
            errors.append("demo: no MedicalVan in fleet")
    prios = Counter(r[4] for r in tables["scenario_requests.csv"])
    days = max((int(r[5]) for r in tables["scenario_requests.csv"]), default=-1) + 1
    summary = {
        "bases": len(bases), "supplies": len(supplies), "inventory_cells": len(inv),
        "shortage_cells": short_cells, "surplus_cells": surplus_cells,
        "total_shortage_units": tot_short, "total_surplus_units": tot_surplus,
        "min_supply_coverage": round(min(coverage), 2),
        "vehicles": len(tables["vehicles.csv"]),
        "trucks": vtypes["Truck"], "fuel_trucks": vtypes["FuelTruck"],
        "medical_vans": vtypes["MedicalVan"], "helicopters": vtypes["Helicopter"],
        "scenario_requests": len(tables["scenario_requests.csv"]), "scenario_days": days,
        "routine": prios["ROUTINE"], "urgent": prios["URGENT"], "critical": prios["CRITICAL"],
    }
    return errors, summary


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--scales", default="small,medium,large")
    ap.add_argument("--out", default="results/dataset_summary.csv")
    a = ap.parse_args()
    rows, failed = [], False
    for sc in a.scales.split(","):
        errs, summ = validate(os.path.join(a.data, sc))
        if errs:
            failed = True
            print(f"[{sc}] INVALID ({len(errs)} problems)")
            for e in errs[:20]:
                print("   -", e)
        else:
            print(f"[{sc}] OK  bases={summ['bases']} vehicles={summ['vehicles']} "
                  f"shortage_cells={summ['shortage_cells']} min_coverage={summ['min_supply_coverage']}")
            rows.append({"scale": sc, **summ})
    if rows:
        os.makedirs(os.path.dirname(a.out) or ".", exist_ok=True)
        with open(a.out, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0]), lineterminator="\n")
            w.writeheader()
            w.writerows(rows)
        print("wrote", a.out)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
