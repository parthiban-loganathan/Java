#!/usr/bin/env python3
"""Compiles and runs students' Java exercises and compares the output with reference test cases.

Layout expected:
    LAB_DIR/<EXERCISE>/*.java          the student's program (the class with main is usually Main)
    LAB_DIR/<EXERCISE>/input.txt       optional: the student's own input, run but not scored
    TESTS_DIR/<EXERCISE>/input-N.txt   reference input of case N
    TESTS_DIR/<EXERCISE>/expected-N.txt  output of the reference program (JDK 21) for case N

Usage:
    python3 grade.py --lab lab --tests tests --out results [--only E01a E02b] [--sandbox docker|none]

--sandbox docker (default) runs every program in a container without network access, with
limited memory, CPU, processes and time, as an unprivileged user on a read-only system: student
code never sees the repository, its credentials or the internet. --sandbox none runs on the host
and is only for testing the grader with trusted code.

Writes OUT/<EXERCISE>.json and .md for every exercise, and OUT/summary.json and summary.md.
Only the Python standard library is used.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
IMAGE = "eclipse-temurin:21-jdk"
IST = dt.timezone(dt.timedelta(hours=5, minutes=30))
NAME_OK = re.compile(r"^[A-Za-z0-9_-]{1,32}$")


def normalise(text):
    lines = text.replace("\r\n", "\n").split("\n")
    return "\n".join(l.rstrip() for l in lines).strip()


def read(path, limit=None):
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            return f.read() if limit is None else f.read(limit)
    except FileNotFoundError:
        return ""


def main_class(java_files):
    """Class to run: Main if present, else the public class that declares main, else Main."""
    names = [os.path.splitext(os.path.basename(f))[0] for f in java_files]
    if "Main" in names:
        return "Main"
    for f in java_files:
        src = read(f)
        if re.search(r"static\s+void\s+main\s*\(", src):
            m = re.search(r"public\s+(?:final\s+|abstract\s+)*class\s+(\w+)", src)
            if m:
                return m.group(1)
            m = re.search(r"class\s+(\w+)", src)
            if m:
                return m.group(1)
    return "Main"


def reference_cases(tests_dir):
    """[(case name, input path or None, expected text)] sorted by case number."""
    cases = []
    if not os.path.isdir(tests_dir):
        return cases
    for name in os.listdir(tests_dir):
        m = re.fullmatch(r"expected-(\d+)\.txt", name)
        if m:
            n = int(m.group(1))
            inp = os.path.join(tests_dir, f"input-{n}.txt")
            cases.append((n, inp if os.path.exists(inp) else None, read(os.path.join(tests_dir, name))))
    return [(f"case-{n}", inp, exp) for n, inp, exp in sorted(cases)]


def run_sandbox(src, cases, out, main, limit, sandbox, image):
    total_timeout = 120 + limit * max(1, len(os.listdir(cases)))
    if sandbox == "docker":
        os.chmod(out, 0o777)
        cmd = ["docker", "run", "--rm", "--network", "none",
               "--memory", "768m", "--memory-swap", "768m", "--cpus", "1", "--pids-limit", "256",
               "--security-opt", "no-new-privileges", "--cap-drop", "ALL", "--user", "65534:65534",
               "--read-only", "--tmpfs", "/tmp:rw,exec,size=64m",
               "-e", "HOME=/tmp", "-e", "JAVA_TOOL_OPTIONS=",
               "-v", f"{src}:/w/src:ro", "-v", f"{cases}:/w/cases:ro", "-v", f"{out}:/w/out:rw",
               "-v", f"{os.path.join(HERE, 'sandbox.sh')}:/w/sandbox.sh:ro",
               image, "bash", "/w/sandbox.sh", "/w/src", "/w/cases", "/w/out", main, str(limit)]
    else:
        env = {k: v for k, v in os.environ.items() if k != "JAVA_TOOL_OPTIONS"}
        cmd = ["bash", os.path.join(HERE, "sandbox.sh"), src, cases, out, main, str(limit)]
    try:
        subprocess.run(cmd, timeout=total_timeout, check=False,
                       stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
                       env=None if sandbox == "docker" else env)
    except subprocess.TimeoutExpired:
        with open(os.path.join(out, "sandbox-timeout"), "w") as f:
            f.write("whole run exceeded %d s" % total_timeout)


def grade_exercise(name, lab_dir, tests_root, limit=10, sandbox="docker", image=IMAGE):
    ex_dir = os.path.join(lab_dir, name)
    java_files = sorted(os.path.join(ex_dir, f) for f in os.listdir(ex_dir) if f.endswith(".java"))
    result = {"exercise": name, "graded_at": dt.datetime.now(IST).isoformat(timespec="seconds"),
              "files": [os.path.basename(f) for f in java_files], "cases": []}
    if not java_files:
        result.update(compiled=False, compile_output="No .java file in this folder.", passed=0, total=0,
                      score_percent=None, main_class=None, code_sha256=None)
        return result
    digest = hashlib.sha256()
    for f in java_files:
        with open(f, "rb") as fh:
            digest.update(fh.read())
    result["code_sha256"] = digest.hexdigest()
    main = main_class(java_files)
    result["main_class"] = main
    tests_dir = next((os.path.join(tests_root, d) for d in (os.listdir(tests_root) if os.path.isdir(tests_root) else [])
                      if d.lower() == name.lower()), None)
    refs = reference_cases(tests_dir) if tests_dir else []

    work = tempfile.mkdtemp(prefix="grade-")
    try:
        src, cases, out = (os.path.join(work, d) for d in ("src", "cases", "out"))
        for d in (src, cases, out):
            os.makedirs(d)
        for f in java_files:
            shutil.copy(f, src)
        for case, inp, _ in refs:
            shutil.copy(inp, os.path.join(cases, case + ".in")) if inp else open(os.path.join(cases, case + ".in"), "w").close()
        own = os.path.join(ex_dir, "input.txt")
        if os.path.exists(own):
            shutil.copy(own, os.path.join(cases, "your-input.in"))
        elif not refs:
            open(os.path.join(cases, "your-input.in"), "w").close()
        for d in (src, cases):
            for f in os.listdir(d):
                os.chmod(os.path.join(d, f), 0o644)
            os.chmod(d, 0o755)
        run_sandbox(src, cases, out, main, limit, sandbox, image)

        result["compiled"] = read(os.path.join(out, "compile.rc")).strip() == "0"
        compile_out = read(os.path.join(out, "compile.txt"), 6000)
        result["compile_output"] = compile_out.replace(src + os.sep, "").replace("/w/src/", "")
        result["time_limit_s"] = limit
        if os.path.exists(os.path.join(out, "sandbox-timeout")):
            result["compile_output"] += "\nThe whole run exceeded its time limit."
        passed = 0
        all_cases = [(c, e, True) for c, _, e in refs]
        if os.path.exists(os.path.join(cases, "your-input.in")):
            all_cases.append(("your-input", None, False))
        for case, expected, scored in all_cases:
            rc_text = read(os.path.join(out, case + ".rc")).strip()
            rc = int(rc_text) if rc_text.lstrip("-").isdigit() else None
            actual = read(os.path.join(out, case + ".out"))
            err = read(os.path.join(out, case + ".err"))
            entry = {"name": case, "scored": scored, "exit_code": rc,
                     "timed_out": rc in (124, 137),   # timeout(1): 124, or 137 when killed by SIGKILL
                     "output_too_large": rc == 153 or os.path.getsize(os.path.join(out, case + ".out")) >= 65536
                     if os.path.exists(os.path.join(out, case + ".out")) else False,
                     "actual": actual[:20000], "stderr": err[:4000]}
            if scored:
                entry["expected"] = expected[:20000]
                entry["passed"] = bool(result["compiled"]) and rc is not None and normalise(actual) == normalise(expected)
                passed += entry["passed"]
            result["cases"].append(entry)
        total = len(refs)
        result.update(passed=passed, total=total,
                      score_percent=round(100.0 * passed / total, 1) if total else None)
    finally:
        shutil.rmtree(work, ignore_errors=True)
    return result


def fence(text):
    """Code block that cannot be broken out of by the student's output."""
    text = text if text.strip() else "(empty)"
    return "~~~~text\n" + text.replace("~~~~", "~ ~ ~ ~") + "\n~~~~"


def markdown(r):
    lines = [f"## {r['exercise']}", "", f"Graded {r['graded_at']} (IST), files: {', '.join(r['files']) or 'none'}", ""]
    if not r.get("compiled"):
        lines += ["**Status: does not compile.** Fix the first error below, save and commit again.", "",
                  fence(r.get("compile_output", ""))]
        return "\n".join(lines) + "\n"
    if r["total"]:
        lines.append(f"**Reference tests passed: {r['passed']} of {r['total']} ({r['score_percent']} %)**")
    else:
        lines.append("**Compiled and ran.** There are no reference tests for this exercise; check the output yourself.")
    lines.append("")
    lines += ["| Case | Result |", "|------|--------|"]
    for c in r["cases"]:
        if c["scored"]:
            status = "passed" if c["passed"] else (f"time limit exceeded ({r.get('time_limit_s', 10)} s)" if c["timed_out"]
                                                   else "output larger than 64 KB" if c["output_too_large"] else "different output")
        else:
            status = "your own input (not scored)"
        lines.append(f"| {c['name']} | {status} |")
    for c in r["cases"]:
        if c["scored"] and c["passed"]:
            continue
        lines += ["", f"### {c['name']}", "", "Your output" + (" (first 4000 characters)" if len(c["actual"]) > 4000 else "") + ":",
                  "", fence(c["actual"][:4000])]
        if c["scored"]:
            lines += ["", "Expected output:", "", fence(c["expected"][:4000])]
        if c["stderr"].strip():
            lines += ["", "Error messages:", "", fence(c["stderr"][:2000])]
    return "\n".join(lines) + "\n"


def exercises(lab_dir, only):
    names = sorted(d for d in os.listdir(lab_dir) if os.path.isdir(os.path.join(lab_dir, d)) and NAME_OK.match(d))
    if only:
        wanted = {o.lower() for o in only}
        names = [n for n in names if n.lower() in wanted]
    return names


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--lab", required=True)
    ap.add_argument("--tests", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--only", nargs="*")
    ap.add_argument("--sandbox", choices=["docker", "none"], default="docker")
    ap.add_argument("--image", default=IMAGE)
    ap.add_argument("--timeout", type=int, default=10, help="seconds per test case")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    summary = []
    for name in exercises(a.lab, a.only):
        r = grade_exercise(name, a.lab, a.tests, a.timeout, a.sandbox, a.image)
        with open(os.path.join(a.out, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(r, f, indent=1, ensure_ascii=False)
        with open(os.path.join(a.out, name + ".md"), "w", encoding="utf-8") as f:
            f.write(markdown(r))
        summary.append({k: r.get(k) for k in ("exercise", "compiled", "passed", "total", "score_percent", "graded_at")})
        print(f"{name}: compiled={r.get('compiled')} passed={r.get('passed')}/{r.get('total')}")
    with open(os.path.join(a.out, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=1)
    rows = ["# My lab results", "", "Updated automatically after every commit. Click an exercise for details.", "",
            "| Exercise | Compiles | Tests passed | Graded (IST) |", "|----------|----------|--------------|--------------|"]
    for s in summary:
        tests = f"{s['passed']} / {s['total']}" if s.get("total") else "no reference tests"
        rows.append(f"| [{s['exercise']}]({s['exercise']}.md) | {'yes' if s['compiled'] else 'NO'} | {tests} | {s['graded_at']} |")
    with open(os.path.join(a.out, "summary.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(rows) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
