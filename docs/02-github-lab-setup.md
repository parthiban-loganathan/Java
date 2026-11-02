# 2. Setting up the lab on GitHub (maintainer)

This guide is for the faculty member who maintains the lab. It takes about one hour.

## Decision: organization + template + forks-free workflow

| Option | Assessment |
|--------|-----------|
| **GitHub Organization (free plan)** owning the central repository and the template | **Recommended.** The lab outlives any one account; several maintainers can share ownership; teams can be created per batch. |
| Everything under a personal account | Works (this repository does today), but transferring ownership later is extra work. |
| Students fork the central repository to build projects | Not recommended: forks inherit the whole lab history and all course files, and every team would carry a copy. |
| Students create repositories **from the template** and submit a **pull request** with one JSON entry | **Recommended.** Each team owns a small, clean repository; the lab only stores the directory entry. |
| GitHub Classroom | Optional. Useful to create 12 team repositories from the template in one step and to see them in one dashboard; it is free for teachers. |

## Step 1. Create the organization

1. Sign in, open **Your organizations → New organization**, choose the **Free** plan.
2. Name: for example `vit-civil-innovation-lab`. Contact e-mail: a departmental address.
3. Add at least one more faculty member as **Owner** so the lab never depends on one person.
4. Settings → Member privileges: base permission **Read**; disable repository creation by
   members if teams should use their own accounts (recommended).

GitHub does not allow organizations to be created through its API, so this step is done in the web
interface.

## Step 2. Move or create the central repository

* Transfer this repository to the organization (**Settings → General → Transfer ownership**), or
  push it to a new public repository named `lab`.
* **Settings → Pages → Build and deployment → Source: GitHub Actions.** The workflow
  `.github/workflows/pages.yml` then publishes the site at
  `https://<organization>.github.io/<repository>/` after every push to `main`.
* **Settings → Branches → Add rule for `main`**: require a pull request and the `CI` status check.

## Step 3. Publish the starter template as its own repository

The folder `starter-template/` is designed to be the root of a repository:

```bash
git clone https://github.com/<organization>/lab.git
cp -R lab/starter-template civiltech-innovation-starter
cd civiltech-innovation-starter
git init -b main
git add .
git commit -m "CivilTech Innovation Starter"
git remote add origin https://github.com/<organization>/civiltech-innovation-starter.git
git push -u origin main
```

Then on GitHub: **Settings → General → Template repository** (tick). Students now see a green
**Use this template** button.

## Step 4. Teams and permissions

* Create one team per batch, e.g. `batch-2026`, with all students as members (Read access to the
  central repository is enough; they contribute by pull request).
* Teams create their **own** project repository from the template, under one member's personal
  account (or in the organization if you prefer central ownership: then give each team **Maintain**
  on its repository).

## Step 5. Review pull requests to the directory

A team's pull request changes only `projects/projects.json`. The `CI` workflow validates the entry
against `projects/projects.schema.json`. Before merging, check:

1. The repository is public and has a licence file.
2. The live demo opens and works on a phone.
3. Data sources and licences are listed in the project's README.
4. The Java module has at least one test that checks a hand calculation.

## Step 6. Each new academic year

* Create the new batch team; archive old teams.
* Keep `main` green: Dependabot or a yearly check of library versions.
* Encourage new teams to improve previous projects through pull requests to *their* repositories.
