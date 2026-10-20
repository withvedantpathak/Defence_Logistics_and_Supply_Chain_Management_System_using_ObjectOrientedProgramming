#!/usr/bin/env sh
# Feeds hostile/garbage input to the terminal via stdin. Passes if exit code is 0 and no Java stack trace appears.
cd "$(dirname "$0")/.." || exit 1
[ -f dist/dlscms.jar ] || scripts/build.sh || exit 1
OUT=$(mktemp)
java -jar dist/dlscms.jar --plain < scripts/smoke_input.txt > "$OUT" 2>&1
CODE=$?
rm -rf reports/stock.csv
FAIL=0
[ "$CODE" -eq 0 ] || { echo "FAIL: exit code $CODE"; FAIL=1; }
if grep -E "^\s+at [a-z]+\.|Exception in thread|Unexpected problem|NullPointer|StackOverflow" "$OUT" >/dev/null; then
  echo "FAIL: stack trace or unexpected problem in output"; grep -nE "^\s+at [a-z]+\.|Exception in thread|Unexpected problem|NullPointer" "$OUT" | head; FAIL=1
fi
ERRS=$(grep -c "ERR >" "$OUT")
grep -q "Goodbye" "$OUT" || { echo "FAIL: did not reach EXIT"; FAIL=1; }
echo "smoke: $(wc -l < scripts/smoke_input.txt) input lines, $ERRS refused with ERR lines, exit=$CODE"
[ "$FAIL" -eq 0 ] && echo "SMOKE OK" || { cp "$OUT" results/smoke_last_output.txt; echo "see results/smoke_last_output.txt"; }
rm -f "$OUT"
exit $FAIL
