#!/usr/bin/env bash
# Build DLSCMS with plain javac (needs JDK 21+ only). Usage: scripts/build.sh [test]
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=out; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes" dist
find src/main/java -name '*.java' > "$OUT/main-sources.txt"
javac --release 17 -Xlint:all -encoding UTF-8 -d "$OUT/classes" @"$OUT/main-sources.txt"
printf 'Main-Class: com.dlscms.Main\n' > "$OUT/manifest.txt"
jar --create --file dist/dlscms.jar --manifest "$OUT/manifest.txt" -C "$OUT/classes" .
echo "BUILD OK -> dist/dlscms.jar"
if [ "${1:-}" = "test" ]; then
  CP="$OUT/classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar"
  find src/test/java -name '*.java' > "$OUT/test-sources.txt"
  javac --release 17 -encoding UTF-8 -cp "$CP" -d "$OUT/test-classes" @"$OUT/test-sources.txt"
  java -cp "$OUT/test-classes:$CP" com.dlscms.RunAll "$OUT/test-classes"
fi
