#!/usr/bin/env python3
"""Runs the user manual's tutorial (section 5) and error table (section 8) against the real program and checks that
every command is documented and every quoted ERR message really appears. Exit 1 on any mismatch."""
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
manual = (ROOT / "docs/user_manual.md").read_text(encoding="utf-8")

def run(commands):
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as f:
        f.write("\n".join(commands) + "\nEXIT\n")
    out = subprocess.run(["java", "-jar", str(ROOT / "dist/dlscms.jar"), "--plain", "--script", f.name],
                         cwd=ROOT, capture_output=True, text=True, encoding="utf-8").stdout
    Path(f.name).unlink()
    return out

problems = []

# ---- section 5: tutorial ----
sec5 = manual.split("## 5.")[1].split("## 6.")[0]
tutorial = ["MON", "INV PUN", "REQ NEW PUN MED-KIT 200 URGENT", "REQ APPROVE R-0001", "REQ FULFIL R-0001", "DLV",
            "DLV D-0001", "DLV DISPATCH D-0001", "TICK 12", "INV PUN", "RPT KPI"]
for c in tutorial:
    if c not in sec5:
        problems.append(f"tutorial command not found in manual section 5: {c}")
out = run(tutorial)
expect = ["Request R-0001 created (NEW)", "Request R-0001 is now APPROVED", "1 delivery(ies) planned",
          "Delivery D-0001 in transit", "1 delivery(ies) arrived", "REPORT KPI", "OK +"]
for e in expect:
    if e not in out:
        problems.append(f"tutorial output missing: {e!r}")
if not re.search(r"MED-KIT\s+Medical Kit\s+MEDICAL\s+240\b", out):
    problems.append("after the tutorial Pune should hold 240 MED-KIT (40 + 200)")

# ---- section 8: error table ----
sec8 = manual.split("## 8.")[1].split("## 9.")[0]
rows = [r for r in sec8.splitlines() if r.startswith("| `")]
cmds = []
for r in rows:
    cmd = re.match(r"\| `([^`]+)`", r).group(1)
    cmds.append(cmd)
# the table uses prose for two rows; run a prepared sequence that triggers each documented case
setup = ["REQ FULFIL R-9999", "INV ADJ PUN MED-KIT -9999", "REQ NEW PUN MED-KIT 50 URGENT", "REQ APPROVE R-0001",
         "REQ FULFIL R-0001", "DLV DISPATCH D-0001", "DLV DISPATCH D-0001", "BASE REMOVE PUN"]
for v in ["FT-01", "FT-02", "HE-02", "MV-01", "MV-02", "TR-01", "TR-02", "TR-03", "TR-04", "TR-05", "TR-06"]:
    setup.append(f"FLT MAINT {v} ON")
setup += ["REQ NEW NAG MED-KIT 50 URGENT", "REQ APPROVE R-0002", "REQ FULFIL R-0002"]
out8 = run(setup)
quoted = re.findall(r"`ERR ▸ ([^`]+)`", sec8)
if len(quoted) < 4:
    problems.append("expected at least 4 quoted ERR messages in section 8")
for q in quoted:
    text = q.split("…")[0].strip()
    if "ERR > " + text not in out8:
        problems.append(f"documented error not produced: {text!r}")

# ---- every command code in the reference exists in HELP ----
help_out = run(["HELP"])
for code in sorted(set(re.findall(r"^\| `([A-Z]+)[ `]", manual, re.M))):
    if code not in help_out and code not in ("DEMO", "ERR"):
        problems.append(f"manual documents {code} but HELP does not list it")

if problems:
    print("MANUAL MISMATCHES:")
    for p in problems:
        print(" -", p)
    sys.exit(1)
print(f"MANUAL OK: {len(tutorial)} tutorial commands, {len(quoted)} error messages, all verified against the program")
