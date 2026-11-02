# Contributing

## Students: add your team's project

Your project lives in **your own** repository, created from the CivilTech Innovation Starter. To
list it on the lab website, open a pull request that adds one entry to `projects/projects.json`;
the full procedure is in [`docs/04-submit-a-project.md`](docs/04-submit-a-project.md). Validate
before pushing:

```bash
python3 tools/validate_projects.py
```

## Improving the lab itself

* Course material: each program folder needs `README.md`, the `.java` file(s), `input*.txt`,
  `expected*.txt` (produced by JDK 21) and `meta.properties`. Run `java tools/CourseCheck.java`.
* Core library: add a JUnit test with every change (`mvn -f java/pom.xml test`).
* Website: run `bash tools/build-site.sh` and check the page on a phone-sized window.
* Keep libraries vendored and pinned; do not add services that need accounts or payment.

## Rules

* Only openly licensed data, with source and licence written down.
* No passwords, keys or personal data in commits.
* AI assistance is allowed; you must understand and be able to explain what you submit.
