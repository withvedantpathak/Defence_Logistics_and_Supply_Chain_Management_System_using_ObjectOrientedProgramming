#!/usr/bin/env python3
"""Coverage proxy (no JaCoCo available): maps every top-level type in src/main to the test classes that
reference it by name, and counts @Test methods. Writes results/test_matrix.csv. Exit 1 if a type is untested."""
import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "src/main/java"
TEST = ROOT / "src/test/java"
SKIP = {"Main"}  # entry point is exercised by TerminalTest through Main.run (listed explicitly below)

TYPE_RE = re.compile(r"^public\s+(?:final\s+|abstract\s+)*(?:class|interface|enum|record)\s+(\w+)", re.M)

tests = {}
for f in sorted(TEST.rglob("*Test.java")):
    text = f.read_text(encoding="utf-8")
    tests[f.stem] = (text, len(re.findall(r"@Test\b", text)))

rows, missing = [], []
for f in sorted(MAIN.rglob("*.java")):
    m = TYPE_RE.search(f.read_text(encoding="utf-8"))
    if not m:
        continue
    name = m.group(1)
    pkg = str(f.parent.relative_to(MAIN / "com/dlscms")).replace("/", ".") or "(root)"
    hits = [t for t, (text, _) in tests.items() if re.search(r"\b%s\b" % re.escape(name), text)]
    if not hits:
        missing.append(name)
    rows.append((pkg, name, ";".join(hits), len(hits)))

out = ROOT / "results/test_matrix.csv"
out.parent.mkdir(exist_ok=True)
with out.open("w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["package", "class", "test_classes", "n_test_classes"])
    w.writerows(rows)

total_tests = sum(n for _, n in tests.values())
print(f"types: {len(rows)}  tested: {len(rows) - len(missing)}  test classes: {len(tests)}  @Test methods: {total_tests}")
if missing:
    print("UNTESTED:", ", ".join(missing))
    sys.exit(1)
print("OK: every public type is referenced by at least one test class ->", out.relative_to(ROOT))
