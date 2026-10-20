#!/usr/bin/env python3
"""Synthetic dataset generator for DLSCMS (Phase 2).

Deterministic: the same --seed and --scale always produce byte-identical files.
Only the Python standard library is used. All data is synthetic: base names are
generic "Logistics Node" labels placed at approximate public city-centre
coordinates; quantities, fleets and requests are randomly generated.

Usage: python3 scripts/gen_data.py [--scale small|medium|large|all] [--seed 42] [--out data]
"""
import argparse
import csv
import os
import random

# id, name, city, lat, lon, size factor (relative storage/demand scale)
# Coordinates are approximate city-centre values rounded to 2 decimals (accuracy is immaterial here).
BASES = [
    ("PUN", "Pune Logistics Node", "Pune", 18.52, 73.86, 1.0),
    ("MUM", "Mumbai Logistics Node", "Mumbai", 19.08, 72.88, 1.4),
    ("NAG", "Nagpur Logistics Node", "Nagpur", 21.15, 79.09, 1.1),
    ("DEL", "Delhi Logistics Node", "Delhi", 28.61, 77.21, 1.5),
    ("JAI", "Jaipur Logistics Node", "Jaipur", 26.91, 75.79, 0.9),
    ("LEH", "Leh Logistics Node", "Leh", 34.15, 77.58, 0.6),
    ("GUW", "Guwahati Logistics Node", "Guwahati", 26.14, 91.74, 0.8),
    ("CHE", "Chennai Logistics Node", "Chennai", 13.08, 80.27, 1.2),
    ("KOL", "Kolkata Logistics Node", "Kolkata", 22.57, 88.36, 1.2),
    ("BLR", "Bengaluru Logistics Node", "Bengaluru", 12.97, 77.59, 1.1),
    ("AMB", "Ambala Logistics Node", "Ambala", 30.38, 76.78, 0.8),
    ("SRI", "Srinagar Logistics Node", "Srinagar", 34.08, 74.80, 0.6),
]

# id, name, category, unit weight kg, reorder level at size factor 1.0
SUPPLIES = [
    ("MED-KIT", "Medical Kit", "MEDICAL", 2.5, 120),
    ("OXY-CYL", "Oxygen Cylinder", "MEDICAL", 12.0, 40),
    ("IV-FLUID", "IV Fluid Pack", "MEDICAL", 1.0, 300),
    ("RATION", "Ration Pack", "FOOD", 1.2, 900),
    ("WATER-20L", "Drinking Water 20L", "FOOD", 20.0, 200),
    ("DIESEL-DRUM", "Diesel Drum 200L", "FUEL", 180.0, 60),
    ("PETROL-DRUM", "Petrol Drum 200L", "FUEL", 150.0, 40),
    ("TYRE-SET", "Vehicle Tyre", "SPARES", 60.0, 30),
    ("ENGINE-PART", "Engine Spare Kit", "SPARES", 35.0, 25),
    ("RADIO-SET", "Field Radio Set", "SPARES", 8.0, 40),
    ("TENT-6P", "Tent 6-person", "SHELTER", 45.0, 50),
    ("BLANKET", "Blanket", "SHELTER", 2.0, 400),
]
SMALL_SUPPLIES = ["MED-KIT", "OXY-CYL", "RATION", "WATER-20L", "DIESEL-DRUM", "TYRE-SET", "TENT-6P", "BLANKET"]

# type, id prefix, share of fleet, capacity options kg, speed km/h (representative values, not model-specific)
VEHICLE_TYPES = [
    ("Truck", "TR", 0.50, [4000, 5000, 7000], 45),
    ("FuelTruck", "FT", 0.15, [4000, 6000, 8000], 40),
    ("MedicalVan", "MV", 0.20, [800, 1000, 1500], 60),
    ("Helicopter", "HE", 0.15, [500, 800, 1200], 180),
]

SCALES = {
    "small": dict(bases=6, vehicles=12, days=10, requests=40, supplies=SMALL_SUPPLIES),
    "medium": dict(bases=10, vehicles=30, days=30, requests=200, supplies=[s[0] for s in SUPPLIES]),
    "large": dict(bases=12, vehicles=60, days=60, requests=1000, supplies=[s[0] for s in SUPPLIES]),
}

PRIORITIES = [("ROUTINE", 0.60), ("URGENT", 0.30), ("CRITICAL", 0.10)]

HEADERS = {
    "bases.csv": ["id", "name", "city", "lat", "lon", "capacity_units"],
    "supplies.csv": ["id", "name", "category", "unit_weight_kg"],
    "inventory.csv": ["base_id", "supply_id", "qty", "reserved", "reorder_level"],
    "vehicles.csv": ["id", "type", "home_base", "current_base", "capacity_kg", "speed_kmph", "status"],
    "requests.csv": ["id", "requester_base", "supply", "qty", "fulfilled_qty", "priority", "status", "created_hour"],
    "deliveries.csv": ["id", "request", "source", "dest", "supply", "qty", "vehicle", "status",
                       "planned_hour", "dispatched_hour", "eta_hour", "delivered_hour", "distance_km", "failure_reason"],
    "scenario_requests.csv": ["id", "requester_base", "supply", "qty", "priority", "day", "hour"],
}


def write_csv(path, header, rows):
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(header)
        w.writerows(rows)


def weighted_choice(rng, items, weights):
    total = sum(weights)
    x = rng.random() * total
    acc = 0.0
    for item, w in zip(items, weights):
        acc += w
        if x <= acc:
            return item
    return items[-1]


def gen_inventory(rng, bases, supplies):
    """inv[(base, supply)] = [qty, reorder]. 25% shortage / 55% normal / 20% surplus cells."""
    inv = {}
    for b in bases:
        for s in supplies:
            reorder = max(5, round(s[4] * b[5] * rng.uniform(0.8, 1.2)))
            r = rng.random()
            if r < 0.25:
                factor = rng.uniform(0.15, 0.9)
            elif r < 0.80:
                factor = rng.uniform(1.0, 1.6)
            else:
                factor = rng.uniform(1.8, 3.0)
            inv[(b[0], s[0])] = [round(reorder * factor), reorder]
    return inv


def repair_feasibility(inv, bases, supplies, ratio=1.2):
    """Ensure network surplus >= ratio * network shortage for every supply (deterministic repair)."""
    for s in supplies:
        sid = s[0]
        for _ in range(1000):
            shortage = sum(max(0, inv[(b[0], sid)][1] - inv[(b[0], sid)][0]) for b in bases)
            surplus = sum(max(0, inv[(b[0], sid)][0] - inv[(b[0], sid)][1]) for b in bases)
            if surplus >= ratio * shortage:
                break
            best = max(bases, key=lambda b: inv[(b[0], sid)][0] / inv[(b[0], sid)][1])
            cell = inv[(best[0], sid)]
            cell[0] += max(1, round(0.1 * cell[1]))


def seed_demo_scenario(inv):
    """Guarantee the brief's example: Pune is short of medical kits; Mumbai and Nagpur hold spare kits."""
    if ("PUN", "MED-KIT") in inv:
        inv[("PUN", "MED-KIT")] = [40, 120]
    if ("MUM", "MED-KIT") in inv:
        inv[("MUM", "MED-KIT")] = [420, 140]
    if ("NAG", "MED-KIT") in inv:
        inv[("NAG", "MED-KIT")] = [300, 110]


def gen_vehicles(rng, bases, count):
    # number per type, at least one of each
    counts = [max(1, round(count * t[2])) for t in VEHICLE_TYPES]
    while sum(counts) > count:
        counts[counts.index(max(counts))] -= 1
    while sum(counts) < count:
        counts[0] += 1
    order = []
    for t, n in zip(VEHICLE_TYPES, counts):
        order += [t] * n
    homes = [b[0] for b in bases]
    rng.shuffle(homes)
    vehicles, seq = [], {}
    for i, t in enumerate(order):
        seq[t[1]] = seq.get(t[1], 0) + 1
        home = homes[i % len(homes)]
        vehicles.append([f"{t[1]}-{seq[t[1]]:02d}", t[0], home, home, rng.choice(t[3]), t[4], "AVAILABLE"])
    # demo guarantee: a MedicalVan is based at Mumbai if Mumbai exists
    if any(b[0] == "MUM" for b in bases) and not any(v[1] == "MedicalVan" and v[2] == "MUM" for v in vehicles):
        for v in vehicles:
            if v[1] == "MedicalVan":
                v[2] = v[3] = "MUM"
                break
    vehicles.sort(key=lambda v: v[0])
    return vehicles


def gen_requests(rng, bases, supplies, inv, days, n):
    reqs = []
    stamps = sorted((rng.randrange(days), rng.randrange(24)) for _ in range(n))
    for i, (day, hour) in enumerate(stamps, start=1):
        s = rng.choice(supplies)
        weights = []
        for b in bases:
            qty, reorder = inv[(b[0], s[0])]
            weights.append(1.0 + 4.0 * max(0.0, (reorder - qty) / reorder))
        b = weighted_choice(rng, bases, weights)
        reorder = inv[(b[0], s[0])][1]
        qty = max(1, round(reorder * rng.uniform(0.15, 0.60)))
        prio = weighted_choice(rng, [p[0] for p in PRIORITIES], [p[1] for p in PRIORITIES])
        reqs.append([f"S-{i:04d}", b[0], s[0], qty, prio, day, hour])
    return reqs


def generate(scale, seed, out_root):
    cfg = SCALES[scale]
    rng = random.Random(f"{seed}-{scale}")
    bases = BASES[: cfg["bases"]]
    supplies = [s for s in SUPPLIES if s[0] in cfg["supplies"]]
    inv = gen_inventory(rng, bases, supplies)
    seed_demo_scenario(inv)
    repair_feasibility(inv, bases, supplies)
    seed_demo_scenario(inv)  # re-assert demo cells (repair may only add surplus elsewhere)
    vehicles = gen_vehicles(rng, bases, cfg["vehicles"])
    reqs = gen_requests(rng, bases, supplies, inv, cfg["days"], cfg["requests"])

    d = os.path.join(out_root, scale)
    os.makedirs(d, exist_ok=True)
    base_rows = []
    for b in bases:
        total = sum(inv[(b[0], s[0])][0] for s in supplies)
        cap = int(round(total * rng.uniform(1.4, 2.0) / 100.0)) * 100
        base_rows.append([b[0], b[1], b[2], f"{b[3]:.2f}", f"{b[4]:.2f}", cap])
    write_csv(os.path.join(d, "bases.csv"), HEADERS["bases.csv"], base_rows)
    write_csv(os.path.join(d, "supplies.csv"), HEADERS["supplies.csv"],
              [[s[0], s[1], s[2], f"{s[3]:.1f}"] for s in supplies])
    write_csv(os.path.join(d, "inventory.csv"), HEADERS["inventory.csv"],
              [[b[0], s[0], inv[(b[0], s[0])][0], 0, inv[(b[0], s[0])][1]] for b in bases for s in supplies])
    write_csv(os.path.join(d, "vehicles.csv"), HEADERS["vehicles.csv"], vehicles)
    write_csv(os.path.join(d, "requests.csv"), HEADERS["requests.csv"], [])
    write_csv(os.path.join(d, "deliveries.csv"), HEADERS["deliveries.csv"], [])
    write_csv(os.path.join(d, "scenario_requests.csv"), HEADERS["scenario_requests.csv"], reqs)
    write_csv(os.path.join(d, "meta.csv"), ["key", "value"], [
        ["clock_hour", 0], ["next_request", 1], ["next_delivery", 1],
        ["source_strategy", "NEAREST"], ["vehicle_strategy", "FASTEST"],
        ["seed", seed], ["scale", scale]])
    print(f"generated {scale}: {len(bases)} bases, {len(supplies)} supplies, "
          f"{len(vehicles)} vehicles, {len(reqs)} scenario requests -> {d}")


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--scale", default="all", choices=list(SCALES) + ["all"])
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--out", default="data")
    a = ap.parse_args()
    for sc in (SCALES if a.scale == "all" else [a.scale]):
        generate(sc, a.seed, a.out)


if __name__ == "__main__":
    main()
