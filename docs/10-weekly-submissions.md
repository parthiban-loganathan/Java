# 10. Weekly lab submissions and marking

Every week each student pastes a Java program into the **Weekly lab** page of the website and saves it,
with their name and register number, in **their own private GitHub repository**. GitHub compiles and
tests it on Java 21 within about a minute and writes the result into that repository. At each deadline
your private **gradebook** repository collects everyone's latest programs, grades them again with its own
copy of the tests, keeps a permanent archive, and produces mark sheets that open in Excel.

```
 student: Weekly lab page ──save──▶ private repo java-lab-<regno> ──Actions──▶ results/ (tests passed, output)
                                              │  (teacher is a collaborator)
 teacher: gradebook repo ◀──collect at deadline┘ ──▶ archive/ + gradebook/summary.csv + similarity.csv + your marks.csv
```

## Why private repositories, one per student

| Requirement | How it is met |
|-------------|---------------|
| Students cannot see or copy each other's code | each repository is private; only the student and you have access |
| Register numbers and names are not public | they are stored only in private repositories |
| A log of all code | every save is a commit with date and time; each collection is archived permanently in the gradebook |
| Real Java, not an imitation | Java 21 (Eclipse Temurin) in a container on GitHub's servers |
| Fair, safe grading | your gradebook re-tests with its own copy of the tests; student code runs with no network, no credentials, limited memory, processes and 10 s per test |
| Free | private repositories and GitHub Actions minutes are included in free accounts (see *Limits*) |

A static website cannot compile Java typed by a user: the free in-browser Java compilers are either
unmaintained or proprietary. Compilation therefore happens on GitHub, which is why results take about a minute.

---

## Part A. Teacher set-up (once, about 45 minutes)

### A1. Create the student template repository

1. Download `java-lab-student.zip` from the website: `https://parthiban-loganathan.github.io/Java/downloads/java-lab-student.zip`.
   Unzip it.
2. GitHub **+ → New repository**: name `java-lab-student`, **Public** (it contains no student data, only the
   template), no README. **Create repository**.
3. Click **uploading an existing file**, drag in everything inside the unzipped folder, **Commit changes**.
4. Check that `.github` arrived. If not: **Add file → Create new file**, name `.github/workflows/check.yml`, paste the
   content of `lab-system/student-template/.github/workflows/check.yml` from the lab repository, commit.
5. **Settings → General → tick Template repository**.

The website's **Weekly lab** page already points to `parthiban-loganathan/java-lab-student`. If you use another
account or organization, change `LAB` at the top of `site/assets/js/common.js` in the lab repository.

### A2. Create your private gradebook repository

1. Download `java-lab-gradebook.zip` from the website's `downloads/` folder and unzip it.
2. **+ → New repository**: name `java-lab-gradebook`, **Private**, no README. Upload everything inside the folder as
   in A1; check that `.github/workflows/collect.yml` arrived.
3. Open `assignments.csv` (pencil) and fill the `due_ist` column for each exercise, e.g. `2026-12-07 23:59`. Adjust weeks and
   maximum marks if needed. Commit.

### A3. Give the gradebook permission to read the students' repositories

1. Click your profile picture → **Settings → Developer settings → Personal access tokens → Tokens (classic) →
   Generate new token (classic)**.
2. Note: `java-lab-gradebook`; expiration: the end of the semester; tick the scope **repo** (needed to read private
   repositories you are invited to and to accept the invitations). **Generate token** and copy it (it is shown once).
3. In `java-lab-gradebook`: **Settings → Secrets and variables → Actions → New repository secret**.
   Name `STUDENT_REPOS_TOKEN`, paste the token, **Add secret**.

The token never leaves the secret store: the workflow uses it only to download repositories, and student programs
run in containers that cannot see it.

---

## Part B. Student set-up (once, in class, 10 minutes)

On the website open **Weekly lab** and follow *First time?*:

1. Create the repository from the template: name `java-lab-<register number in lower case>`, **Private**.
2. Enter name, register number and repository on the page; press **Open student.json**, paste the text the page
   shows, commit.
3. Press **Add my teacher** → **Add people** → your GitHub username. You will receive invitations by e-mail; you
   do **not** need to accept them one by one: the gradebook accepts every invitation to a repository whose name
   starts with `java-lab` and adds the student to `roster.csv` automatically.

## Part C. Every week (students)

1. **Weekly lab** page → choose the exercise → paste the program → **Save to my repository**.
2. On GitHub press **Commit changes** (if the editor is empty, Ctrl+V: the page has copied the code).
3. After about a minute, **Result of this exercise** shows the tests passed and, for failed tests, their output beside
   the expected output. They may correct and commit again until the deadline.

For your own tasks (not in the course), give students a code such as `W05-Q1`; they choose *Other* and type it.

## Part D. Collecting and marking (teacher)

### Collect

Runs automatically every Monday at 00:05 IST. To run now: `java-lab-gradebook` → **Actions → Collect and grade →
Run workflow** (optionally type a week number) → **Run workflow**. A class of 60 takes a few minutes.

### Read the results

Open `gradebook/` in the repository; GitHub shows each CSV as a table, and **Download raw file** gives a file Excel
opens directly.

| File | Use |
|------|-----|
| `summary.csv` | one row per student and exercise: status, tests passed, your mark, late, path of the archived code |
| `students.csv` | one row per student: number submitted, average test score, total of your marks |
| `missing.csv` | who has not submitted what |
| `similarity.csv` | pairs of programs with the same structure even if variable names differ. A reason to look closer, not proof; programs copied from the handout are naturally similar (see the `vs_reference` columns) |
| `collection-log.csv` | each repository in each run: collected, not accessible, `student.json` mismatch |

To read a student's code: follow the `code` path in `summary.csv`, e.g. `archive/2026-12-08_0005/24BCL0001/E01a/`,
which contains the program, `report.md` (tests) and `result.json`.

### Give marks

Edit `marks.csv` (pencil), one line per mark, then commit:

```text
regno,exercise,mark,comment
24BCL0001,E01a,9,Clear names; handles negative input
24BCL0002,E01a,6,Works but no comments; explain Math.abs in viva
```

The gradebook is rebuilt automatically a minute later with your marks in `summary.csv` and `students.csv`.

### Late work

`late` compares the time of the student's last commit to that exercise with `due_ist`. Commits made in the browser
carry GitHub's server time. A student working with Git on their own computer could set a false date, so the reliable
rule is the scheduled collection itself: the Monday 00:05 snapshot is what was submitted by the deadline.

### Your own tests for new tasks

Create `tests/W05-Q1/input-1.txt` and `tests/W05-Q1/expected-1.txt` (and `-2`, `-3`, ...) in the gradebook
repository (**Add file → Create new file**), and add a row to `assignments.csv`. Produce the expected output by running
your own solution on Java 21. To give students the same automatic feedback, add the same folder under
`lab-tools/tests/` in the `java-lab-student` template before they create their repositories (repositories created
earlier keep the old tests; the gradebook's tests always decide).

## Limits and costs

* Each student's checks run in the student's own account: GitHub Free includes 2,000 Actions minutes per month for
  private repositories, far more than needed (about 1 minute per save).
* Collections run in your account: about 5 seconds per program; a weekly collection of 60 students × 4 exercises
  uses roughly 20-30 minutes of your 2,000.
* Private repositories of free accounts cannot publish GitHub Pages; none are needed here.
* If a student deletes their repository or removes you, `collection-log.csv` shows "clone failed"; everything
  collected earlier stays in your archive.

## Optional: GitHub Classroom

GitHub Classroom (free for teachers) can create the private student repositories for you from the
`java-lab-student` template: create a classroom, an individual assignment named `java-lab`, starter code
`java-lab-student`, private repositories. Students then accept an invitation link instead of steps B1-B3. Their
repositories live in your organization and are named `java-lab-<username>`; the gradebook discovers them the same way,
because their names start with `java-lab`. Students still fill `student.json`.
