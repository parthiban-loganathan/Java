# 3. GitHub for students, step by step

You need only a free GitHub account and a web browser. Everything else is free.

## Vocabulary

| Word | Meaning |
|------|---------|
| **Repository (repo)** | A project folder on GitHub, with the full history of every change. |
| **Commit** | A saved snapshot of your changes with a short message ("add rainfall chart"). |
| **Push** | Upload your commits from your computer (or codespace) to GitHub. |
| **Pull** | Download other people's commits. |
| **Clone** | Make a local copy of a repository. |
| **Branch** | A separate line of work; `main` is the published one. |
| **Template** | A repository you can copy to start a new one with clean history. |
| **Fork** | Your own copy of someone else's repository, used to propose changes back. |
| **Pull request (PR)** | A request to merge your changes into a repository; others review it first. |
| **GitHub Actions** | Automatic jobs (tests, build, deployment) that run on GitHub after a push. |
| **GitHub Pages** | Free hosting of your project's website. |
| **Codespace** | A complete development computer in the browser (VS Code + Java + Maven). |

## 1. Create your account

Sign up at github.com with your college e-mail. Optional but recommended: apply for the **GitHub
Student Developer Pack** (free GitHub Pro for students, including more Codespaces hours).

## 2. Create your team repository from the template (one member does this)

1. Open the **CivilTech Innovation Starter** template repository and press **Use this template →
   Create a new repository**.
2. Name it after your project, e.g. `smart-rainwater-harvesting`. Choose **Public**.
3. **Settings → Collaborators → Add people**: add your four team members (role *Write*).
4. **Settings → Pages → Source: GitHub Actions**.

## 3. Open it in Codespaces (no installation)

Press **Code → Codespaces → Create codespace on main**. After a minute you have VS Code in the
browser with JDK 21, Maven and Node.js installed. In its terminal:

```bash
mvn -q package
```

This runs the tests and compiles the Java to JavaScript. Then:

```bash
bash build.sh
python3 -m http.server 8000 --directory dist
```

Codespaces offers to open port 8000 in the browser: that is your application.

Working on a lab PC instead: install JDK 21 (Eclipse Temurin), Maven, Git and VS Code, then
`git clone https://github.com/<you>/<repo>.git`.

## 4. The daily cycle: edit, test, commit, push

```bash
git pull                         # get your teammates' work first
# ... edit files ...
mvn -q test                      # do the hand-calculation tests still pass?
git add -A
git commit -m "Add CSV upload for borehole data"
git push
```

In Codespaces you can also use the **Source Control** panel (the branch icon): type a message, press
**Commit**, then **Sync**.

After every push, open the **Actions** tab: a green tick means the site was rebuilt and deployed to
`https://<you>.github.io/<repo>/`. A red cross shows exactly which test or step failed.

## 5. Working as a team without conflicts

* Agree who edits which file (one person: Java calculation; one: page layout; one: charts/map;
  one: data and documentation; one: testing and the presentation).
* Pull before you start, push when a small piece works.
* For larger changes use a branch and a pull request inside your own repository:

```bash
git switch -c csv-upload
# ... work, commit ...
git push -u origin csv-upload
```

Then open a pull request on GitHub; a teammate reviews and merges it.

* If Git reports a **merge conflict**, open the file, keep the correct lines between the
  `<<<<<<<`, `=======` and `>>>>>>>` markers, delete the markers, commit.

## 6. Rules of the lab

* Use only data whose licence allows reuse; write the source and licence in your README.
* Never commit passwords, API keys or personal data.
* AI assistants may help, but every team member must be able to explain every line in the viva.
* Write at least one test that compares your Java with a hand calculation.
