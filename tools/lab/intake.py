#!/usr/bin/env python3
"""Checks a new "Lab submission" issue (workflow .github/workflows/lab-intake.yml).

1. Finds the encrypted package in the issue (it cannot be read here: no private key exists in
   this public repository) and checks that it is complete and made for the current teacher key.
2. Waits for a running "Lab session control" workflow, then reads lab/control.json.
3. Accepts the submission when GitHub's creation time of the issue lies in an open window of the
   session (tools/lab/control.py), otherwise rejects it.
4. Replies with a comment that keeps a copy of the package (students cannot edit that comment,
   so later edits of the issue change nothing), labels the issue lab-accepted or lab-rejected,
   closes and locks it.
Only the Python standard library is used. Environment: GITHUB_TOKEN, GITHUB_REPOSITORY, GITHUB_EVENT_PATH.
"""
import datetime as dt
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import control as ctl  # noqa: E402
import labcrypto  # noqa: E402

MARKER = "<!-- lab-intake v1 "
LABELS = {"lab-submission": ("0b6e8a", "Java Lab submission"),
          "lab-accepted": ("1d7a46", "Lab submission accepted"),
          "lab-rejected": ("b3261e", "Lab submission rejected")}


def field(body, label):
    """Value of a section of an issue form ("### Label")."""
    m = re.search(r"^###\s+" + re.escape(label) + r"\s*$(.*?)(?=^###\s|\Z)", body or "", re.S | re.M)
    if not m:
        return ""
    v = m.group(1).strip()
    v = re.sub(r"^```\w*\s*|\s*```$", "", v).strip()
    return "" if v == "_No response_" else v


def local_time(t, zone):
    try:
        from zoneinfo import ZoneInfo
        return t.astimezone(ZoneInfo(zone)).strftime("%d %b %Y %H:%M:%S ") + zone
    except Exception:
        return ctl.fmt_time(t)


def evaluate(issue, control, public_pem):
    """Decision for one issue: dict(status, reason, session, question, sha256, package, notes)."""
    body = issue.get("body") or ""
    session = field(body, "Session").strip()
    question = field(body, "Question").strip()
    receipt = field(body, "Receipt").strip().lower()
    created = ctl.parse_time(issue["created_at"])
    result = {"session": session, "question": question, "created_at": issue["created_at"],
              "sha256": None, "package": None, "notes": []}
    packages = labcrypto.find_packages(body)
    if not packages:
        has_begin = labcrypto.BEGIN in body
        result.update(status="rejected", reason="the package is damaged or incomplete (copy it again with the Copy package button)"
                      if has_begin else "no package was pasted into the issue")
        return result
    binary = packages[-1]
    result["sha256"] = labcrypto.sha256(binary)
    result["package"] = labcrypto.armour(binary)
    if len(packages) > 1:
        result["notes"].append("the issue contains %d packages; the last one is used" % len(packages))
    if receipt and receipt != result["sha256"][:16]:
        result["notes"].append("the receipt %s does not match this package (%s): was an older package pasted?"
                               % (receipt, result["sha256"][:16]))
    expected_key = labcrypto.key_id(public_pem) if public_pem else None
    if expected_key and labcrypto.package_key_id(binary) != expected_key:
        result.update(status="rejected", reason="the package was made with an old teacher key: reload the Java Lab page and submit again")
        return result
    ok, reason = ctl.decide(control, session, question, created)
    result.update(status="accepted" if ok else "rejected", reason=reason)
    return result


def comment_text(result, zone):
    meta = {k: result[k] for k in ("status", "session", "question", "created_at", "sha256")}
    when = local_time(ctl.parse_time(result["created_at"]), zone)
    lines = [MARKER + json.dumps(meta) + " -->"]
    if result["status"] == "accepted":
        lines.append("**Accepted.** Session %s, question %s, submitted %s." % (result["session"], result["question"], when))
    else:
        lines.append("**Not accepted:** %s. (Submitted %s.)" % (result["reason"], when))
    if result["sha256"]:
        lines.append("")
        lines.append("Receipt `%s`. The content is encrypted; only the teacher can read it." % result["sha256"][:16])
    for n in result["notes"]:
        lines += ["", "Note: " + n + "."]
    if result["package"]:
        lines += ["", "<details><summary>Copy of the package kept for marking</summary>", "", "```text",
                  result["package"], "```", "", "</details>"]
    return "\n".join(lines)


class GitHub:
    def __init__(self, repo, token, api="https://api.github.com"):
        self.repo, self.token, self.api = repo, token, api

    def call(self, method, path, data=None, accept="application/vnd.github+json"):
        req = urllib.request.Request(self.api + path, method=method,
                                     data=None if data is None else json.dumps(data).encode(),
                                     headers={"Authorization": "Bearer " + self.token, "Accept": accept,
                                              "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "lab-intake"})
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read()
            if accept.endswith("raw"):
                return raw.decode("utf-8")
            return json.loads(raw) if raw else None

    def control_runs_active(self):
        n = 0
        for status in ("queued", "in_progress"):
            try:
                r = self.call("GET", "/repos/%s/actions/workflows/lab-control.yml/runs?status=%s&per_page=5" % (self.repo, status))
                n += r.get("total_count", 0)
            except urllib.error.HTTPError:
                pass
        return n

    def control(self, branch):
        try:
            text = self.call("GET", "/repos/%s/contents/lab/control.json?ref=%s" % (self.repo, branch),
                             accept="application/vnd.github.raw")
            return json.loads(text)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return ctl.empty()
            raise

    def ensure_labels(self):
        for name, (color, desc) in LABELS.items():
            try:
                self.call("POST", "/repos/%s/labels" % self.repo, {"name": name, "color": color, "description": desc})
            except urllib.error.HTTPError as e:
                if e.code != 422:     # 422: exists already
                    raise


def main():
    with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as f:
        event = json.load(f)
    issue = event["issue"]
    names = [l["name"] for l in issue.get("labels", [])]
    if "lab-submission" not in names and not (issue.get("title") or "").startswith("[Lab]"):
        print("not a lab submission")
        return
    gh = GitHub(os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_TOKEN"])
    branch = event.get("repository", {}).get("default_branch", "main")
    for _ in range(18):                       # a close or open that is being written decides first
        if not gh.control_runs_active():
            break
        print("waiting for the Lab session control workflow...")
        time.sleep(10)
    control = gh.control(branch)
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    key_path = os.path.join(root, "lab", "teacher-public-key.pem")
    public_pem = open(key_path, encoding="utf-8").read() if os.path.exists(key_path) else None
    result = evaluate(issue, control, public_pem)
    zone = control.get("settings", {}).get("timeZone", "Asia/Kolkata")
    print(json.dumps({k: v for k, v in result.items() if k != "package"}))
    num = issue["number"]
    gh.ensure_labels()
    gh.call("POST", "/repos/%s/issues/%d/comments" % (gh.repo, num), {"body": comment_text(result, zone)})
    gh.call("POST", "/repos/%s/issues/%d/labels" % (gh.repo, num),
            {"labels": ["lab-submission", "lab-accepted" if result["status"] == "accepted" else "lab-rejected"]})
    gh.call("PATCH", "/repos/%s/issues/%d" % (gh.repo, num),
            {"state": "closed", "state_reason": "completed" if result["status"] == "accepted" else "not_planned"})
    try:
        gh.call("PUT", "/repos/%s/issues/%d/lock" % (gh.repo, num), {"lock_reason": "resolved"})
    except urllib.error.HTTPError as e:
        print("could not lock:", e.code)


if __name__ == "__main__":
    main()
