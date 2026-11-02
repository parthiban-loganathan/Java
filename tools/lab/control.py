#!/usr/bin/env python3
"""Open and close lab assessments: lab/control.json (format 1).

    {"format": 1, "updated_at": "...Z",
     "settings": {"regnoPattern": "^[0-9]{2}[A-Z]{3}[0-9]{4}$"},
     "sessions": [{"id": "W05", "title": "...", "questions": ["Q01", "E05a"],
                   "windows": [{"opened_at": "...Z", "closes_at": "...Z" or null, "closed_at": "...Z" or null}]}]}

A submission is accepted when GitHub's creation time of its issue lies in a window of its session
(opened_at <= time < the earlier of closed_at and closes_at) and the question belongs to the session.
The Java Lab page and tools/lab/intake.py and collect_lab.py all use these rules.

    python3 tools/lab/control.py open  --session W05 --questions "Q01 E05a" [--minutes 120] [--title "Week 5"]
    python3 tools/lab/control.py close [--session W05]        (no --session: close every open session)
    python3 tools/lab/control.py status
Options: --file lab/control.json, --now 2026-11-02T09:00:00Z (default: current time), --root (repository root).
"""
import argparse
import datetime as dt
import json
import os
import re
import sys

ID = re.compile(r"^[A-Za-z0-9_-]{1,32}$")
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def parse_time(text):
    return dt.datetime.fromisoformat(text.replace("Z", "+00:00")).astimezone(dt.timezone.utc)


def fmt_time(t):
    return t.astimezone(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def empty():
    return {"format": 1, "updated_at": None, "settings": {"regnoPattern": "^[0-9]{2}[A-Z]{3}[0-9]{4}$"}, "sessions": []}


def load(path):
    if not os.path.exists(path):
        return empty()
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data.setdefault("sessions", [])
    data.setdefault("settings", empty()["settings"])
    return data


def save(path, data):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def window_end(w):
    ends = [parse_time(w[k]) for k in ("closed_at", "closes_at") if w.get(k)]
    return min(ends) if ends else None


def in_window(w, t):
    end = window_end(w)
    return parse_time(w["opened_at"]) <= t and (end is None or t < end)


def find(data, session_id):
    return next((s for s in data["sessions"] if s["id"] == session_id), None)


def open_windows(data, now):
    return [(s, w) for s in data["sessions"] for w in s.get("windows", []) if in_window(w, now)]


def decide(data, session_id, question, created_at):
    """(accepted, reason) for a submission whose issue GitHub created at created_at (datetime)."""
    s = find(data, session_id or "")
    if s is None:
        return False, "unknown session %r" % session_id
    if question not in s.get("questions", []):
        return False, "question %r is not part of session %s (questions: %s)" % (question, s["id"], ", ".join(s.get("questions", [])))
    for w in s.get("windows", []):
        if in_window(w, created_at):
            return True, "inside the window opened at %s" % w["opened_at"]
    return False, "session %s was not open at %s" % (s["id"], fmt_time(created_at))


def question_exists(root, qid):
    if os.path.isfile(os.path.join(root, "lab", "questions", qid, "README.md")):
        return True
    for group in ("lessons", "experiments"):
        d = os.path.join(root, "course", group)
        if os.path.isdir(d) and any(f.split("-")[0] == qid for f in os.listdir(d)):
            return True
    return False


def do_open(data, session_id, questions, minutes, title, now, root):
    if not ID.match(session_id or ""):
        raise SystemExit("Session name must be 1-32 letters, digits, - or _ (for example W05).")
    s = find(data, session_id)
    qs = [q for q in re.split(r"[\s,]+", questions or "") if q]
    for q in qs:
        if not ID.match(q):
            raise SystemExit("Question id %r is not valid." % q)
        if not question_exists(root, q):
            raise SystemExit("Question %s not found: add lab/questions/%s/README.md or use a course id such as E05a." % (q, q))
    if s is None:
        if not qs:
            raise SystemExit("A new session needs its questions, for example: Q01 Q02 or E05a.")
        s = {"id": session_id, "title": title or "", "questions": qs, "windows": []}
        data["sessions"].append(s)
    else:
        if qs:
            s["questions"] = qs
        if title:
            s["title"] = title
    closes = fmt_time(now + dt.timedelta(minutes=minutes)) if minutes and minutes > 0 else None
    current = next((w for w in s["windows"] if in_window(w, now)), None)
    if current:                                  # already open: change the automatic closing time
        current["closes_at"] = closes
        return "Session %s was already open; questions %s; closes %s." % (session_id, " ".join(s["questions"]), closes or "when you close it")
    s["windows"].append({"opened_at": fmt_time(now), "closes_at": closes, "closed_at": None})
    return "Session %s OPEN from %s; questions %s; closes %s." % (session_id, fmt_time(now), " ".join(s["questions"]), closes or "when you close it")


def do_close(data, session_id, now):
    closed = []
    for s, w in open_windows(data, now):
        if session_id and s["id"] != session_id:
            continue
        w["closed_at"] = fmt_time(now)
        closed.append(s["id"])
    if session_id and find(data, session_id) is None:
        raise SystemExit("Unknown session %s." % session_id)
    return ("Closed %s at %s." % (", ".join(closed), fmt_time(now))) if closed else "Nothing was open."


def status(data, now):
    lines = []
    for s in data["sessions"]:
        state = "OPEN" if any(in_window(w, now) for w in s.get("windows", [])) else "closed"
        lines.append("%-8s %-6s questions: %s  windows: %d  %s" % (s["id"], state, " ".join(s.get("questions", [])),
                                                                   len(s.get("windows", [])), s.get("title", "")))
    return "\n".join(lines) or "No sessions yet."


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("action", choices=["open", "close", "status"])
    ap.add_argument("--session", default="")
    ap.add_argument("--questions", default="")
    ap.add_argument("--minutes", type=int, default=0)
    ap.add_argument("--title", default="")
    ap.add_argument("--file", default=os.path.join(ROOT, "lab", "control.json"))
    ap.add_argument("--root", default=ROOT)
    ap.add_argument("--now", default="")
    a = ap.parse_args(argv)
    now = parse_time(a.now) if a.now else dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    data = load(a.file)
    if a.action == "status":
        print(status(data, now))
        return
    if a.action == "open":
        msg = do_open(data, a.session.strip(), a.questions, a.minutes, a.title.strip(), now, a.root)
    else:
        msg = do_close(data, a.session.strip(), now)
    data["updated_at"] = fmt_time(now)
    save(a.file, data)
    print(msg)
    print(status(data, now))


if __name__ == "__main__":
    main()
