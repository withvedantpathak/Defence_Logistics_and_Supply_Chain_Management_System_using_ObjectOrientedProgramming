#!/usr/bin/env python3
"""Plain-English checker for an organisation's own DLSCMS data folder (bring-your-own-data).

Unlike validate_data.py (which checks the generated research datasets), this lists EVERY problem it finds in one
pass, so you can fix your CSV files before loading them.   Usage:  python3 scripts/check_my_data.py <folder>
Exit code 0 = no errors (warnings are advice only).
"""
import csv
import os
import sys

HEADERS = {
    "bases.csv": "id,name,city,lat,lon,capacity_units",
    "supplies.csv": "id,name,category,unit_weight_kg",
    "inventory.csv": "base_id,supply_id,qty,reserved,reorder_level",
    "vehicles.csv": "id,type,home_base,current_base,capacity_kg,speed_kmph,status",
}
OPTIONAL = {
    "requests.csv": "id,requester_base,supply,qty,fulfilled_qty,priority,status,created_hour",
    "deliveries.csv": "id,request,source,dest,supply,qty,vehicle,status,planned_hour,dispatched_hour,eta_hour,delivered_hour,distance_km,failure_reason",
    "scenario_requests.csv": "id,requester_base,supply,qty,priority,day,hour",
}
CATEGORIES = {"MEDICAL", "FOOD", "FUEL", "SPARES", "SHELTER"}
VTYPES = {"Truck", "FuelTruck", "MedicalVan", "Helicopter"}
CARRIES = {"Truck": CATEGORIES - {"FUEL"}, "FuelTruck": {"FUEL"}, "MedicalVan": {"MEDICAL", "FOOD"},
           "Helicopter": CATEGORIES - {"FUEL"}}


def main(folder):
    errors, warns = [], []

    def err(f, line, msg):
        errors.append(f"{f}" + (f" line {line}" if line else "") + f": {msg}")

    def num(f, line, col, v, kind=float, minimum=None):
        try:
            x = kind(v)
        except ValueError:
            err(f, line, f"'{col}' must be a {'whole number' if kind is int else 'number'}, got '{v}'")
            return None
        if minimum is not None and x < minimum:
            err(f, line, f"'{col}' must be at least {minimum}, got {v}")
        return x

    tables = {}
    for name, header in {**HEADERS, **OPTIONAL}.items():
        p = os.path.join(folder, name)
        if not os.path.exists(p):
            (err if name in HEADERS else warns.append)(*([name, None, "file is missing"] if name in HEADERS else
                                                          [f"{name}: file is missing (optional; the app creates it)"]))
            continue
        with open(p, newline="", encoding="utf-8-sig") as fh:
            rows = list(csv.reader(fh))
        if not rows or ",".join(h.strip() for h in rows[0]) != header:
            err(name, 1, f"first line must be exactly: {header}")
            continue
        body = [(i, r) for i, r in enumerate(rows[1:], start=2) if any(c.strip() for c in r)]
        for i, r in body:
            if len(r) != len(header.split(",")):
                err(name, i, f"expected {len(header.split(','))} columns, found {len(r)}")
        tables[name] = [(i, [c.strip() for c in r]) for i, r in body if len(r) == len(header.split(","))]
    if not os.path.exists(os.path.join(folder, "meta.csv")):
        warns.append("meta.csv: file is missing (optional; defaults to clock 0 and request id 1)")

    def ids(name):
        seen, out = {}, set()
        for i, r in tables.get(name, []):
            if r[0] in seen:
                err(name, i, f"duplicate id '{r[0]}' (first used on line {seen[r[0]]})")
            seen[r[0]] = i
            out.add(r[0])
        return out

    bases, supplies, vehicles = ids("bases.csv"), ids("supplies.csv"), ids("vehicles.csv")
    cat = {}
    for i, r in tables.get("bases.csv", []):
        for c, lo, hi in [(3, -90, 90), (4, -180, 180)]:
            v = num("bases.csv", i, HEADERS["bases.csv"].split(",")[c], r[c])
            if v is not None and not lo <= v <= hi:
                err("bases.csv", i, f"{HEADERS['bases.csv'].split(',')[c]} {v} is out of range")
        num("bases.csv", i, "capacity_units", r[5], int, 1)
    for i, r in tables.get("supplies.csv", []):
        if r[2] not in CATEGORIES:
            err("supplies.csv", i, f"category '{r[2]}' is not one of {sorted(CATEGORIES)} (map your product groups to these)")
        cat[r[0]] = r[2]
        num("supplies.csv", i, "unit_weight_kg", r[3], float, 0.001)
    cells, used = set(), {}
    for i, r in tables.get("inventory.csv", []):
        if r[0] not in bases:
            err("inventory.csv", i, f"base_id '{r[0]}' is not in bases.csv")
        if r[1] not in supplies:
            err("inventory.csv", i, f"supply_id '{r[1]}' is not in supplies.csv")
        q = num("inventory.csv", i, "qty", r[2], int, 0)
        rs = num("inventory.csv", i, "reserved", r[3], int, 0)
        ro = num("inventory.csv", i, "reorder_level", r[4], int, 0)
        if q is not None and rs is not None and rs > q:
            err("inventory.csv", i, "reserved cannot exceed qty (use 0 when starting)")
        if rs:
            warns.append(f"inventory.csv line {i}: reserved is {rs}; start with 0 unless you are resuming a saved session")
        if (r[0], r[1]) in cells:
            err("inventory.csv", i, f"duplicate row for base '{r[0]}' and supply '{r[1]}'")
        cells.add((r[0], r[1]))
        if q is not None:
            used[r[0]] = used.get(r[0], 0) + q
    for i, r in tables.get("bases.csv", []):
        cap = num("bases.csv", i, "capacity_units", r[5], int)
        if cap and used.get(r[0], 0) > cap:
            err("bases.csv", i, f"base '{r[0]}' holds {used[r[0]]} units in inventory.csv but capacity_units is {cap}")
    for b in bases:
        for s in supplies:
            if (b, s) not in cells:
                warns.append(f"inventory.csv: no row for base '{b}' / supply '{s}' (allowed; add a row with qty 0 and a "
                             f"reorder_level if you want shortage alerts for it)")
    fleet_types = set()
    for i, r in tables.get("vehicles.csv", []):
        if r[1] not in VTYPES:
            err("vehicles.csv", i, f"type '{r[1]}' must be one of {sorted(VTYPES)}")
        else:
            fleet_types.add(r[1])
        for c in (2, 3):
            if r[c] not in bases:
                err("vehicles.csv", i, f"base '{r[c]}' is not in bases.csv")
        num("vehicles.csv", i, "capacity_kg", r[4], int, 1)
        num("vehicles.csv", i, "speed_kmph", r[5], int, 1)
        if r[6] != "AVAILABLE":
            err("vehicles.csv", i, f"status must be AVAILABLE when starting, got '{r[6]}'")
    for s, c in cat.items():
        if not any(c in CARRIES[t] for t in fleet_types):
            warns.append(f"supply '{s}' ({c}): no vehicle type in your fleet can carry this category, so it can never be delivered")
    for i, r in tables.get("scenario_requests.csv", []):
        if r[1] not in bases or r[2] not in supplies:
            err("scenario_requests.csv", i, "unknown base or supply")
        if r[4] not in {"ROUTINE", "URGENT", "CRITICAL"}:
            err("scenario_requests.csv", i, f"priority must be ROUTINE, URGENT or CRITICAL, got '{r[4]}'")
        num("scenario_requests.csv", i, "qty", r[3], int, 1)
    if len(bases) < 2:
        err("bases.csv", None, "at least two bases are needed (stock moves between bases)")

    print(f"Checked folder: {folder}")
    print(f"  {len(bases)} bases, {len(supplies)} supplies, {len(cells)} inventory rows, {len(vehicles)} vehicles")
    for w in warns:
        print("  WARNING:", w)
    for e in errors:
        print("  ERROR:  ", e)
    print(f"Result: {len(errors)} error(s), {len(warns)} warning(s)." + ("  Ready to load." if not errors else "  Fix the errors, then check again."))
    return 1 if errors else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: python3 scripts/check_my_data.py <folder>")
    sys.exit(main(sys.argv[1]))
