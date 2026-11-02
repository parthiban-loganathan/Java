#!/usr/bin/env python3
"""Writes problems/README.md (readable on GitHub) from problems/problems.json.
   python3 tools/render_problem_bank.py          # write
   python3 tools/render_problem_bank.py --check  # fail if README.md is out of date
"""
import json
import sys

def render(doc):
    out = ["# Problem bank: 100 real civil, environmental and climate problems", "",
           doc["description"], "",
           "Browse, filter and claim them on the lab website (page **Problems**). To work on one:", "",
           "1. Open a **Project proposal** issue with the problem ID (the website's *Claim* button fills it in).",
           "2. Create your repository from the CivilTech Innovation Starter and build the app.",
           "3. Submit it with the website's **Submit a project** page.", "",
           "Levels: Beginner (formulas, loops, arrays), Intermediate (classes, files, several methods), "
           "Advanced (algorithms, large datasets, numerical methods).", ""]
    cats = []
    for p in doc["problems"]:
        if p["category"] not in cats:
            cats.append(p["category"])
    out.append("| ID | Problem | Category | Level |")
    out.append("|----|---------|----------|-------|")
    for p in doc["problems"]:
        out.append(f"| [{p['id']}](#{p['id'].lower()}) | {p['title']} | {p['category']} | {p['level']} |")
    for c in cats:
        out += ["", f"## {c}"]
        for p in [q for q in doc["problems"] if q["category"] == c]:
            data = "; ".join(f"[{d['name']}]({d['url']})" if d["url"] else d["name"] for d in p["data"])
            out += ["", f"### {p['id']}", "", f"**{p['title']}** ({p['level']})", "",
                    f"- **Problem:** {p['problem']}",
                    f"- **Who benefits:** {p['users']}",
                    f"- **Method:** {p['method']}",
                    f"- **Open data:** {data}",
                    f"- **Java practised:** {', '.join(p['javaConcepts'])}",
                    f"- **The app shows:** {p['appShows']}",
                    f"- **Stretch goal:** {p['stretch']}"]
    out += ["", "Datasets marked as team field measurements are collected by the students; publish them with the app "
            "and state how they were measured.", ""]
    return "\n".join(out)

if __name__ == "__main__":
    doc = json.load(open("problems/problems.json", encoding="utf-8"))
    text = render(doc)
    if "--check" in sys.argv:
        if open("problems/README.md", encoding="utf-8").read() != text:
            print("problems/README.md is out of date: run python3 tools/render_problem_bank.py")
            sys.exit(1)
        print("problems/README.md is up to date")
    else:
        open("problems/README.md", "w", encoding="utf-8").write(text)
        print(f"wrote problems/README.md ({len(doc['problems'])} problems)")
