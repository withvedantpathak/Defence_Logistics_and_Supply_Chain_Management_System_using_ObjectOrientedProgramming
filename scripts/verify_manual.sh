#!/usr/bin/env sh
# Verifies docs/user_manual.md against the built program (tutorial + error table). Needs python3 and dist/dlscms.jar.
cd "$(dirname "$0")/.." || exit 1
[ -f dist/dlscms.jar ] || scripts/build.sh || exit 1
exec python3 scripts/verify_manual.py
