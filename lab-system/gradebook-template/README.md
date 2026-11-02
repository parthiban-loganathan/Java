# Java lab gradebook (private, teacher only)

Collects the weekly Java programs of every student, grades them on Java 21 in isolated containers
and keeps a permanent archive with a gradebook you can open in Excel. Full instructions: lab website,
**Guides → 10. Weekly lab submissions and marking**.

| File or folder | What it is | Who edits it |
|----------------|-----------|--------------|
| `assignments.csv` | exercises, week, due date and time (IST, `YYYY-MM-DD HH:MM`), maximum marks | you |
| `roster.csv` | register number, name, GitHub username, repository of every student; filled automatically | the robot (you may correct it) |
| `marks.csv` | your marks and comments: `regno,exercise,mark,comment` | you |
| `tests/<EX>/` | reference test cases (`input-N.txt`, `expected-N.txt`); add folders for your own tasks | you |
| `reference/<EX>/` | reference solutions, used only to compare in the similarity report | you |
| `archive/<date>/<regno>/<EX>/` | each collected program with its result and report, kept forever | the robot |
| `latest/<regno>/<EX>/` | the newest collected copy of each program | the robot |
| `gradebook/` | `summary.csv`, `students.csv`, `missing.csv`, `similarity.csv`, `collection-log.csv` | the robot |
| `grader/` | the grading programs | nobody |

## Run a collection

**Actions → Collect and grade → Run workflow** (optionally a week number). It also runs by itself every
Monday at 00:05 IST. When it finishes, open `gradebook/summary.csv`.

## Give marks

Open `marks.csv`, click the pencil, add one line per mark, for example
`24BCL0001,E01a,9,Clear variable names; handles negative input`, and commit. The gradebook is
rebuilt automatically within a minute.
