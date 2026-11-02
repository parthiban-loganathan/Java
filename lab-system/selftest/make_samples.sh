#!/usr/bin/env bash
# Builds a test lab: every course solution (must pass) plus programs that must fail or be contained.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$1"
rm -rf "$OUT"; mkdir -p "$OUT/lab"
python3 "$ROOT/lab-system/make_tests.py" "$ROOT/course" "$OUT/tests"
for d in "$ROOT"/course/lessons/L* "$ROOT"/course/experiments/E*; do
  id="$(basename "$d" | cut -d- -f1)"
  [ -d "$OUT/tests/$id" ] || continue
  mkdir -p "$OUT/lab/$id"; cp "$d"/*.java "$OUT/lab/$id/"
done
mk() { mkdir -p "$OUT/lab/$1" "$OUT/tests/$1"; cp "$OUT/tests/E01a/"* "$OUT/tests/$1/"; printf '%s\n' "$2" > "$OUT/lab/$1/Main.java"; }
mk X-wrong        'public class Main { public static void main(String[] a) { System.out.println("Interest for Option 1: 0.00"); } }'
mk X-nocompile    'public class Main { public static void main(String[] a) { int x = "text"; } }'
mk X-loop         'public class Main { public static void main(String[] a) { while (true) { } } }'
mk X-flood        'public class Main { public static void main(String[] a) { while (true) System.out.println("flood"); } }'
mk X-network      'public class Main { public static void main(String[] a) throws Exception { new java.net.URL("https://example.com").openStream().read(); System.out.println("NETWORK REACHED"); } }'
mk X-readsecrets  'public class Main { public static void main(String[] a) { System.out.println(System.getenv()); System.out.println(new java.io.File("/w/src").canWrite() ? "SRC WRITABLE" : "src read-only"); } }'
mk X-forkbomb     'public class Main { public static void main(String[] a) throws Exception { for (int i = 0; i < 100000; i++) new Thread(() -> { try { Thread.sleep(100000); } catch (Exception e) {} }).start(); } }'
mkdir -p "$OUT/lab/X-renamed"; sed 's/public class Main/public class Interest/' "$ROOT/course/experiments/E01a-financing-interest/Main.java" > "$OUT/lab/X-renamed/Interest.java"
mkdir -p "$OUT/tests/X-renamed"; cp "$OUT/tests/E01a/"* "$OUT/tests/X-renamed/"
echo "sample lab ready in $OUT"
