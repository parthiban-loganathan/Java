#!/usr/bin/env bash
# Compiles and runs one student program. Executed INSIDE an isolated container (no network,
# limited memory/CPU/processes, read-only system, unprivileged user) by grade.py.
#   sandbox.sh SRC_DIR CASES_DIR OUT_DIR MAIN_CLASS TIMEOUT_SECONDS
# Writes OUT_DIR/compile.txt, compile.rc and, for every CASES_DIR/<name>.in,
# <name>.out (stdout, kept up to 64 KB), <name>.err (stderr, up to 16 KB) and <name>.rc (exit code;
# 137 = killed at the time limit, 153 = output larger than 1 MB).
set -u
SRC="$1"; CASES="$2"; OUT="$3"; MAIN="$4"; LIMIT="$5"
mkdir -p "$OUT/classes"
javac -encoding UTF-8 -Xlint:none -d "$OUT/classes" "$SRC"/*.java > "$OUT/compile.txt" 2>&1
echo $? > "$OUT/compile.rc"
if [ "$(cat "$OUT/compile.rc")" != "0" ]; then
  exit 0
fi
for f in "$CASES"/*.in; do
  [ -e "$f" ] || continue
  n="$(basename "$f" .in)"
  work="$(mktemp -d)"          # fresh folder: programs that write files start clean
  (
    cd "$work" || exit 1
    ulimit -f 1024            # no file (output included) may exceed 1 MB
    timeout -s KILL "$LIMIT" java -Xss8m -Xmx256m -Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US \
      -cp "$OUT/classes" "$MAIN" < "$f" > "$OUT/$n.out" 2> "$OUT/$n.err"
    echo $? > "$OUT/$n.rc"
  )
  truncate -s '<65536' "$OUT/$n.out" 2>/dev/null
  truncate -s '<16384' "$OUT/$n.err" 2>/dev/null
  rm -rf "$work"
done
exit 0
