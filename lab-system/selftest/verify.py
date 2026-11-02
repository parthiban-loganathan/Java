#!/usr/bin/env python3
"""Asserts the grader's verdicts on the self-test lab (see make_samples.sh)."""
import glob
import json
import os
import sys

out = sys.argv[1]
res = {os.path.basename(p)[:-5]: json.load(open(p)) for p in glob.glob(os.path.join(out, "*.json")) if not p.endswith("summary.json")}
problems = []
def expect(cond, msg):
    if not cond:
        problems.append(msg)

for ex, r in res.items():
    if ex.startswith(("E", "L")):
        expect(r["compiled"] and r["passed"] == r["total"] and r["total"] > 0, f"{ex}: reference solution should pass all tests, got {r.get('passed')}/{r.get('total')}")
expect(res["X-wrong"]["compiled"] and res["X-wrong"]["passed"] == 0, "X-wrong must fail")
expect(not res["X-nocompile"]["compiled"] and "Main.java:1" in res["X-nocompile"]["compile_output"], "X-nocompile must report Main.java:1")
expect(res["X-loop"]["cases"][0]["timed_out"], "X-loop must hit the time limit")
expect(res["X-flood"]["passed"] == 0 and len(res["X-flood"]["cases"][0]["actual"]) <= 20000, "X-flood output must be limited")
net = res["X-network"]["cases"][0]
expect("NETWORK REACHED" not in net["actual"], "X-network must not reach the internet")
sec = res["X-readsecrets"]["cases"][0]["actual"]
expect("GITHUB_TOKEN" not in sec and "ACTIONS_RUNTIME_TOKEN" not in sec and "ghp_" not in sec, "X-readsecrets must not see tokens")
expect("src read-only" in sec, "X-readsecrets: source folder must be read-only")
expect(res["X-forkbomb"]["passed"] == 0, "X-forkbomb must be contained")
expect(res["X-renamed"]["passed"] == res["X-renamed"]["total"] == 1, "X-renamed (public class Interest) must run")
for p in problems:
    print("FAIL", p)
print(f"{len(res)} programs checked, {len(problems)} problems")
sys.exit(1 if problems else 0)
