# 10. Java Lab assessments: set up, open, close, collect, mark

Everything runs on free, open-source pieces and free GitHub services: no server of your own, nothing to install.

| Piece | What it does | Where |
|-------|-------------|-------|
| **Java Lab page** | Students type a program (pasting and dropping text are blocked and recorded), compile and run it **inside the browser** (OpenJDK javac + TeaVM compiled to WebAssembly), check it against sample cases and submit it with register number and name | `lab.html` on the website |
| **Lab session control** | You open an assessment with its questions and close it, each with one button | Actions tab of this repository |
| **Lab submission check** | Every submission becomes a GitHub issue; this robot accepts it if GitHub's time of the issue lies inside an open window, keeps an unchangeable copy, then closes and locks the issue | Actions tab, runs by itself |
| **Private gradebook** | Your private repository: decrypts every submission, re-runs it with your hidden test cases in isolated containers, summarises the typing log, compares programs and writes mark sheets | your repository `java-lab-gradebook` |
| **Teacher tools page** | Creates your key pair; opens any submission and replays how it was typed | `teacher.html` on the website |

**Privacy.** This repository is public, so anyone can read its source code, the questions and the sample cases.
Students' code, names and register numbers are never readable in it: each submission is encrypted in the
student's browser with your public key (RSA-OAEP 4096 + AES-256-GCM) and only your private key opens it. The
issue shows only the student's GitHub username, the session and the question. Hidden test cases, reference
solutions, the roster and all marks live only in your private gradebook.

---

## Part A. One-time set-up (about 30 minutes)

### A1. Bring the work into `main`
1. Open `https://github.com/parthiban-loganathan/Java`, tab **Pull requests**, **New pull request**.
2. **base: main**, **compare: claude/fervent-darwin-sfgske**, **Create pull request**, **Create pull request**.
3. Wait for the checks (about 15 minutes the first time: the in-browser compiler is built from source), then
   **Merge pull request**, **Confirm merge**.
4. Tab **Actions**: the run **Deploy website to GitHub Pages** must end green (about 15 minutes the first time).
   Open `https://parthiban-loganathan.github.io/Java/lab.html`: the right-hand box says *Java compiler ready*
   once you have pressed Start (Part B).

If Pages is not enabled yet: **Settings, Pages, Build and deployment, Source: GitHub Actions** (Guide 0, step 1).

### A2. Repository settings (once)
1. **Settings, General, Features**: **Issues** ticked.
2. **Settings, Actions, General, Workflow permissions**: either choice works (the lab workflows ask for exactly
   the permissions they need); leave *Allow GitHub Actions to create and approve pull requests* unticked.

### A3. Your key pair
1. Open `https://parthiban-loganathan.github.io/Java/teacher.html`, **Create key pair** (a few seconds).
2. **Download lab-private-key.pem**. Keep it safe (password manager or encrypted drive) and never put it in
   the public repository. Without it no submission can be read; nobody can recreate it.
3. **Create the file on GitHub**: GitHub opens a new file `lab/teacher-public-key.pem` with the public key filled in;
   **Commit changes**, **Commit changes**. (Or copy the text into that file by hand.)

The key id (16 characters) is shown on the page; every intake reply and submission refers to it.

### A4. Your private gradebook repository
1. Download `https://parthiban-loganathan.github.io/Java/downloads/java-lab-gradebook.zip` and unzip it.
2. On GitHub: **+**, **New repository**, name `java-lab-gradebook`, **Private**, **Create repository**.
3. **uploading an existing file**: drag in everything from the unzipped folder **except** the `.github` folder
   (browsers often skip folders whose names start with a dot), **Commit changes**.
4. **Add file, Create new file**, name `.github/workflows/collect.yml`, paste the content of that file from the
   unzipped folder, **Commit changes**.
5. **Settings, Secrets and variables, Actions, New repository secret**: name `LAB_PRIVATE_KEY`, value: open
   `lab-private-key.pem` in a text editor and paste all of it, including the BEGIN and END lines. **Add secret**.
6. Edit `roster.csv` (pencil icon): one line per student, `regno,name,github`, for example
   `24BCL0123,Anitha R,anitha-r`. The GitHub username is optional; when it is given, a submission made from another
   account is flagged.

### A5. Students (once, 10 minutes in class)
Every student needs a free GitHub account (Guide 3, Part A). Nothing else: no repository, no software.

---

## Part B. Verify the whole chain once (15 minutes, before the first real lab)

1. **Open a test session.** This repository, **Actions**, **Lab session control** (left list), **Run workflow**:
   action `open`, session `T00`, questions `Q01`, minutes `20`, **Run workflow**. Within a minute the run is green
   and `lab/control.json` shows `T00`.
2. **Be a student.** In a private browser window (or on your phone) open `lab.html`. The green banner says
   *T00 is OPEN*. Enter a register number in the class format (for example `24BCL0001`) and a name, **Start**.
3. Choose **Q01**. Click in the editor and type the program (try Ctrl+V once: it is refused with a message).
   **Check with sample cases**: *2 of 2 sample cases passed* once the program is right. **Run** with your own input.
4. **Submit**, confirm. The package is copied. **Open GitHub** (sign in with any GitHub account), click in the box
   **Package**, paste, **Create**.
5. Within about a minute a reply appears: **Accepted**, with the time and a receipt equal to the one on the Lab page.
   The issue is closed and labelled `lab-accepted`.
6. **Close.** **Lab session control**, **Run workflow**, action `close`, session `T00`. Submit again from the Lab
   page: the reply now says **Not accepted: session T00 was not open at ...**.
7. **Collect.** In `java-lab-gradebook`: **Actions**, **Collect and mark lab submissions**, **Run workflow**,
   session `T00`. When green (3-6 minutes), open `results/T00/README.md`: your test student with *6/6* (or what
   your program earned), typed characters, active minutes and the flag *1 paste attempt(s) blocked*.
8. **Replay.** Open `results/T00/Q01/24BCL0001/<issue number>/submission.json`, **Download raw file**. On
   `teacher.html`, part 2, choose that file, **Open**: drag the slider or press **Play** to watch the typing.
9. Label the test issues `lab-ignore` (issue, right column, **Labels**) so they are left out from now on.

What the automatic checks in this repository verify on every change (tab Actions, workflow **CI**): the compiler is
built from source and every course program is compiled and run in Chromium and compared with the JDK; the Lab and
Teacher pages are driven end to end in Chromium (typing, paste and drop blocked and logged, Check and Run with the
real compiler, encrypted package, decryption, replay); session control, intake and collection are unit-tested
(open/close boundaries to the second, damaged packages, wrong keys, late and ignored issues, roster mismatches);
the grader contains wrong, endless, flooding, network-using, secret-reading and fork-bomb programs.

---

## Part C. Every lab session

**Before (any time):** decide the questions. Use course programs by id (`E05a`, `L07`, ...) or your own (Part E).

**Start of the lab** (10 seconds): **Actions**, **Lab session control**, **Run workflow**:

| Field | Example | Meaning |
|-------|---------|---------|
| action | `open` | |
| session | `W05` | any short name; one per lab session (batches: `W05-A`, `W05-B`) |
| questions | `Q04 E05a` | separated by spaces |
| minutes | `0` or `120` | `0`: open until you close it; otherwise it also closes by itself after this time |
| title | `Week 5: interfaces` | optional, shown to students |

Students refresh `lab.html`: the banner turns green within about a minute. Opening a session again later adds a new
window (same questions unless you give new ones); running `open` while it is open only changes the closing time.

**During the lab:** students type, check and submit; they may submit again, the **last accepted** submission counts.
Watch the issues tab (filter `label:lab-submission`) if you like: each reply says accepted or not.

**End of the lab** (10 seconds): **Run workflow**, action `close`, session `W05` (empty session = close everything
open). The time you pressed the button is recorded; an issue GitHub created after it is rejected.

**After the lab:** gradebook, **Collect and mark lab submissions**, session `W05`. Then read `results/W05/`:

| File | Content |
|------|---------|
| `README.md` | table of all students: tests passed, typed versus final characters, active minutes, flags; similar pairs; rejected submissions with reasons |
| `marks.csv` | one row per student and question, every number and flag (opens in LibreOffice Calc or any spreadsheet) |
| `marks-wide.csv` | one row per student, one column per question (test score in %) and the average |
| `similarity.csv` | pairs of programs sharing 80 %+ of their 5-token sequences (comments and string contents ignored) |
| `W05/<Q>/<REGNO>/<ISSUE>/` | `Main.java`, `result.md` (each test case, expected and actual output), `submission.json` (code + full log) |

Every attempt is kept, not only the last. The collection can be run as often as you like; unchanged submissions are
not re-run. Changing `tests/` or `roster.csv` re-marks automatically.

**Late or special cases:** to accept a rejected submission, add the label `lab-accept-late` to its issue and collect
again. To leave an issue out, label it `lab-ignore`.

---

## Part D. What students do (put this on the board)

1. Open **parthiban-loganathan.github.io/Java/lab.html**.
2. Register number and full name, **Start** (once per browser; on shared PCs press *Remove my work from this computer*
   when leaving).
3. Choose the assessment and the question. Read the question and the sample cases.
4. **Type** the program. **Run** (type the keyboard input in the box first) and **Check with sample cases**.
5. When satisfied: **Submit**, **OK**. Then **Open GitHub**, click in **Package**, **Ctrl+V**, **Create**.
6. Wait for the reply on that page: **Accepted** (with the time). If it says not accepted, read why.
7. Next question: choose it at the top. You can improve and submit again until the teacher closes.

Work is saved in the browser after every keystroke; a reload or crash loses nothing.

---

## Part E. Your own questions with hidden test cases

1. In this repository, **Add file, Create new file**, `lab/questions/Q07/README.md`: the statement in Markdown,
   starting with `# Q07 Title`, stating the exact input order and output format (see `lab/questions/Q01`).
2. Sample cases shown to students: `lab/questions/Q07/input.txt` and `expected.txt` (more as `input-2.txt`,
   `expected-2.txt`). Commit them shortly before the lab if the question should stay unseen until then.
3. In the gradebook: `solutions/Q07/Main.java` (your solution) and the marking cases `tests/Q07/input-1.txt`,
   `input-2.txt`, ... (include the samples and add harder, hidden ones: boundaries, invalid values, large values).
4. Make the expected outputs with the real JDK, for example in a Codespace of the gradebook or a lab PC:
   ```bash
   javac -d /tmp/q solutions/Q07/Main.java
   ```
   ```bash
   for f in tests/Q07/input-*.txt; do java -cp /tmp/q Main < "$f" > "${f/input-/expected-}"; done
   ```
   and the same for the samples (`expected.txt`) so that Check agrees with your solution.

Q01-Q03 (rational method, cube strength, water demand) are worked examples; their hidden cases are in the public
template too, so use them for practice and demonstration, not for marks.

---

## Part F. Team projects

Projects are submitted with their GitHub repository and live link on the **Submit** page (Guide 4); the check robot
validates the entry and you publish it in the project directory.

---

## Part G. Reading the typing log

| Flag in `marks.csv` | Meaning | What to do |
|------|---------|-----------|
| `n paste attempt(s) blocked` | text from outside was pasted into the editor; it was refused (the first 200 characters are in the log) | look at what was pasted (replay page, event list) |
| `n large insertion(s) without typing` | 20+ visible characters appeared at once without a paste (browser extension, automation, developer tools) | replay; ask the student to explain the code |
| `typed X characters for Y in the code` | far fewer characters were typed than the program contains | as above |
| `code differs from the typing log` | the submitted code is not what the recorded keystrokes produce (the package was built outside the page) | treat as not submitted through the Lab page |
| `away from the page N min` | the page was in the background for more than 5 minutes in total | consider with the lab rules |
| `submitted from GitHub account X, roster says Y` / `not in roster.csv` | identity mismatch | check with the student |

`active_min` sums the gaps of less than one minute between edits. Normal typing of a 40-line program: 600-1,200 typed
characters, 10-30 active minutes, several runs and checks.

---

## Part H. Limits (stated plainly)

* **Public source.** GitHub Pages is free only for public repositories, so the website's source is public. Nothing
  secret is stored in it: hidden cases, solutions, roster, marks and decrypted work stay in the private gradebook.
* **Paste blocking is a deterrent, not a lock.** A determined student can retype from another screen or use browser
  developer tools. Every such route leaves traces in the log (large insertions, time away, a log that does not produce
  the code), and similar programs are listed; use a short viva for flagged cases.
* **Time.** Acceptance uses GitHub's clock (issue creation time) and the time you pressed the button, never the
  student's clock. The page's banner is refreshed every minute and is only a guide.
* **What runs in the browser.** Console programs with `Scanner`, `printf`/`String.format` (`%d %f %s %c %b %x %n`,
  width, flags), strings, arrays, collections, classes, interfaces, exceptions, generics and threads compile and run.
  Not available in the browser: files (`FileWriter`, `FileReader`: Experiments 8-10), object serialization
  (Experiments 11-12), JDBC (Experiment 16), `%e`. Those are marked on the real JDK by the gradebook as usual, so
  they can still be submitted; students then test them in Codespaces or on a lab PC.
* **Accounts.** Each student needs a GitHub account; the GitHub username is visible on the (encrypted) issue.
* **Free limits.** Public repository: Actions minutes are free. Private gradebook: GitHub Free includes
  2,000 minutes a month; one collection of 60 students x 2 questions takes about 10-20 minutes.

---

## Part I. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Lab page: *The in-browser Java compiler is not installed on this website* | The Pages deployment has not finished, or failed: Actions, **Deploy website to GitHub Pages**. |
| Lab page: *The teacher has not published the submission key yet* | Step A3.3. |
| Banner stays grey after opening | Wait one minute and reload; check that **Lab session control** ended green. |
| No reply on a submission issue | Actions, **Lab submission check**: open the run; the issue title must start with `[Lab]` (the page does this). |
| Reply: *made with an old teacher key* | You replaced the key pair; students reload the Lab page and submit again. Keep using the new private key. |
| Reply: *package is damaged or incomplete* | The student copied only part of it: Copy package again and create a new issue. |
| Gradebook run fails at *Collect, decrypt, mark* with *Set LAB_PRIVATE_KEY* | Step A4.5. |
| A student lost work | Same browser and register number restore it; work is saved per browser, so use the same PC or submit before moving. |
