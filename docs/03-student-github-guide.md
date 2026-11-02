# 3. GitHub for students, step by step

You need only a web browser and an e-mail address. Everything is free. Follow the parts in order;
each step says exactly where to click.

## Words you will meet

| Word | Meaning |
|------|---------|
| **Repository (repo)** | A project folder on GitHub with the history of every change. |
| **Commit** | Saving a change, with a short message ("add rainfall chart"). |
| **Template** | A repository you copy to start your own project with the same files. |
| **GitHub Actions** | Robots that test your Java and publish your website after every commit. |
| **GitHub Pages** | The free website of your repository: `https://<username>.github.io/<repository>/`. |
| **Issue** | A discussion page. You use issues to claim a problem and to submit your app. |
| **Codespace** | A complete computer with Java in your browser, for running and testing code. |

---

## Part 1. Create your account (5 minutes)

1. Go to **github.com** and click **Sign up**.
2. Enter your e-mail (college e-mail recommended), a password and a **username**. Choose a professional username:
   it appears in your website address and your project credits.
3. Verify the e-mail (GitHub sends a code).
4. Optional, recommended: apply for the **GitHub Student Developer Pack** at `education.github.com` with your college
   e-mail; it gives free GitHub Pro, including more Codespaces hours.

## Part 2. Explore the lab (10 minutes)

1. Open the lab website (your teacher gives the address) and the lab repository on GitHub.
2. On the website open **Learn Java → L01** and press **Run**.
3. On GitHub, click through the folders `course`, `site`, `java`. Click a file to read it. Nothing you do while
   reading can break anything.

## Part 3. Create your team's project from the template (one member, 15 minutes)

1. Open the **CivilTech Innovation Starter** repository (link from the teacher or the website's *Starter template*
   page).
2. Click the green **Use this template** button → **Create a new repository**.
3. **Repository name**: your project in lower-case words joined by `-`, e.g. `rooftop-rainwater-vit`.
   Choose **Public**. Click **Create repository**.
4. Add your teammates: **Settings → Collaborators → Add people**, enter each username, role *Write*. Each teammate
   accepts the e-mail invitation.
5. Turn on the website: **Settings → Pages → Source: GitHub Actions**.
6. Start the first build: open `web/index.html`, click the **pencil** (Edit), change the title text between
   `<h1>` and `</h1>` to your project's question, click **Commit changes** → **Commit changes**.
7. Click the **Actions** tab. Wait for the green tick (about 2 minutes).
8. Open `https://<your-username>.github.io/<repository-name>/` (also shown under **Settings → Pages**).
   Your website is live. Share the link with your team.

## Part 4. Edit files in the browser (every day)

For small changes you never need anything except the browser:

1. Open the file → click the **pencil**.
2. Make the change.
3. **Commit changes**, write a message that says what you changed, click **Commit changes**.
4. **Actions** tab: green tick = tests passed and the site is updated; red cross = open it and read which test or step
   failed, then fix the file the same way.

## Part 5. Run and test Java in a Codespace

1. In your repository click **Code → Codespaces → Create codespace on main**. A browser editor (VS Code) opens after
   a minute.
2. At the bottom is the **Terminal**. Type, then press Enter:

```bash
mvn -q test
```

   This runs your Java tests. `BUILD SUCCESS`, or no error output, means they pass.

3. To see the website before committing:

```bash
bash build.sh
```

```bash
python3 -m http.server 8000 --directory dist
```

   A pop-up offers **Open in Browser**.
4. Save your work to GitHub: click the **Source Control** icon on the left (branches symbol), type a message, click
   **Commit**, then **Sync Changes**.
5. When you finish, stop the codespace (**Code → Codespaces → ... → Stop codespace**) so you do not use up your free
   hours.

## Part 6. Claim a problem

1. On the lab website open **100 Problems**. Use the filters (start with *Beginner*).
2. Discuss with your team and teacher. Then press **Claim** on your problem (or **Claim my own idea**).
3. GitHub opens an issue form with the problem ID filled in. Complete the other boxes and click **Submit new issue**.
4. Your teacher replies in the issue. Post progress there during the semester.

## Part 7. Submit your app

1. On the lab website open **Submit** (or press **Submit app** on your problem card).
2. Fill every field. The right-hand panel lists anything still missing. When it shows *All checks passed*, click
   **Open submission on GitHub**, then **Submit new issue**.
3. Within minutes a robot comments on the issue. If it lists problems, fix them in the form, generate again, and edit
   the issue (three dots → **Edit**, paste the new entry).
4. Your teacher checks the live demo and publishes the project on the lab website.

## Working as a team without conflicts

* Agree who edits which file (Java calculation, page layout, charts and map, data and documentation, testing).
* Commit small, working steps; write clear messages.
* If two people edited the same lines, GitHub asks you to resolve a **conflict**: keep the correct lines between
  `<<<<<<<`, `=======` and `>>>>>>>`, delete those marker lines, and commit.

## Rules of the lab

* Use only data whose licence allows reuse; write the source and licence in your README.
* Never commit passwords, API keys or personal data (phone numbers, addresses).
* AI assistants may help, but every team member must be able to explain every line.
* Write at least one test that compares your Java with a hand calculation.
