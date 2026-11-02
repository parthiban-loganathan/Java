#!/usr/bin/env bash
# Builds the complete website into build/site (or the directory given as $1).
#   1. Maven: tests the Java core, compiles course programs + core to JavaScript (TeaVM)
#   2. Copies the static site, the generated JavaScript, course files, guides and project list
#   3. Builds the starter template demo into <site>/starter
# Requires JDK 21 and Maven 3.9 (both preinstalled on GitHub Actions and in the dev container).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$ROOT/build/site}"
SKIP_TESTS="${SKIP_TESTS:-false}"

echo "== Java build (core tests, TeaVM)"
mvn -B -q -f "$ROOT/java/pom.xml" install -DskipTests="$SKIP_TESTS"

echo "== Assembling $OUT"
rm -rf "$OUT"
mkdir -p "$OUT"
cp -R "$ROOT/site/." "$OUT/"
mkdir -p "$OUT/assets/java"
cp "$ROOT/java/web-bridge/target/js/civiltech.js" "$OUT/assets/java/"
java "$ROOT/tools/BuildCourse.java" --course "$ROOT/course" --site "$OUT/course"
mkdir -p "$OUT/docs" "$OUT/projects"
cp "$ROOT"/docs/*.md "$ROOT/docs/index.json" "$OUT/docs/"
cp "$ROOT/projects/projects.json" "$ROOT/projects/projects.schema.json" "$OUT/projects/"
mkdir -p "$OUT/problems"
cp "$ROOT/problems/problems.json" "$OUT/problems/"

if [ -n "${JAVAC_DIST:-}" ] && [ -f "$JAVAC_DIST/compiler.wasm" ]; then
  echo "== In-browser Java compiler from $JAVAC_DIST"
  mkdir -p "$OUT/assets/javac"
  cp "$JAVAC_DIST"/* "$OUT/assets/javac/"
else
  echo "== In-browser Java compiler not included (set JAVAC_DIST; built by tools/browser-javac/build.sh)"
fi

echo "== Starter template demo"
bash "$ROOT/starter-template/build.sh" "$OUT/starter"

echo "== Starter template ZIP (for creating the template repository through the browser)"
mkdir -p "$OUT/downloads"
python3 - "$ROOT/starter-template" "$OUT/downloads/civiltech-innovation-starter.zip" <<'PY'
import os, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
skip = {"target", "dist"}
with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(src):
        dirs[:] = sorted(d for d in dirs if d not in skip)
        for f in sorted(files):
            full = os.path.join(root, f)
            z.write(full, os.path.join("civiltech-innovation-starter", os.path.relpath(full, src)))
PY

echo "== Weekly lab system templates (student and gradebook repositories)"
python3 "$ROOT/lab-system/package.py" "$ROOT/course" "$OUT/downloads"

touch "$OUT/.nojekyll"
echo "Site ready: $OUT ($(du -sh "$OUT" | cut -f1))"
