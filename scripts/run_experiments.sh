#!/usr/bin/env sh
# Full experiment pipeline: generate seeded datasets -> run grid -> analyse -> figures.
# Usage: scripts/run_experiments.sh [seeds]      (default 30)   Needs: JDK-built dist/dlscms.jar, python3 + numpy/matplotlib/scipy
# Reproducibility: the only non-deterministic columns of results/raw/runs.csv are wall_ms and used_mb (timing).
cd "$(dirname "$0")/.." || exit 1
SEEDS=${1:-30}
[ -f dist/dlscms.jar ] || scripts/build.sh || exit 1
ROOT=results/tmp/seeds
rm -rf "$ROOT"; mkdir -p "$ROOT" results/raw
s=1
while [ "$s" -le "$SEEDS" ]; do
  python3 scripts/gen_data.py --seed "$s" --out "$ROOT/$s" >/dev/null || exit 1
  s=$((s+1))
done
START=$(date +%s)
java -jar dist/dlscms.jar --sim-grid --data-root "$ROOT" --seeds "$SEEDS" --out results/raw/runs.csv 2>&1 | grep -v "Picked up" || exit 1
echo "grid finished in $(( $(date +%s) - START )) s"
python3 scripts/analyze.py || exit 1
