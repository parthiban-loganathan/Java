#!/usr/bin/env python3
"""Tests of the Java Lab submission system (no network, no GitHub).

    python3 tools/lab/test_lab.py [-v]
Needs the cryptography package and Node 20+ (for the browser-module cross-checks). The marking
test runs programs with --sandbox none (trusted reference code) and needs a JDK.
"""
import csv
import datetime as dt
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
import activity  # noqa: E402
import collect_lab  # noqa: E402
import control as ctl  # noqa: E402
import intake  # noqa: E402
import labcrypto  # noqa: E402

UTC = dt.timezone.utc
T = lambda s: dt.datetime.fromisoformat(s).replace(tzinfo=UTC)  # noqa: E731
CODE = open(os.path.join(ROOT, "lab-system", "gradebook", "solutions", "Q01", "Main.java"), encoding="utf-8").read()


def typed_events(code, t0=1000):
    """Events as the editor records them when code is typed line by line."""
    ev, t = [], t0
    for row, line in enumerate(code.split("\n")):
        if row:
            ev.append(["i", t, row - 1, len(code.split("\n")[row - 1]), "\n"])
            t += 300
        if line:
            ev.append(["i", t, row, 0, line])
            t += 100 * len(line)
    return ev


def labcrypto_read(directory):
    with open(os.path.join(directory, "teacher-public-key.pem")) as f:
        return f.read()


def node(*args):
    r = subprocess.run(["node", os.path.join(HERE, "interop.mjs"), *args], capture_output=True, text=True, check=True)
    return json.loads(r.stdout)


class Keys:
    dir = tempfile.mkdtemp(prefix="labkeys-")
    labcrypto.keygen(dir)
    public = open(os.path.join(dir, "teacher-public-key.pem")).read()
    private = labcrypto.load_private_key(open(os.path.join(dir, "lab-private-key.pem")).read())
    private_path = os.path.join(dir, "lab-private-key.pem")


def payload(regno="24BCL0001", session="W01", question="Q01", code=CODE, events=None):
    events = typed_events(code) if events is None else events
    return {"format": 1, "kind": "lab-submission", "session": session, "question": question, "regno": regno,
            "name": "Student " + regno[-4:], "files": {"Main.java": code}, "createdAt": "2026-11-02T09:10:00Z",
            "activity": {"format": 1, "t0": "2026-11-02T09:00:00Z", "events": events, "trimmed": False}}


def issue_body(session, question, armour, receipt=""):
    return ("### Session\n\n%s\n\n### Question\n\n%s\n\n### Receipt\n\n%s\n\n### Package\n\n```text\n%s\n```\n"
            % (session, question, receipt or "_No response_", armour))


class ControlTest(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp()
        self.f = os.path.join(self.d, "control.json")

    def run_ctl(self, *args):
        ctl.main(list(args) + ["--file", self.f, "--root", ROOT])
        return ctl.load(self.f)

    def test_open_close_reopen(self):
        c = self.run_ctl("open", "--session", "W01", "--questions", "Q01 E05a", "--now", "2026-11-02T09:00:00Z")
        self.assertTrue(ctl.decide(c, "W01", "Q01", T("2026-11-02T09:00:00"))[0])
        self.assertTrue(ctl.decide(c, "W01", "E05a", T("2026-11-02T10:59:59"))[0])
        self.assertFalse(ctl.decide(c, "W01", "Q01", T("2026-11-02T08:59:59"))[0])
        self.assertFalse(ctl.decide(c, "W01", "Q02", T("2026-11-02T09:30:00"))[0])
        self.assertFalse(ctl.decide(c, "W02", "Q01", T("2026-11-02T09:30:00"))[0])
        c = self.run_ctl("close", "--now", "2026-11-02T11:00:00Z")
        self.assertTrue(ctl.decide(c, "W01", "Q01", T("2026-11-02T10:59:59"))[0])
        self.assertFalse(ctl.decide(c, "W01", "Q01", T("2026-11-02T11:00:00"))[0])
        c = self.run_ctl("open", "--session", "W01", "--minutes", "15", "--now", "2026-11-03T09:00:00Z")
        self.assertEqual(len(c["sessions"][0]["windows"]), 2)
        self.assertEqual(c["sessions"][0]["questions"], ["Q01", "E05a"])
        self.assertTrue(ctl.decide(c, "W01", "Q01", T("2026-11-03T09:14:59"))[0])
        self.assertFalse(ctl.decide(c, "W01", "Q01", T("2026-11-03T09:15:00"))[0])
        self.assertFalse(ctl.decide(c, "W01", "Q01", T("2026-11-02T12:00:00"))[0])

    def test_extend_open_session_and_close_one(self):
        self.run_ctl("open", "--session", "A", "--questions", "Q01", "--minutes", "10", "--now", "2026-11-02T09:00:00Z")
        self.run_ctl("open", "--session", "B", "--questions", "Q02", "--now", "2026-11-02T09:00:00Z")
        c = self.run_ctl("open", "--session", "A", "--minutes", "60", "--now", "2026-11-02T09:05:00Z")
        self.assertEqual(len(c["sessions"][0]["windows"]), 1)
        self.assertTrue(ctl.decide(c, "A", "Q01", T("2026-11-02T10:04:00"))[0])
        c = self.run_ctl("close", "--session", "B", "--now", "2026-11-02T09:30:00Z")
        self.assertFalse(ctl.decide(c, "B", "Q02", T("2026-11-02T09:31:00"))[0])
        self.assertTrue(ctl.decide(c, "A", "Q01", T("2026-11-02T09:31:00"))[0])

    def test_rejects_bad_input(self):
        with self.assertRaises(SystemExit):
            self.run_ctl("open", "--session", "W 1", "--questions", "Q01")
        with self.assertRaises(SystemExit):
            self.run_ctl("open", "--session", "W01", "--questions", "Q99")
        with self.assertRaises(SystemExit):
            self.run_ctl("open", "--session", "W01")

    def test_shipped_control_file(self):
        c = ctl.load(os.path.join(ROOT, "lab", "control.json"))
        self.assertEqual(c["format"], 1)


class CryptoTest(unittest.TestCase):
    def test_python_round_trip_and_tamper(self):
        b = labcrypto.encrypt(payload(), Keys.public)
        self.assertEqual(labcrypto.decrypt(labcrypto.unarmour("x" + labcrypto.armour(b) + "y"), Keys.private)["files"]["Main.java"], CODE)
        bad = bytearray(b)
        bad[-5] ^= 1
        with self.assertRaises(labcrypto.PackageError):
            labcrypto.decrypt(bytes(bad), Keys.private)
        other = tempfile.mkdtemp()
        labcrypto.keygen(other)
        with self.assertRaises(labcrypto.PackageError):
            labcrypto.decrypt(labcrypto.encrypt(payload(), labcrypto_read(other)), Keys.private)

    @unittest.skipUnless(shutil.which("node"), "node not installed")
    def test_browser_module_interop(self):
        d = tempfile.mkdtemp()
        events = typed_events(CODE) + [["p", 99000, 120, "public class"], ["a", 99100], ["f", 159100],
                                       ["r", 160000, 0, 0, 0, 6], ["i", 160500, 0, 0, "import"], ["x", 161000, "ok", 300], ["c", 162000, 2, 2]]
        p = payload(events=events)
        with open(os.path.join(d, "in.json"), "w") as f:
            json.dump(p, f)
        js = node("encrypt", os.path.join(Keys.dir, "teacher-public-key.pem"), os.path.join(d, "in.json"))
        self.assertEqual(js["keyId"], labcrypto.key_id(Keys.public))
        binary = labcrypto.unarmour(js["armour"])
        self.assertEqual(labcrypto.sha256(binary), js["sha256"])
        self.assertEqual(labcrypto.decrypt(binary, Keys.private), p)
        py = activity.summarise(events, CODE)
        self.assertEqual(js["summary"], py)
        self.assertTrue(py["replayMatches"])
        self.assertEqual(py["pasteBlocked"], 1)
        self.assertEqual(py["awayMs"], 60000)
        self.assertEqual(js["fnv"], activity.fnv(CODE))
        with open(os.path.join(d, "pkg.txt"), "w") as f:
            f.write(labcrypto.armour(labcrypto.encrypt(p, Keys.public)))
        self.assertEqual(node("decrypt", Keys.private_path, os.path.join(d, "pkg.txt"))["data"], p)

    @unittest.skipUnless(shutil.which("node"), "node not installed")
    def test_passphrase_protected_keys(self):
        d = tempfile.mkdtemp()
        js = node("keygen", "a good long passphrase", d)
        pem = open(os.path.join(d, "private.pem")).read()
        self.assertIn("BEGIN ENCRYPTED PRIVATE KEY", pem)
        with self.assertRaises(labcrypto.PackageError):
            labcrypto.load_private_key(pem)                       # no passphrase
        with self.assertRaises(labcrypto.PackageError):
            labcrypto.load_private_key(pem, "wrong passphrase")
        key = labcrypto.load_private_key(pem, "a good long passphrase")
        public = open(os.path.join(d, "public.pem")).read()
        self.assertEqual(labcrypto.key_id(public), js["keyId"])
        self.assertEqual(labcrypto.decrypt(labcrypto.encrypt(payload(), public), key)["regno"], "24BCL0001")
        # a key made by Python with a passphrase opens in the browser code
        p = tempfile.mkdtemp()
        labcrypto.keygen(p, "python passphrase 789")
        with open(os.path.join(p, "lab-private-key.pem")) as f:
            self.assertIn("ENCRYPTED PRIVATE KEY", f.read())
        with open(os.path.join(p, "pkg.txt"), "w") as f:
            f.write(labcrypto.armour(labcrypto.encrypt(payload(), labcrypto_read(p))))
        env = dict(os.environ, PASSPHRASE="python passphrase 789")
        r = subprocess.run(["node", os.path.join(HERE, "interop.mjs"), "decrypt", os.path.join(p, "lab-private-key.pem"),
                            os.path.join(p, "pkg.txt")], capture_output=True, text=True, check=True, env=env)
        self.assertEqual(json.loads(r.stdout)["data"]["regno"], "24BCL0001")

    @unittest.skipUnless(shutil.which("node"), "node not installed")
    def test_replay_multiline_and_unicode(self):
        events = [["i", 1, 0, 0, "ab"], ["i", 2, 0, 1, "X\nY\nZ"], ["r", 3, 1, 0, 2, 1], ["i", 4, 0, 0, "é€😀"], ["r", 5, 0, 2, 0, 5]]
        code = "é€X\nb"          # the emoji is 2 UTF-16 units, as in the browser
        self.assertEqual(activity.replay(events), code)
        d = tempfile.mkdtemp()
        with open(os.path.join(d, "e.json"), "w") as f:
            json.dump(events, f)
        with open(os.path.join(d, "c.txt"), "w", encoding="utf-8") as f:
            f.write(code)
        js = node("summary", os.path.join(d, "e.json"), os.path.join(d, "c.txt"))
        self.assertEqual(js["replay"], code)
        self.assertEqual(js["summary"], activity.summarise(events, code))


class IntakeTest(unittest.TestCase):
    def setUp(self):
        self.control = ctl.empty()
        ctl.do_open(self.control, "W01", "Q01 Q02", 0, "", T("2026-11-02T09:00:00"), ROOT)
        ctl.do_close(self.control, "W01", T("2026-11-02T11:00:00"))
        self.armour = labcrypto.armour(labcrypto.encrypt(payload(), Keys.public))
        self.sha = labcrypto.sha256(labcrypto.unarmour(self.armour))

    def issue(self, created, body=None):
        return {"number": 7, "created_at": created, "body": body if body is not None else issue_body("W01", "Q01", self.armour, self.sha[:16])}

    def test_accept_and_reject_by_time(self):
        r = intake.evaluate(self.issue("2026-11-02T10:59:59Z"), self.control, Keys.public)
        self.assertEqual(r["status"], "accepted", r)
        self.assertEqual(r["notes"], [])
        text = intake.comment_text(r, "Asia/Kolkata")
        self.assertTrue(text.startswith(intake.MARKER))
        self.assertIn("16:29:59 Asia/Kolkata", text)
        self.assertEqual(labcrypto.find_packages(text)[0], labcrypto.unarmour(self.armour))
        r = intake.evaluate(self.issue("2026-11-02T11:00:00Z"), self.control, Keys.public)
        self.assertEqual(r["status"], "rejected")
        self.assertIn("not open", r["reason"])

    def test_problems(self):
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z", issue_body("W01", "Q01", "")), self.control, Keys.public)
        self.assertEqual((r["status"], r["reason"]), ("rejected", "no package was pasted into the issue"))
        cut = self.armour[:len(self.armour) // 2] + "\n" + labcrypto.END
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z", issue_body("W01", "Q01", cut)), self.control, Keys.public)
        self.assertIn("damaged", r["reason"])
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z", issue_body("@team <b>W01</b>", "Q01", self.armour)), self.control, Keys.public)
        self.assertEqual(r["session"], "teambW01b")
        self.assertNotIn("@", intake.comment_text(r, "UTC"))
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z", issue_body("W01", "Q09", self.armour)), self.control, Keys.public)
        self.assertIn("not part of session", r["reason"])
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z", issue_body("W01", "Q01", self.armour, "0" * 16)), self.control, Keys.public)
        self.assertEqual(r["status"], "accepted")
        self.assertIn("receipt", r["notes"][0])
        other = tempfile.mkdtemp()
        labcrypto.keygen(other)
        r = intake.evaluate(self.issue("2026-11-02T10:00:00Z"), self.control, labcrypto_read(other))
        self.assertIn("old teacher key", r["reason"])


class FakeApi:
    def __init__(self, control, issues, comments):
        self._control, self._issues, self._comments = control, issues, comments

    def control(self):
        return self._control

    def issues(self):
        return iter(self._issues)

    def comments(self, n):
        return self._comments.get(n, [])


@unittest.skipUnless(shutil.which("javac"), "JDK not installed")
class CollectTest(unittest.TestCase):
    def test_collect_and_mark(self):
        control = ctl.empty()
        ctl.do_open(control, "W01", "Q01 Q02", 0, "", T("2026-11-02T09:00:00"), ROOT)
        ctl.do_close(control, "W01", T("2026-11-02T11:00:00"))
        wrong = CODE.replace("q < 1 ?", "q < 2 ?")
        copied = CODE.replace("double q", "double  q")
        cases = [  # (issue, regno, created, code, events, labels, bot)
            (1, "24BCL0001", "2026-11-02T09:30:00Z", wrong, None, [], True),
            (2, "24BCL0001", "2026-11-02T10:30:00Z", CODE, None, [], True),        # later: replaces issue 1
            (3, "24BCL0002", "2026-11-02T10:40:00Z", copied, [["p", 5, len(copied), copied[:200]], ["B", 6, len(copied)], ["i", 6, 0, 0, copied]], [], True),
            (4, "24BCL0003", "2026-11-02T11:05:00Z", CODE, None, [], True),        # after close
            (5, "24BCL0004", "2026-11-02T11:10:00Z", CODE, None, ["lab-accept-late"], True),
            (6, "24BCL0005", "2026-11-02T10:00:00Z", CODE, None, ["lab-ignore"], True),
            (7, "24BCL0006", "2026-11-02T10:00:00Z", CODE, None, [], False),       # no bot copy
        ]
        issues, comments = [], {}
        for n, regno, created, code, events, labels, bot in cases:
            armour = labcrypto.armour(labcrypto.encrypt(payload(regno=regno, code=code, events=events), Keys.public))
            issues.append({"number": n, "html_url": "https://github.com/o/r/issues/%d" % n, "user": {"login": "gh%d" % n},
                           "created_at": created, "labels": [{"name": l} for l in ["lab-submission"] + labels],
                           "body": issue_body("W01", "Q01", armour)})
            if bot:
                comments[n] = [{"user": {"login": "github-actions[bot]"}, "body": intake.MARKER + "{} -->\n```text\n" + armour + "\n```"}]
        out = tempfile.mkdtemp()
        roster = {"24BCL0001": {"name": "A", "github": "gh2"}, "24BCL0002": {"name": "B", "github": "someone-else"},
                  "24BCL0004": {"name": "D", "github": ""}, "24BCL0006": {"name": "F", "github": ""}}
        tests = os.path.join(ROOT, "lab-system", "gradebook", "tests")
        attempts, latest = collect_lab.collect(FakeApi(control, issues, comments), Keys.private, out, tests, roster, "", "none", 10)
        self.assertEqual(sorted(k[2] for k in latest), ["24BCL0001", "24BCL0002", "24BCL0004", "24BCL0006"])
        self.assertEqual(latest[("W01", "Q01", "24BCL0001")]["issue"], 2)
        rows = {r["regno"]: r for r in csv.DictReader(open(os.path.join(out, "W01", "marks.csv")))}
        self.assertEqual((rows["24BCL0001"]["tests_passed"], rows["24BCL0001"]["tests_total"]), ("6", "6"))
        self.assertEqual(rows["24BCL0001"]["flags"], "")
        self.assertIn("paste attempt", rows["24BCL0002"]["flags"])
        self.assertIn("large insertion", rows["24BCL0002"]["flags"])
        self.assertIn("roster says someone-else", rows["24BCL0002"]["flags"])
        self.assertIn("no bot copy", rows["24BCL0006"]["flags"])
        self.assertNotIn("24BCL0003", rows)
        self.assertNotIn("24BCL0005", rows)
        self.assertTrue(os.path.exists(os.path.join(out, "W01", "Q01", "24BCL0001", "1", "Main.java")))
        sim = list(csv.DictReader(open(os.path.join(out, "W01", "similarity.csv"))))
        self.assertTrue(any({s["regno_a"], s["regno_b"]} == {"24BCL0001", "24BCL0002"} for s in sim))
        readme = open(os.path.join(out, "W01", "README.md")).read()
        self.assertIn("24BCL0003", readme)                  # listed under Not accepted
        sub = json.load(open(os.path.join(out, "W01", "Q01", "24BCL0001", "2", "submission.json")))
        self.assertEqual(sub["files"]["Main.java"], CODE)
        # second run uses the cached marks
        collect_lab.collect(FakeApi(control, issues, comments), Keys.private, out, tests, roster, "", "none", 10)


if __name__ == "__main__":
    unittest.main()
