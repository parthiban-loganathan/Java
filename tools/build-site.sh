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

echo "== Starter template demo"
bash "$ROOT/starter-template/build.sh" "$OUT/starter"

touch "$OUT/.nojekyll"
echo "Site ready: $OUT ($(du -sh "$OUT" | cut -f1))"
