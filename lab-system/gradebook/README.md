# Java Lab gradebook (PRIVATE repository)

Keep this repository **private**. It holds your hidden test cases, the roster and, after each
collection, every student's decrypted code, typing log and marks.

| Path | What |
|------|------|
| `roster.csv` | `regno,name,github` for every student (GitHub username optional but recommended: a submission from another account is flagged) |
| `tests/<QUESTION>/input-N.txt`, `expected-N.txt` | the cases used for marking (sample + hidden). Course experiments are included; Q01-Q03 are examples |
| `solutions/<QUESTION>/Main.java` | your reference solutions (used only by you to make `expected-N.txt`) |
| `results/<SESSION>/` | written by the workflow: `README.md`, `marks.csv`, `marks-wide.csv`, `similarity.csv` and one folder per question and student |
| `.github/workflows/collect.yml` | the collection workflow |

## Once

1. Secret: Settings, Secrets and variables, Actions, New repository secret: name `LAB_PRIVATE_KEY`, value the whole
   content of `lab-private-key.pem` (made on the website's Teacher tools page).
2. Fill `roster.csv`.

## After every lab session

Actions, **Collect and mark lab submissions**, Run workflow, session (for example `W05`), Run.
When it is green, open `results/W05/README.md`.

In every student folder `results/<SESSION>/<QUESTION>/<REGNO>/<ISSUE>/`:
`Main.java` (the code), `result.md` (the tests), `submission.json` (code and complete typing log: open it on the
Teacher tools page to replay how the program was typed).

## New question with hidden tests

1. Public lab repository: `lab/questions/Q07/README.md` (statement), `input.txt`/`expected.txt` (sample shown to students).
2. Here: `solutions/Q07/Main.java`; put inputs in `tests/Q07/input-1.txt`, `input-2.txt`, ...; make the expected outputs
   with the JDK: `javac -d /tmp/q solutions/Q07/Main.java` and for every input
   `java -cp /tmp/q Main < tests/Q07/input-1.txt > tests/Q07/expected-1.txt`.

Late submission you want to accept: add the label `lab-accept-late` to its issue in the lab repository and run the
workflow again. Test or junk issue: label `lab-ignore`.
