#!/usr/bin/env python3
"""Copies the JDK-verified test cases of the course (course/*/*/input*.txt, expected*.txt) into a
tests/ folder for the grader: tests/<ID>/input-N.txt and expected-N.txt.
Programs that need a database (requires=mysql) are skipped.

    python3 lab-system/make_tests.py course OUT_DIR
"""
import os
import re
import shutil
import sys


def main(course, out):
    count = 0
    for group in ("lessons", "experiments"):
        for folder in sorted(os.listdir(os.path.join(course, group))):
            d = os.path.join(course, group, folder)
            meta = os.path.join(d, "meta.properties")
            if not os.path.exists(meta):
                continue
            if "requires=mysql" in open(meta, encoding="utf-8").read():
                continue
            ex = folder.split("-")[0]
            target = os.path.join(out, ex)
            os.makedirs(target, exist_ok=True)
            for name in os.listdir(d):
                m = re.fullmatch(r"(input|expected)(?:-(\d+))?\.txt", name)
                if m:
                    n = m.group(2) or "1"
                    shutil.copy(os.path.join(d, name), os.path.join(target, f"{m.group(1)}-{n}.txt"))
            count += 1
    print(f"{count} exercises with reference tests written to {out}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
