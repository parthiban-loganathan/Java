# My Java lab (private)

This private repository holds your weekly Java lab programs. Only you and your teacher can see it.
After every save, GitHub compiles and runs your programs on Java 21 against the official test
cases and writes the results to [`results/summary.md`](results/summary.md).

## First-time set-up (10 minutes)

1. You are reading this in your own copy, created with **Use this template**, named `java-lab-<your
   register number>` (for example `java-lab-24bcl0001`) and set to **Private**.
2. Open `student.json`, click the **pencil**, fill in your name and register number exactly as in
   college records, for example:
   ```json
   {
     "name": "Asha R",
     "regno": "24BCL0001"
   }
   ```
   Click **Commit changes**.
3. **Settings → Collaborators → Add people**: add your teacher's GitHub username. Your teacher can then
   read your programs to evaluate them. Nobody else can.

## Every week

1. Open the lab website, page **Weekly lab**.
2. Enter your name, register number and repository once (the page remembers them on your device).
3. Choose the exercise, paste your Java code, press **Save to my repository**.
4. GitHub opens with your file ready: press **Commit changes**.
5. Wait about a minute, then open `results/summary.md` (or the page's **My results** button).
   A green tick next to your latest commit means the checks ran; open the exercise's report to see
   which test cases passed and, for the others, your output beside the expected output.
6. Not right yet? Edit `lab/<exercise>/Main.java` (pencil), fix, commit. Every version is kept in the
   history, so you can improve your program until the deadline.

## Rules

* Write your own code. Your teacher sees every version you commit and a similarity report across the class.
* Do not change `lab-tools/` or `.github/`: your teacher grades with an independent copy of the tests.
