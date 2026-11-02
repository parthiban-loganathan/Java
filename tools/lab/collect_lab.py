#!/usr/bin/env python3
"""Collects, decrypts and marks Java Lab submissions (runs in the teacher's PRIVATE gradebook).

For every "Lab submission" issue of the public lab repository:
  1. takes the package copy kept by the intake bot (students cannot edit it),
  2. decrypts it with the private key (environment variable LAB_PRIVATE_KEY),
  3. decides acceptance again from lab/control.json and GitHub's creation time of the issue
     (label "lab-accept-late" on an issue = accepted anyway; "lab-ignore" = left out),
  4. saves every attempt: OUT/<session>/<question>/<regno>/<issue>/ with the .java files,
     submission.json (code + full activity log; open it in teacher.html to replay the typing),
  5. marks the LAST accepted attempt of each student and question with the sandboxed grader
     against TESTS/<question>/ (your hidden cases), and summarises the activity log,
  6. writes OUT/<session>/marks.csv, marks-wide.csv, similarity.csv and README.md.

    LAB_PRIVATE_KEY="$(cat lab-private-key.pem)" python3 collect_lab.py --repo OWNER/REPO \
        --tests tests --roster roster.csv --out results [--session W05] [--sandbox docker]
Needs: Python 3.10+, the cryptography package, Docker (for --sandbox docker).
"""
import argparse
import csv
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import sys
import tempfile
import urllib.error
import urllib.request
from itertools import combinations

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(HERE)), "lab-system", "grader"))
import activity  # noqa: E402
import control as ctl  # noqa: E402
import labcrypto  # noqa: E402
import intake  # noqa: E402

IST = dt.timezone(dt.timedelta(hours=5, minutes=30))
SAFE = re.compile(r"[^A-Za-z0-9_-]")
JAVA_NAME = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]{0,63}\.java$")
BOT = "github-actions[bot]"


# ---------------------------------------------------------------- GitHub (read only)
class Api:
    def __init__(self, repo, token=None, api="https://api.github.com"):
        self.repo, self.token, self.api = repo, token, api

    def get(self, path, accept="application/vnd.github+json"):
        headers = {"Accept": accept, "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "lab-collect"}
        if self.token:
            headers["Authorization"] = "Bearer " + self.token
        with urllib.request.urlopen(urllib.request.Request(self.api + path, headers=headers), timeout=60) as r:
            raw = r.read()
            return raw.decode("utf-8") if accept.endswith("raw") else json.loads(raw)

    def pages(self, path):
        page = 1
        while True:
            sep = "&" if "?" in path else "?"
            items = self.get("%s%sper_page=100&page=%d" % (path, sep, page))
            yield from items
            if len(items) < 100:
                return
            page += 1

    def issues(self):
        for i in self.pages("/repos/%s/issues?labels=lab-submission&state=all&sort=created&direction=asc" % self.repo):
            if "pull_request" not in i:
                yield i

    def comments(self, number):
        return list(self.pages("/repos/%s/issues/%d/comments" % (self.repo, number)))

    def control(self):
        try:
            return json.loads(self.get("/repos/%s/contents/lab/control.json" % self.repo, accept="application/vnd.github.raw"))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return ctl.empty()
            raise


# ---------------------------------------------------------------- helpers
def ist(text):
    return ctl.parse_time(text).astimezone(IST).strftime("%Y-%m-%d %H:%M:%S")


def package_of(issue, comments):
    """(binary package, source) from the intake bot's comment, else from the issue body."""
    for c in comments:
        if (c.get("user") or {}).get("login") == BOT and (c.get("body") or "").startswith(intake.MARKER):
            found = labcrypto.find_packages(c["body"])
            if found:
                return found[-1], "bot copy"
    found = labcrypto.find_packages(issue.get("body") or "")
    if found:
        return found[-1], "issue body (no bot copy: the issue may have been edited)"
    return None, "no package"


def tokens(code):
    code = re.sub(r"//[^\n]*|/\*.*?\*/", " ", code, flags=re.S)
    code = re.sub(r'"(?:\\.|[^"\\])*"', '"S"', code)
    return re.findall(r"[A-Za-z_]\w*|\d+(?:\.\d+)?|\S", code)


def fingerprint(code, k=5):
    t = tokens(code)
    return {hash(tuple(t[i:i + k])) for i in range(max(1, len(t) - k + 1))}


def similarity(a, b):
    return len(a & b) / len(a | b) if a and b else 0.0


def tests_hash(path):
    h = hashlib.sha256()
    if os.path.isdir(path):
        for name in sorted(os.listdir(path)):
            with open(os.path.join(path, name), "rb") as f:
                h.update(name.encode() + b"\0" + f.read())
    return h.hexdigest()


def load_roster(path):
    roster = {}
    if path and os.path.exists(path):
        with open(path, newline="", encoding="utf-8-sig") as f:
            for row in csv.DictReader(f):
                reg = (row.get("regno") or "").strip().upper()
                if reg:
                    roster[reg] = {"name": (row.get("name") or "").strip(), "github": (row.get("github") or "").strip().lower()}
    return roster


def write_csv(path, header, rows):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)


# ---------------------------------------------------------------- main work
def collect(api, key, out, tests_root, roster, session_filter, sandbox, timeout):
    import grade  # lab-system/grader/grade.py
    control = api.control()
    attempts = []
    for issue in api.issues():
        labels = {l["name"] for l in issue.get("labels", [])}
        if "lab-ignore" in labels:
            continue
        session = intake.field(issue.get("body") or "", "Session").strip()
        if session_filter and session != session_filter:
            continue
        binary, source = package_of(issue, api.comments(issue["number"]))
        a = {"issue": issue["number"], "url": issue["html_url"], "github": issue["user"]["login"],
             "created_at": issue["created_at"], "session": session,
             "question": intake.field(issue.get("body") or "", "Question").strip(), "source": source, "problems": []}
        if binary is None:
            a.update(accepted=False, reason="no readable package")
            attempts.append(a)
            continue
        a["sha256"] = labcrypto.sha256(binary)
        try:
            data = labcrypto.decrypt(binary, key)
        except labcrypto.PackageError as e:
            a.update(accepted=False, reason=str(e))
            attempts.append(a)
            continue
        a["data"] = data
        a["regno"] = SAFE.sub("", str(data.get("regno", "")).upper())[:20] or "UNKNOWN"
        a["name"] = str(data.get("name", ""))[:80]
        if data.get("session") != a["session"] or data.get("question") != a["question"]:
            a["problems"].append("issue says %s/%s but the package says %s/%s" % (a["session"], a["question"], data.get("session"), data.get("question")))
            a["session"], a["question"] = str(data.get("session")), str(data.get("question"))
        ok, reason = ctl.decide(control, a["session"], a["question"], ctl.parse_time(issue["created_at"]))
        if not ok and "lab-accept-late" in labels:
            ok, reason = True, "accepted by the teacher (label lab-accept-late); " + reason
        a.update(accepted=ok, reason=reason)
        if source != "bot copy":
            a["problems"].append(source)
        r = roster.get(a["regno"])
        if roster and r is None:
            a["problems"].append("register number not in roster.csv")
        elif r and r["github"] and r["github"] != a["github"].lower():
            a["problems"].append("submitted from GitHub account %s, roster says %s" % (a["github"], r["github"]))
        attempts.append(a)

    # save every attempt
    for a in attempts:
        if "data" not in a:
            continue
        d = os.path.join(out, SAFE.sub("_", a["session"]), SAFE.sub("_", a["question"]), a["regno"], str(a["issue"]))
        os.makedirs(d, exist_ok=True)
        files = a["data"].get("files") or {}
        a["files"] = {}
        for name, text in files.items():
            if JAVA_NAME.match(name) and isinstance(text, str):
                with open(os.path.join(d, name), "w", encoding="utf-8") as f:
                    f.write(text)
                a["files"][name] = text
        meta = {k: a[k] for k in ("issue", "url", "github", "created_at", "session", "question", "regno", "name",
                                  "sha256", "accepted", "reason", "problems", "source")}
        with open(os.path.join(d, "submission.json"), "w", encoding="utf-8") as f:
            json.dump({"meta": meta, **a["data"]}, f, ensure_ascii=False)
        a["dir"] = d

    # last accepted attempt per session, question, student
    latest = {}
    for a in attempts:
        if a.get("accepted") and a.get("files") is not None and "dir" in a:
            k = (a["session"], a["question"], a["regno"])
            if k not in latest or a["created_at"] > latest[k]["created_at"]:
                latest[k] = a
    os.environ.pop("LAB_PRIVATE_KEY", None)        # student programs never see the key

    for (session, question, regno), a in sorted(latest.items()):
        tests_dir = os.path.join(tests_root, question)
        key_text = json.dumps(a["files"], sort_keys=True) + tests_hash(tests_dir)
        cache_key = hashlib.sha256(key_text.encode()).hexdigest()
        result_path = os.path.join(a["dir"], "result.json")
        result = None
        if os.path.exists(result_path):
            with open(result_path, encoding="utf-8") as f:
                old = json.load(f)
            if old.get("cache_key") == cache_key:
                result = old
        if result is None:
            work = tempfile.mkdtemp(prefix="lab-")
            try:
                os.makedirs(os.path.join(work, question))
                for name, text in a["files"].items():
                    with open(os.path.join(work, question, name), "w", encoding="utf-8") as f:
                        f.write(text)
                result = grade.grade_exercise(question, work, tests_root, timeout, sandbox)
            finally:
                shutil.rmtree(work, ignore_errors=True)
            result["cache_key"] = cache_key
            with open(result_path, "w", encoding="utf-8") as f:
                json.dump(result, f, indent=1, ensure_ascii=False)
            with open(os.path.join(a["dir"], "result.md"), "w", encoding="utf-8") as f:
                f.write(grade.markdown(result))
        a["result"] = result
        act = a["data"].get("activity") or {}
        code = next(iter(a["files"].values()), "")
        a["summary"] = activity.summarise(act.get("events") or [], code)
        a["flags"] = activity.flags(a["summary"], bool(act.get("trimmed"))) + a["problems"]
        a["fp"] = fingerprint("\n".join(a["files"].values()))
        print("%s %s %s: %s/%s %s" % (session, question, regno, result.get("passed"), result.get("total"), "; ".join(a["flags"])))

    reports(out, attempts, latest, roster)
    return attempts, latest


def reports(out, attempts, latest, roster):
    sessions = sorted({k[0] for k in latest} | {a["session"] for a in attempts if a.get("session")})
    for session in sessions:
        sdir = os.path.join(out, SAFE.sub("_", session))
        mine = {k: a for k, a in latest.items() if k[0] == session}
        questions = sorted({k[1] for k in mine})
        rows = []
        for (s, q, regno), a in sorted(mine.items(), key=lambda kv: (kv[0][2], kv[0][1])):
            r, m = a["result"], a["summary"]
            rows.append([regno, a["name"], a["github"], q, a["issue"], ist(a["created_at"]),
                         "yes" if r.get("compiled") else "no", r.get("passed"), r.get("total"), r.get("score_percent"),
                         m["typedChars"], m["finalChars"], round(m["activeMs"] / 60000, 1), m["pasteBlocked"], m["bulkInserts"],
                         m["awayCount"], round(m["awayMs"] / 60000, 1), m["runs"], m["checks"],
                         "yes" if m["replayMatches"] else "no", "; ".join(a["flags"]), os.path.relpath(a["dir"], sdir)])
        write_csv(os.path.join(sdir, "marks.csv"),
                  ["regno", "name", "github", "question", "issue", "submitted_ist", "compiles", "tests_passed", "tests_total",
                   "score_percent", "typed_chars", "code_chars", "active_min", "paste_blocked", "bulk_inserts",
                   "times_away", "away_min", "runs", "checks", "typing_log_matches_code", "flags", "folder"], rows)
        students = sorted({k[2] for k in mine} | ({r for r in roster} if roster else set()))
        wide = []
        for regno in students:
            scores = [mine[(session, q, regno)]["result"].get("score_percent") if (session, q, regno) in mine else "" for q in questions]
            nums = [x for x in scores if isinstance(x, (int, float))]
            name = next((mine[k]["name"] for k in mine if k[2] == regno), roster.get(regno, {}).get("name", ""))
            wide.append([regno, name] + scores + [round(sum(nums) / len(questions), 1) if questions else ""])
        write_csv(os.path.join(sdir, "marks-wide.csv"), ["regno", "name"] + questions + ["average_percent"], wide)
        sim = []
        for q in questions:
            group = [a for k, a in mine.items() if k[1] == q]
            for x, y in combinations(group, 2):
                v = similarity(x["fp"], y["fp"])
                if v >= 0.8:
                    sim.append([q, x["regno"], y["regno"], round(v, 3)])
        sim.sort(key=lambda r: -r[3])
        write_csv(os.path.join(sdir, "similarity.csv"), ["question", "regno_a", "regno_b", "similarity"], sim)
        rejected = [a for a in attempts if a.get("session") == session and not a.get("accepted")]
        lines = ["# Session %s" % session, "",
                 "Updated %s IST. Marked: the last accepted submission of each student and question." % dt.datetime.now(IST).strftime("%Y-%m-%d %H:%M"), "",
                 "Files: `marks.csv` (one row per student and question, with activity numbers and flags), "
                 "`marks-wide.csv` (one row per student), `similarity.csv` (pairs of programs with 80 %+ of the same token sequences).", "",
                 "| Register no. | Name | Question | Tests | Typed / code chars | Active min | Flags |", "|---|---|---|---|---|---|---|"]
        for row in rows:
            lines.append("| %s | %s | [%s](%s) | %s/%s | %s / %s | %s | %s |" % (row[0], row[1], row[3], row[21].replace(os.sep, "/") + "/result.md",
                                                                          row[7], row[8], row[10], row[11], row[12], row[20] or ""))
        if sim:
            lines += ["", "## Similar programs", "", "| Question | A | B | Similarity |", "|---|---|---|---|"]
            lines += ["| %s | %s | %s | %.2f |" % tuple(r) for r in sim]
        if rejected:
            lines += ["", "## Not accepted", "", "| Issue | GitHub | Register no. | Created (IST) | Reason |", "|---|---|---|---|---|"]
            lines += ["| [#%d](%s) | %s | %s | %s | %s |" % (a["issue"], a["url"], a["github"], a.get("regno", ""), ist(a["created_at"]), a["reason"])
                      for a in rejected]
        with open(os.path.join(sdir, "README.md"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True, help="public lab repository, OWNER/NAME")
    ap.add_argument("--tests", default="tests")
    ap.add_argument("--roster", default="roster.csv")
    ap.add_argument("--out", default="results")
    ap.add_argument("--session", default="")
    ap.add_argument("--sandbox", choices=["docker", "none"], default="docker")
    ap.add_argument("--timeout", type=int, default=10)
    a = ap.parse_args()
    pem = os.environ.get("LAB_PRIVATE_KEY", "")
    if "PRIVATE KEY" not in pem:
        sys.exit("Set LAB_PRIVATE_KEY to the private key (the whole lab-private-key.pem file).")
    key = labcrypto.load_private_key(pem)
    api = Api(a.repo, os.environ.get("GITHUB_TOKEN"))
    attempts, latest = collect(api, key, a.out, a.tests, load_roster(a.roster), a.session.strip(), a.sandbox, a.timeout)
    print("%d submissions read, %d marked" % (len(attempts), len(latest)))


if __name__ == "__main__":
    main()
