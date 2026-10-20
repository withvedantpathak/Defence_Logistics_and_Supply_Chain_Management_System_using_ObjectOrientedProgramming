#!/usr/bin/env sh
# Starts DLSCMS. Usage: ./run.sh [--plain] [--demo] [--script file] [--data folder]
cd "$(dirname "$0")" || exit 1
if [ ! -f dist/dlscms.jar ]; then
  echo "dist/dlscms.jar not found - building (needs JDK 17+)..."
  scripts/build.sh || exit 1
fi
exec java -jar dist/dlscms.jar "$@"
