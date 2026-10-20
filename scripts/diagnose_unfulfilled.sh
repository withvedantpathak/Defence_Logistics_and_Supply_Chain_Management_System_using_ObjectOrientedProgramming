#!/usr/bin/env bash
# Classifies unfulfilled requests by supply category and reason for NEAREST+FASTEST and RANDOM+RANDOM over all seeds.
# Needs dist/dlscms.jar and results/tmp/seeds/ (from scripts/run_experiments.sh). Output: results/unfulfilled_diagnosis.csv
set -euo pipefail
cd "$(dirname "$0")/.."
SEEDS=${1:-30}
MODE=${2:-}          # pass "replan" to add one extra planning pass at the horizon end (writes results/unfulfilled_diagnosis_replan.csv)
OUT=results/unfulfilled_diagnosis${MODE:+_$MODE}.csv
mkdir -p out/diag
javac --release 17 -cp dist/dlscms.jar -d out/diag scripts/diag/UnfulfilledDiag.java 2>&1 | grep -v "Picked up" || true
echo "scale,seed,src,veh,category,reason,count" > "$OUT"
for s in $(seq 1 "$SEEDS"); do
  for sc in small medium large; do
    for c in "NEAREST FASTEST" "RANDOM RANDOM"; do
      java -cp dist/dlscms.jar:out/diag UnfulfilledDiag "results/tmp/seeds/$s/$sc" "$sc" "$s" $c $MODE 2>&1 | grep -v "Picked up" >> "$OUT"
    done
  done
done
echo "wrote $OUT"
