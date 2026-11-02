# 0. Start here: step-by-step for a GitHub beginner (teacher / maintainer)

This guide takes you from "the code is on GitHub" to "the lab website is live, students can claim
problems and submit apps". Everything is done in a web browser; you do not need to install anything.
Allow about one hour the first time.

Words you will meet:

| Word | Meaning |
|------|---------|
| **Repository (repo)** | A project folder on GitHub with the full history of every change. Yours is `parthiban-loganathan/Java`. |
| **Branch** | A separate line of work. `main` is the official one; the lab's work currently sits on the branch `claude/fervent-darwin-sfgske`. |
| **Commit** | One saved change, with a message. |
| **Pull request (PR)** | A request to copy the commits of one branch into another, shown as a page where you can review and press *Merge*. |
| **GitHub Actions** | Robots that run after every change: they test the code and publish the website. The *Actions* tab shows them (green tick = success, red cross = failure). |
| **GitHub Pages** | Free website hosting for a repository. |
| **Issue** | A discussion page in the repository. Students use issues to claim problems and submit apps. |

The repository page is `https://github.com/parthiban-loganathan/Java`. Sign in at github.com first.

---

## Step 1. Turn on GitHub Pages (do this before merging)

1. Open the repository page.
2. Click **Settings** (the gear tab at the top right of the repository; you see it because you own the repository).
3. In the left menu click **Pages**.
4. Under **Build and deployment**, open the **Source** drop-down and choose **GitHub Actions**.
5. Nothing else is needed on this page. It saves automatically.

## Step 2. Create two labels (lets you filter student issues)

1. Click the **Issues** tab, then **Labels** (next to the search box), then **New label**.
2. Name `proposal`, any colour, **Create label**.
3. Again **New label**: name `submission`, **Create label**.

## Step 3. Merge the lab into `main` with a pull request

1. Click the **Pull requests** tab, then the green **New pull request** button.
2. Set **base:** to `main` and **compare:** to `claude/fervent-darwin-sfgske`.
   GitHub shows the list of changed files (more than 300 new files).
3. Click **Create pull request**. Title: `Civil Engineering Innovation Lab`. Click **Create pull request** again.
4. Wait 2-3 minutes. At the bottom of the pull request, the checks **CI / Java tests ...**, **CI / Experiment 16 ...**
   and **CI / Android APK** turn green.
5. Click **Merge pull request**, then **Confirm merge**.

## Step 4. Watch the website being published

1. Click the **Actions** tab. A run called **Deploy website to GitHub Pages** starts (yellow dot = running).
2. Click it. It has three boxes: *android* (builds the app), *build* (builds the website) and *deploy*.
3. When all three are green (about 4 minutes), the *deploy* box shows a link. Your site is:

   **https://parthiban-loganathan.github.io/Java/**

If *deploy* fails with a message about the `github-pages` environment or branch protection:
**Settings → Environments → github-pages → Deployment branches and tags** → make sure `main` is
allowed, then in **Actions** open the failed run and click **Re-run all jobs**.

## Step 5. Check the website yourself (10 minutes)

| Page | What to try |
|------|-------------|
| Home | Read the cards; press **Download APK** (Android phones only). |
| Learn Java | Open L01, press **Run**. Open L05, change the input numbers, press **Run** again. Open E07 (threads). |
| Catchment simulator | Wait for the 3D view; press **Play**. Click somewhere on the terrain: the catchment is redrawn. Change *Soil group* to D. |
| Starter template | The example app the students will copy. |
| 100 Problems | Filter by *Beginner*; open *Method, data, Java, deliverable* of one problem. |
| Submit | The form students use to submit. Fill it with test values to see the checks (do not open the GitHub issue). |
| Guides | These documents. |

On a phone, open the same address; every page is mobile friendly.

## Step 6. Create the starter template repository (once)

Students start their projects from a **template repository**. Create it like this:

1. On your website open `https://parthiban-loganathan.github.io/Java/downloads/civiltech-innovation-starter.zip`.
   The file downloads. Unzip it (Windows: right-click → *Extract All*). You get a folder
   `civiltech-innovation-starter`.
2. On GitHub click **+** (top right) → **New repository**.
   - Repository name: `civiltech-innovation-starter`
   - **Public**
   - Do **not** tick *Add a README*.
   - Click **Create repository**.
3. On the new empty repository page click the link **uploading an existing file**.
4. Open the unzipped folder on your computer, select **everything inside it** (Ctrl+A), and drag it onto the
   GitHub page. Wait until all files are listed. Click **Commit changes**.
5. Check that the folder `.github` arrived: you should see `.github` in the file list. Some computers hide folders
   whose names start with a dot and do not upload them. If `.github` is missing:
   - Click **Add file → Create new file**.
   - In the name box type `.github/workflows/deploy.yml` (the slashes create the folders).
   - Open `https://github.com/parthiban-loganathan/Java/blob/main/starter-template/.github/workflows/deploy.yml`,
     click **Copy raw file** (the copy icon), paste it into the editor, click **Commit changes**.
6. **Settings → General** → tick **Template repository**.
7. **Settings → Pages → Source: GitHub Actions**.
8. Make one small change to start the robot: open `README.md`, click the pencil, add a line, **Commit changes**.
   In **Actions** wait for the green tick; the example app is then live at
   `https://parthiban-loganathan.github.io/civiltech-innovation-starter/`.

Students now see a green **Use this template** button on that repository.

## Step 7 (optional). Move the lab into an organization

An organization keeps the lab independent of one person's account. It is free.

1. Click your profile picture → **Your organizations** → **New organization** → **Free**.
2. Name, for example, `vit-civil-innovation-lab`; contact e-mail: a departmental address.
3. Invite a second faculty member as **Owner** (organization page → **People** → **Invite member**).
4. To move a repository: repository **Settings → General → Danger Zone → Transfer ownership**, choose the organization.
   The website address then becomes `https://vit-civil-innovation-lab.github.io/Java/`; repeat Step 1 in the moved
   repository if Pages is not active.

## Step 8. When a student team claims a problem

A team presses **Claim** on the *100 Problems* page and submits the form. You get an e-mail and the issue appears
under **Issues** with the label `proposal`.

1. Open the issue. Check: real problem, open data, team of five, roles.
2. Reply in the comment box, e.g. "Approved. First checkpoint: hand calculation and one Java test by <date>."
3. Leave the issue open; teams post progress in it.

## Step 9. When a team submits a project

The team uses the **Submit** page; an issue titled `[Submission] ...` appears. Within a few minutes the robot
**Check project submission** comments on it: either a list of things to fix, or "The entry is valid" followed by a
block of JSON.

When the entry is valid:

1. Open the team's **live demo** link and try it on a phone. Open the repository and look for the README, data
   sources and a test.
2. Copy the JSON block from the robot's comment (the copy icon at its top right).
3. In your lab repository open the file `projects/projects.json` and click the **pencil** (Edit).
4. Scroll to the end. The file ends like this:

   ```text
       }
     ]
   }
   ```

   Click just after the last `}` that is followed by `]`, type a comma `,`, press Enter, and paste the JSON.
   The end must now look like:

   ```text
       },
       {
         "id": "rooftop-rainwater-vit-hostels",
         ...
       }
     ]
   }
   ```

5. Click **Commit changes**, keep *Commit directly to the main branch*, click **Commit changes**.
6. The **CI** robot checks the file (Actions tab). If it shows a red cross, open it: it names the problem
   (usually a missing comma). Fix it with the pencil again.
7. The website updates itself within about 4 minutes. Close the issue with a comment such as "Published".

## Step 10. Weekly routine (15 minutes)

1. **Issues**: answer new proposals and submissions.
2. **Actions**: everything green?
3. **Insights → Traffic** shows visitors to the repository.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Website shows 404 | Step 1 not done, or the deploy run has not finished. Check **Actions**. |
| Deploy fails: environment protection | Step 4, second paragraph. |
| Red cross on CI after editing `projects.json` | Missing or extra comma, or a field that breaks a rule; the log line says which. |
| Students cannot see **Use this template** | Step 6.6 not ticked, or the template repository is private. |
| A student's site shows 404 | In their repository: **Settings → Pages → Source: GitHub Actions**, then any commit. |
| The robot did not comment on a submission | The title must start with `[Submission]`; the Submit page does this automatically. |

Next: [9. Teaching and demonstration plan](09-teaching-plan.md).
