#!/usr/bin/env python3
"""Assembles the two repository templates of the weekly lab system as ZIP files.

    python3 lab-system/package.py course OUT_DIR
creates OUT_DIR/java-lab-student.zip and OUT_DIR/java-lab-gradebook.zip:
  student:   template + lab-tools/grader + lab-tools/tests (from course)
  gradebook: template + grader + tests (from course) + reference/ (course solutions)
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
                info = zipfile.ZipInfo.from_file(full, os.path.join(top, os.path.relpath(full, src)))
                with open(full, "rb") as fh:
                    z.writestr(info, fh.read(), zipfile.ZIP_DEFLATED)


def copy_grader(dst):
    os.makedirs(dst, exist_ok=True)
    for f in ("grade.py", "collect.py", "sandbox.sh"):
        shutil.copy(os.path.join(HERE, "grader", f), dst)
    os.chmod(os.path.join(dst, "sandbox.sh"), 0o755)


def main(course, out):
    os.makedirs(out, exist_ok=True)
    tmp = tempfile.mkdtemp()
    try:
        s = os.path.join(tmp, "student")
        shutil.copytree(os.path.join(HERE, "student-template"), s)
        copy_grader(os.path.join(s, "lab-tools", "grader"))
        os.remove(os.path.join(s, "lab-tools", "grader", "collect.py"))
        subprocess.run([sys.executable, os.path.join(HERE, "make_tests.py"), course, os.path.join(s, "lab-tools", "tests")], check=True)
        zipdir(s, os.path.join(out, "java-lab-student.zip"), "java-lab-student")

        g = os.path.join(tmp, "gradebook")
        shutil.copytree(os.path.join(HERE, "gradebook-template"), g)
        copy_grader(os.path.join(g, "grader"))
        subprocess.run([sys.executable, os.path.join(HERE, "make_tests.py"), course, os.path.join(g, "tests")], check=True)
        for group in ("lessons", "experiments"):
            for folder in sorted(os.listdir(os.path.join(course, group))):
                d = os.path.join(course, group, folder)
                if not os.path.exists(os.path.join(d, "meta.properties")):
                    continue
                ref = os.path.join(g, "reference", folder.split("-")[0])
                os.makedirs(ref, exist_ok=True)
                for f in os.listdir(d):
                    if f.endswith(".java"):
                        shutil.copy(os.path.join(d, f), ref)
        zipdir(g, os.path.join(out, "java-lab-gradebook.zip"), "java-lab-gradebook")
    finally:
        shutil.rmtree(tmp)
    print(f"wrote {out}/java-lab-student.zip and {out}/java-lab-gradebook.zip")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
