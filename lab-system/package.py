#!/usr/bin/env python3
"""Assembles the teacher's private gradebook repository as a ZIP file.

    python3 lab-system/package.py course OUT_DIR       -> OUT_DIR/java-lab-gradebook.zip
Contents: lab-system/gradebook (workflow, README, roster, Q01-Q03 example tests and solutions)
+ tests and reference solutions of every course program (course/*/*/input*.txt, expected*.txt).
"""
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))


def zipdir(src, dst, top):
    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk(src):
            dirs.sort()
            for f in sorted(files):
                full = os.path.join(root, f)
                z.write(full, os.path.join(top, os.path.relpath(full, src)))


def main(course, out):
    os.makedirs(out, exist_ok=True)
    tmp = tempfile.mkdtemp()
    try:
        g = os.path.join(tmp, "gradebook")
        shutil.copytree(os.path.join(HERE, "gradebook"), g)
        subprocess.run([sys.executable, os.path.join(HERE, "make_tests.py"), course, os.path.join(g, "tests")], check=True)
        for group in ("lessons", "experiments"):
            for folder in sorted(os.listdir(os.path.join(course, group))):
                d = os.path.join(course, group, folder)
                if not os.path.exists(os.path.join(d, "meta.properties")):
                    continue
                ref = os.path.join(g, "solutions", folder.split("-")[0])
                os.makedirs(ref, exist_ok=True)
                for f in os.listdir(d):
                    if f.endswith(".java"):
                        shutil.copy(os.path.join(d, f), ref)
        zipdir(g, os.path.join(out, "java-lab-gradebook.zip"), "java-lab-gradebook")
    finally:
        shutil.rmtree(tmp)
    print(f"wrote {out}/java-lab-gradebook.zip")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
