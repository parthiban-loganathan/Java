#!/usr/bin/env python3
"""Teacher side: collects every student's lab programs, grades them and writes the gradebook.

    python3 grader/collect.py                      # collect, grade, archive, gradebook
    python3 grader/collect.py --gradebook-only     # rebuild gradebook/ after editing marks.csv
    options: --only E01a E01b   --week 3   --sandbox none (testing only)

Inputs (in the gradebook repository):
    roster.csv       regno,name,github,repository   (repository = owner/name of the student's repo)
    assignments.csv  exercise,week,title,due_ist,max_marks   (due_ist "YYYY-MM-DD HH:MM", may be empty)
    marks.csv        regno,exercise,mark,comment     (filled by the teacher)
    tests/<EX>/      input-N.txt, expected-N.txt    (reference test cases)
    reference/<EX>/  the reference solution (optional; for the similarity check)
Environment: STUDENT_REPOS_TOKEN = a GitHub token that can read the students' private repositories.

With --discover the script first accepts pending collaborator invitations to repositories whose
name starts with --prefix (default java-lab), lists every such repository the token can read, and
adds students missing from roster.csv using their student.json (regno, name).

Outputs:
    archive/<run>/<regno>/<EX>/   code, input.txt, result.json, report.md   (permanent, one folder per run)
    latest/<regno>/<EX>/          the most recent copy of each program
    gradebook/summary.csv         one row per student and exercise (auto score, teacher mark, late, link)
    gradebook/students.csv        one row per student (totals)
    gradebook/missing.csv         exercises not submitted
    gradebook/similarity.csv      pairs of very similar programs (a hint for checking, not proof)
    gradebook/collection-log.csv  what happened to every repository in every run
"""
import argparse
import base64
import csv
import datetime as dt
import difflib
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import grade  # noqa: E402

IST = dt.timezone(dt.timedelta(hours=5, minutes=30))
JAVA_KEYWORDS = set("""abstract assert boolean break byte case catch char class const continue default do double else enum
extends final finally float for goto if implements import instanceof int interface long native new package private
protected public return short static strictfp super switch synchronized this throw throws transient try void volatile
while var record yield true false null String System out println print printf Scanner Math""".split())


def read_csv(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8-sig") as f:
        return [{k.strip(): (v or "").strip() for k, v in row.items() if k} for row in csv.DictReader(f)]


def write_csv(path, rows, fields):
    if os.path.dirname(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def git_env(token):
    env = dict(os.environ)
    env["GIT_TERMINAL_PROMPT"] = "0"
    if token:
        basic = base64.b64encode(f"x-access-token:{token}".encode()).decode()
        env.update(GIT_CONFIG_COUNT="1", GIT_CONFIG_KEY_0="http.extraHeader",
                   GIT_CONFIG_VALUE_0=f"Authorization: Basic {basic}")
    return env


def fetch_repo(repository, dest, token):
    """Clones owner/name (or copies a local folder, for testing). Returns an error message or None."""
    if os.path.isdir(repository):
        shutil.copytree(repository, dest)
        return None
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        return f"repository '{repository}' is not in the form owner/name"
    r = subprocess.run(["git", "clone", "--quiet", "--depth", "200", f"https://github.com/{repository}.git", dest],
                       env=git_env(token), capture_output=True, text=True, timeout=300)
    if r.returncode != 0:
        msg = (r.stderr or "").replace(token or "\0", "***").strip().splitlines()
        return "clone failed: " + (msg[-1] if msg else "unknown error")
    return None


def last_commit(repo_dir, path):
    if not os.path.isdir(os.path.join(repo_dir, ".git")):
        return "", ""
    r = subprocess.run(["git", "-C", repo_dir, "log", "-1", "--format=%H|%cI", "--", path], capture_output=True, text=True)
    parts = r.stdout.strip().split("|")
    return (parts[0], parts[1]) if len(parts) == 2 else ("", "")


def to_ist(iso):
    if not iso:
        return ""
    return dt.datetime.fromisoformat(iso).astimezone(IST).strftime("%Y-%m-%d %H:%M")


def api(method, url, token, timeout=60):
    """Minimal GitHub REST call. Returns (status, parsed JSON or None)."""
    req = urllib.request.Request(url, method=method, headers={
        "Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "civiltech-lab-collector"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            body = r.read()
            return r.status, (json.loads(body) if body else None)
    except urllib.error.HTTPError as e:
        return e.code, None


def discover(args, token):
    """Accepts invitations, lists matching repositories, adds unknown students to roster.csv."""
    base, prefix = args.api.rstrip("/"), args.prefix.lower()
    accepted = 0
    status, invites = api("GET", f"{base}/user/repository_invitations?per_page=100", token)
    for inv in invites or []:
        name = (inv.get("repository") or {}).get("name", "")
        if name.lower().startswith(prefix):
            st, _ = api("PATCH", f"{base}/user/repository_invitations/{inv['id']}", token)
            accepted += st == 204
    repos, page = [], 1
    while True:
        st, items = api("GET", f"{base}/user/repos?affiliation=collaborator,organization_member&per_page=100&page={page}", token)
        if st != 200 or not items:
            break
        repos += [r for r in items if r.get("name", "").lower().startswith(prefix)]
        page += 1
    roster = read_csv(args.roster)
    known_repos = {r.get("repository", "").lower() for r in roster}
    known_regnos = {r.get("regno", "").upper() for r in roster}
    added = []
    for r in repos:
        full = r["full_name"]
        if full.lower() in known_repos:
            continue
        st, f = api("GET", f"{base}/repos/{full}/contents/student.json", token)
        try:
            info = json.loads(base64.b64decode(f["content"]).decode("utf-8"))
            regno, name = str(info.get("regno", "")).strip().upper(), str(info.get("name", "")).strip()
        except (TypeError, KeyError, ValueError):
            print(f"discover: {full}: student.json missing or invalid, not added")
            continue
        if not re.fullmatch(r"[A-Z0-9_-]{1,32}", regno) or regno in known_regnos:
            print(f"discover: {full}: regno '{regno}' invalid or already in the roster, not added")
            continue
        row = {"regno": regno, "name": name, "github": r.get("owner", {}).get("login", ""), "repository": full}
        roster.append(row)
        added.append(row)
        known_regnos.add(regno)
    if added:
        roster.sort(key=lambda x: x.get("regno", ""))
        write_csv(args.roster, roster, ["regno", "name", "github", "repository"])
    print(f"discover: {accepted} invitation(s) accepted, {len(repos)} repositories found, {len(added)} student(s) added to the roster")


def collect(args, token):
    if args.discover:
        if token:
            discover(args, token)
        else:
            print("discover: no STUDENT_REPOS_TOKEN, skipped")
    roster = read_csv(args.roster)
    assignments = read_csv(args.assignments)
    wanted = [a["exercise"] for a in assignments if not args.week or a.get("week") == str(args.week)]
    if args.only:
        wanted = [e for e in wanted if e in args.only] + [e for e in args.only if e not in wanted]
    run_id = dt.datetime.now(IST).strftime("%Y-%m-%d_%H%M")
    log = read_csv(os.path.join(args.gradebook, "collection-log.csv"))
    work = tempfile.mkdtemp(prefix="collect-")
    try:
        for s in roster:
            regno = s.get("regno", "")
            if not re.fullmatch(r"[A-Za-z0-9_-]{1,32}", regno):
                log.append({"run": run_id, "regno": regno, "status": "skipped: invalid regno in roster"})
                continue
            dest = os.path.join(work, regno)
            err = fetch_repo(s.get("repository", ""), dest, token)
            if err:
                log.append({"run": run_id, "regno": regno, "repository": s.get("repository", ""), "status": err})
                print(f"{regno}: {err}")
                continue
            note = ""
            try:
                info = json.load(open(os.path.join(dest, "student.json"), encoding="utf-8"))
                if str(info.get("regno", "")).strip().upper() != regno.upper():
                    note = f"student.json regno '{info.get('regno', '')}' differs from roster"
            except (OSError, ValueError):
                note = "student.json missing or invalid"
            found = 0
            for ex in wanted:
                src = os.path.join(dest, "lab", ex)
                if not os.path.isdir(src) or not glob.glob(os.path.join(src, "*.java")):
                    continue
                found += 1
                target = os.path.join(args.archive, run_id, regno, ex)
                os.makedirs(target, exist_ok=True)
                for f in os.listdir(src):
                    if f.endswith(".java") or f == "input.txt":
                        shutil.copy(os.path.join(src, f), target)
                sha, when = last_commit(dest, f"lab/{ex}")
                result = grade.grade_exercise(ex, os.path.join(args.archive, run_id, regno), args.tests,
                                              args.timeout, args.sandbox, args.image)
                result.update(regno=regno, name=s.get("name", ""), repository=s.get("repository", ""),
                              commit=sha, committed_ist=to_ist(when), run=run_id)
                with open(os.path.join(target, "result.json"), "w", encoding="utf-8") as f:
                    json.dump(result, f, indent=1, ensure_ascii=False)
                with open(os.path.join(target, "report.md"), "w", encoding="utf-8") as f:
                    f.write(grade.markdown(result))
                latest = os.path.join(args.latest, regno, ex)
                shutil.rmtree(latest, ignore_errors=True)
                shutil.copytree(target, latest)
            log.append({"run": run_id, "regno": regno, "repository": s.get("repository", ""),
                        "status": f"ok: {found} exercise(s) collected", "note": note})
            print(f"{regno}: {found} exercise(s) collected {note}")
    finally:
        shutil.rmtree(work, ignore_errors=True)
    write_csv(os.path.join(args.gradebook, "collection-log.csv"), log, ["run", "regno", "repository", "status", "note"])


def tokens(code):
    code = re.sub(r"/\*.*?\*/", " ", code, flags=re.S)
    code = re.sub(r"//[^\n]*", " ", code)
    code = re.sub(r'"(?:\\.|[^"\\])*"', ' "S" ', code)
    out = []
    for t in re.findall(r"[A-Za-z_]\w*|\d+(?:\.\d+)?|\S", code):
        if re.match(r"[A-Za-z_]", t):
            out.append(t if t in JAVA_KEYWORDS else "ID")
        elif t[0].isdigit():
            out.append("N")
        else:
            out.append(t)
    return out


def code_of(folder):
    return "\n".join(open(f, encoding="utf-8", errors="replace").read() for f in sorted(glob.glob(os.path.join(folder, "*.java"))))


def similarity(a, b):
    return difflib.SequenceMatcher(None, a, b, autojunk=False).ratio()


def build_gradebook(args):
    roster = read_csv(args.roster)
    assignments = {a["exercise"]: a for a in read_csv(args.assignments)}
    marks = {(m.get("regno", "").upper(), m.get("exercise", "")): m for m in read_csv(args.marks)}
    latest_result = {}
    for path in glob.glob(os.path.join(args.archive, "*", "*", "*", "result.json")):
        r = json.load(open(path, encoding="utf-8"))
        key = (r.get("regno", "").upper(), r.get("exercise"))
        if key not in latest_result or r.get("run", "") > latest_result[key][0].get("run", ""):
            latest_result[key] = (r, os.path.dirname(path))
    now = dt.datetime.now(IST).strftime("%Y-%m-%d %H:%M")
    summary, missing, students = [], [], []
    for s in roster:
        reg = s.get("regno", "").upper()
        n_sub = late_n = 0
        auto, t_marks, t_max = [], 0.0, 0.0
        for ex, a in assignments.items():
            m = marks.get((reg, ex), {})
            row = {"regno": s.get("regno"), "name": s.get("name"), "exercise": ex, "week": a.get("week", ""),
                   "max_marks": a.get("max_marks", ""), "teacher_mark": m.get("mark", ""), "teacher_comment": m.get("comment", "")}
            try:
                t_max += float(a.get("max_marks") or 0)
                t_marks += float(m.get("mark") or 0)
            except ValueError:
                pass
            if (reg, ex) not in latest_result:
                row["status"] = "not submitted"
                summary.append(row)
                missing.append({"regno": s.get("regno"), "name": s.get("name"), "exercise": ex, "week": a.get("week", "")})
                continue
            r, folder = latest_result[(reg, ex)]
            n_sub += 1
            due = a.get("due_ist", "")
            late = bool(due and r.get("committed_ist") and r["committed_ist"] > due)
            late_n += late
            if r.get("score_percent") is not None:
                auto.append(r["score_percent"])
            row.update(status="submitted" if r.get("compiled") else "does not compile",
                       auto_passed=r.get("passed", 0), auto_total=r.get("total", 0),
                       auto_percent=r.get("score_percent") if r.get("score_percent") is not None else "",
                       last_commit_ist=r.get("committed_ist", ""), late="yes" if late else "",
                       collected=r.get("run", ""), code=os.path.relpath(folder, os.path.dirname(args.gradebook) or "."))
            summary.append(row)
        students.append({"regno": s.get("regno"), "name": s.get("name"), "github": s.get("github"),
                         "repository": s.get("repository"), "submitted": f"{n_sub} of {len(assignments)}",
                         "average_auto_percent": round(sum(auto) / len(auto), 1) if auto else "",
                         "teacher_marks": round(t_marks, 2), "max_marks": round(t_max, 2), "late": late_n})
    write_csv(os.path.join(args.gradebook, "summary.csv"), summary,
              ["regno", "name", "exercise", "week", "status", "auto_passed", "auto_total", "auto_percent",
               "teacher_mark", "max_marks", "teacher_comment", "last_commit_ist", "late", "collected", "code"])
    write_csv(os.path.join(args.gradebook, "students.csv"), students,
              ["regno", "name", "github", "repository", "submitted", "average_auto_percent", "teacher_marks", "max_marks", "late"])
    write_csv(os.path.join(args.gradebook, "missing.csv"), missing, ["regno", "name", "exercise", "week"])

    pairs = []
    by_ex = {}
    for (reg, ex), (r, folder) in latest_result.items():
        by_ex.setdefault(ex, []).append((reg, folder))
    for ex, items in sorted(by_ex.items()):
        toks = {reg: tokens(code_of(folder)) for reg, folder in items}
        ref_dir = os.path.join(args.reference, ex)
        ref = tokens(code_of(ref_dir)) if os.path.isdir(ref_dir) else None
        regs = sorted(toks)
        for i in range(len(regs)):
            for j in range(i + 1, len(regs)):
                a, b = toks[regs[i]], toks[regs[j]]
                if not a or not b or abs(len(a) - len(b)) > 0.3 * max(len(a), len(b)):
                    continue
                sim = similarity(a, b)
                if sim >= args.similarity:
                    pairs.append({"exercise": ex, "regno_a": regs[i], "regno_b": regs[j], "similarity": round(sim, 3),
                                  "a_vs_reference": round(similarity(a, ref), 3) if ref else "",
                                  "b_vs_reference": round(similarity(b, ref), 3) if ref else ""})
    pairs.sort(key=lambda p: (-p["similarity"], p["exercise"]))
    write_csv(os.path.join(args.gradebook, "similarity.csv"), pairs,
              ["exercise", "regno_a", "regno_b", "similarity", "a_vs_reference", "b_vs_reference"])
    with open(os.path.join(args.gradebook, "README.md"), "w", encoding="utf-8") as f:
        f.write(f"# Gradebook\n\nBuilt {now} IST from {len(latest_result)} collected programs of {len(roster)} students.\n\n"
                "| File | Content |\n|------|---------|\n"
                "| [summary.csv](summary.csv) | one row per student and exercise: automatic test score, your mark, late flag, link to the code |\n"
                "| [students.csv](students.csv) | one row per student: exercises submitted, average test score, total marks |\n"
                "| [missing.csv](missing.csv) | exercises not submitted |\n"
                "| [similarity.csv](similarity.csv) | pairs of programs with near-identical structure (variable names ignored). A hint to look closer, not proof of copying; "
                "programs that closely follow the handout solution are expected to be similar, see the *_vs_reference columns |\n"
                "| [collection-log.csv](collection-log.csv) | result of every repository in every collection run |\n\n"
                "Open a CSV on GitHub to see it as a table, or download it (Raw, then save) and open it in Excel.\n")
    print(f"gradebook: {len(summary)} rows, {len(missing)} missing, {len(pairs)} similar pairs")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--roster", default="roster.csv")
    ap.add_argument("--assignments", default="assignments.csv")
    ap.add_argument("--marks", default="marks.csv")
    ap.add_argument("--tests", default="tests")
    ap.add_argument("--reference", default="reference")
    ap.add_argument("--archive", default="archive")
    ap.add_argument("--latest", default="latest")
    ap.add_argument("--gradebook", default="gradebook")
    ap.add_argument("--only", nargs="*")
    ap.add_argument("--week")
    ap.add_argument("--sandbox", choices=["docker", "none"], default="docker")
    ap.add_argument("--image", default=grade.IMAGE)
    ap.add_argument("--timeout", type=int, default=10)
    ap.add_argument("--similarity", type=float, default=0.9, help="report pairs at or above this similarity")
    ap.add_argument("--gradebook-only", action="store_true")
    ap.add_argument("--discover", action="store_true", help="accept invitations and add new students to the roster")
    ap.add_argument("--prefix", default="java-lab", help="name prefix of student repositories")
    ap.add_argument("--api", default="https://api.github.com")
    args = ap.parse_args()
    if not args.gradebook_only:
        collect(args, os.environ.get("STUDENT_REPOS_TOKEN", ""))
    build_gradebook(args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
