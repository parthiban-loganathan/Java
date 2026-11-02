#!/usr/bin/env python3
"""Checks a project-submission issue and prints a Markdown reply for the issue.

Usage: python3 tools/check_submission.py ISSUE_BODY_FILE  (exit status 0 = valid entry)
The JSON entry is taken from the first ```json fenced block of the issue body (the issue form
renders the "Directory entry" field that way). It is validated against the schema together with
the current directory, so duplicate ids are caught too.
"""
import json
import re
import sys

sys.path.insert(0, "tools")
from validate_projects import validate  # noqa: E402


def main(body_file):
    body = open(body_file, encoding="utf-8").read()
    m = re.search(r"```json\s*(\{.*?\})\s*```", body, re.S) or re.search(r"(\{.*\})", body, re.S)
    lines = ["### Automatic check of this submission", ""]
    if not m:
        lines.append("No JSON directory entry was found. Please generate it with the **Submit a project** page "
                     "of the lab website and paste it into the *Directory entry* field.")
        print("\n".join(lines))
        return 1
    try:
        entry = json.loads(m.group(1))
    except json.JSONDecodeError as e:
        lines.append(f"The directory entry is not valid JSON: `{e}`. Generate it again with the Submit a project page.")
        print("\n".join(lines))
        return 1
    data = json.load(open("projects/projects.json", encoding="utf-8"))
    schema = json.load(open("projects/projects.schema.json", encoding="utf-8"))
    data["projects"].append(entry)
    errors = validate(data, schema)
    if errors:
        lines.append("The entry needs changes before it can be added:")
        lines.append("")
        lines += [f"- {e.replace('$.projects[' + str(len(data['projects']) - 1) + ']', 'entry')}" for e in errors]
        lines.append("")
        lines.append("Edit this issue (three dots, Edit) after fixing the entry; the check runs again automatically.")
        print("\n".join(lines))
        return 1
    lines.append("The entry is valid. A maintainer will review the live demo and add it to the directory.")
    lines.append("")
    lines.append("For the maintainer: add this object to `projects/projects.json` (see docs/00-start-here.md, step 9).")
    lines.append("")
    lines.append("```json")
    lines.append(json.dumps(entry, indent=2, ensure_ascii=False))
    lines.append("```")
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
