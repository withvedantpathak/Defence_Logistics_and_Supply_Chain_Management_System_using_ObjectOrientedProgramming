#!/usr/bin/env sh
# Re-runs the simulation grid twice (3 seeds) and diffs every column except wall_ms/used_mb. Exit 0 = identical.
cd "$(dirname "$0")/.." || exit 1
[ -f dist/dlscms.jar ] || scripts/build.sh || exit 1
ROOT=results/tmp/repro; rm -rf "$ROOT"; mkdir -p "$ROOT"
for s in 1 2 3; do python3 scripts/gen_data.py --seed "$s" --out "$ROOT/data/$s" >/dev/null || exit 1; done
for run in a b; do
  java -jar dist/dlscms.jar --sim-grid --data-root "$ROOT/data" --seeds 3 --out "$ROOT/$run.csv" 2>&1 | grep -v "Picked up" >/dev/null
  cut -d, -f1-18 "$ROOT/$run.csv" > "$ROOT/$run.stable"
done
if diff -q "$ROOT/a.stable" "$ROOT/b.stable" >/dev/null; then
  echo "REPRODUCIBLE: $(($(wc -l < $ROOT/a.stable) - 1)) runs identical in all columns except timing"
else
  echo "NOT REPRODUCIBLE"; diff "$ROOT/a.stable" "$ROOT/b.stable" | head; exit 1
fi
