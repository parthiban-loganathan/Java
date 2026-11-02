# Java Lab: status, questions and submission key

| File | Written by | Purpose |
|------|-----------|---------|
| `control.json` | the workflow **Lab session control** (do not edit by hand during a session) | which sessions are open, their questions and time windows |
| `teacher-public-key.pem` | the teacher, once (Teacher tools page) | students' submissions are encrypted with it |
| `questions/<ID>/README.md` | the teacher | statement shown on the Java Lab page |
| `questions/<ID>/input.txt`, `expected.txt` (`-2`, `-3`, ...) | the teacher | sample cases shown to students and used by **Check** |

Hidden marking cases and reference solutions belong in the private gradebook repository, never here: this
repository is public. Course programs (E01a ... E15, L01 ... L14) can also be used as questions by their id.
See [docs/10-lab-assessments.md](../docs/10-lab-assessments.md).
