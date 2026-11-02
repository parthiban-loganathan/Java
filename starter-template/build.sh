#!/usr/bin/env bash
# Builds the application into dist/ (or the directory given as $1):
#   mvn package  -> runs the tests and translates the Java to JavaScript (target/js/app.js)
#   copies web/ and the generated JavaScript into the output directory.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${1:-$HERE/dist}"
mvn -B -q -f "$HERE/pom.xml" package
rm -rf "$OUT"
mkdir -p "$OUT/java"
cp -R "$HERE/web/." "$OUT/"
cp "$HERE/target/js/app.js" "$OUT/java/app.js"
touch "$OUT/.nojekyll"
echo "Built $OUT"
