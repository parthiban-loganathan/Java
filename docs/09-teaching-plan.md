# 9. Teaching and demonstration plan (from absolute basics)

How to teach the lab to 60 first-year students who have never programmed, using the website as the
blackboard. Every session has three parts: **Demonstrate** (you, on the projector), **Students do**
(in pairs, on lab PCs or phones) and **Check** (questions to ask before moving on).

The syllabus experiments (E01a-E16) keep their usual lab slots; the lessons (L01-L14) prepare for
them, and the project work runs from week 6.

| Week | Theme | Course material | Project milestone |
|------|-------|-----------------|-------------------|
| 1 | What a program is; first run | L01, L02 | accounts created |
| 2 | Arithmetic, text, input | L03, L04, L05, E01a, E01b | - |
| 3 | Decisions and loops | L06, L07, E01c | GitHub session 1 |
| 4 | Arrays | L08, E02a, E02b | GitHub session 2; teams formed |
| 5 | Lists, methods | L09, L10 | catchment demonstration |
| 6 | Classes and objects | L11, E03a, E03b | problem claimed |
| 7 | Inheritance, interfaces | E04a, E04b, E05a, E05b | hand calculation + flowchart |
| 8 | Exceptions | L12, L13, E06a, E06b | Java module + first test |
| 9 | Threads, files | E07, E08-E10 | web page from the template |
| 10 | Serialization | E11, E12 | charts and map |
| 11 | Generics, collections | E13-E15 | deployed on Pages |
| 12 | Databases | E16 (Codespaces) | README, data, limitations |
| 13 | From console to apps | L14 | submission |
| 14 | Presentations | - | viva and demo day |

---

## Week 1. What is a program? (L01, L02)

**Demonstrate (20 min)**
1. Open the website → **Learn Java** → **L01**. Read the table "Hand calculation versus program" aloud with the
   beam example: input, processing, output.
2. Scroll to the code. Point at each part: `class`, `main`, `{ }`, `System.out.println`, `;`.
3. Press **Run**. Show *Output (browser)* and *Expected output (JDK)*, and the green badge *matches the JDK output*.
4. Open **L02**. Explain a variable as a labelled box. Run it; stop at `Integer.MAX_VALUE + 1` and ask why it becomes
   negative. Stop at `0.1 + 0.2`.

**Students do**: open L01 on their own screens and run it; in pairs, predict the output of L02 before running it.

**Check**: What does `;` do? Why is `7 / 2` equal to 3? Which type would you use for a span in metres?

## Lab assessments (from week 2)

During the lab hour students type each experiment or question on the website's **Java Lab** page, run and check it
in the browser and press Submit; you open the assessment at the start and close it at the end with one button each.
Set it up once with [10. Java Lab assessments](10-lab-assessments.md) (Part A, 30 minutes). Students only need a free
GitHub account (Guide 3) before their first assessment.

## Week 2. Arithmetic, text and keyboard input (L03-L05, E01a, E01b)

**Demonstrate**: in L05 change the *Input* box (span 6 → 8) and run again; show that the same program works for any
data. Show the `nextLine()` trap from the README. Then E01a with the handout input.

**Students do**: E01a and E01b with three inputs of their own, one of them unusual (zero, negative).

**Check**: What is a token? Why does E01b use `Math.abs`?

## Week 3. Decisions, loops, and GitHub session 1

**Demonstrate**: L06 with the boundary values 2.4 and 2.5 (test cases 2 and 3 in the drop-down). L07: the SCS
runoff table; change CN from 80 to 60 and discuss why runoff falls.

**GitHub session 1 (45 min)**: follow [3. GitHub for students](03-student-github-guide.md), parts 1 and 2,
live on the projector. Every student creates an account and opens the lab repository.

**Check**: How many times does `for (int h = 1; h <= 6; h++)` run? What is a repository?

## Week 4. Arrays, GitHub session 2, teams

**Demonstrate**: L08; explain that the elevation model of the catchment simulator is a 2-D array of 226 × 307
numbers. Open the **Catchment simulator** and choose colour *Elevation*.

**GitHub session 2 (60 min)**: each student follows part 3 of the student guide: create a repository from the
**CivilTech Innovation Starter** template, enable Pages, edit the title in `web/index.html` in the browser, commit,
watch the Actions tick, open their own live site. Everyone leaves with a working website.

**Teams**: form 12 teams of 5 and agree roles (Java, interface, charts/map, data and documentation, testing and
presentation).

## Week 5. Demonstration of a complete application (50 min)

Use the **Catchment simulator** as the model of what a finished project looks like.

1. **Problem**: rain falls on the hills next to VIT; how much water reaches the stream, and when? Show the map.
2. **Open data**: open [6. Data sources](06-data-sources.md). Elevation from the Copernicus satellite DEM, land
   cover from ESA WorldCover, rainfall from ERA5 for Cyclone Fengal. Mention licences.
3. **Algorithm**: colour *Flow accumulation* shows the stream network found by the D8 algorithm; the yellow
   outline is the catchment. Click on another valley: the Java code delineates a new catchment in milliseconds.
4. **Engineering method**: the SCS Curve Number. Change the soil group from C to A and to D and compare the runoff.
5. **Results**: the hydrograph chart, the table, the CSV download.
6. **3D**: press **Play**; blue tint = runoff generated, dots = stream flow.
7. **Honesty**: read the orange *Educational model* notice. Ask: why can we not call this a flood forecast?
8. **Code**: open `java/civiltech-core` on GitHub: the classes `PriorityFlood`, `FlowNetwork`, `Watershed`,
   `ScsCurveNumber`. Show a test in `HydrologyTest.java` that checks a hand calculation.
9. **Same Java on a phone**: install the APK from the home page.
10. **Your turn**: open the **100 Problems** page.

## Week 6. Choosing and claiming a problem

**Demonstrate**: filter the problem bank by *Beginner*, open one problem's details, press **Claim**, show the
pre-filled GitHub issue (do not submit it).

**Teams do**: choose a problem (or OWN), discuss it with you, then submit the claim issue. Approve it in the issue.

## Weeks 7-12. Project checkpoints (30 minutes per week per team)

| Checkpoint | Evidence to show | Course link |
|------------|------------------|-------------|
| Hand calculation and flowchart | photo or PDF in the repository | L01 |
| Java class with one method and one test | `mvn test` green in Actions | L10, L11 |
| Reading the data file | CSV in `web/data`, parsed in Java | L09, L13, E08-E10 |
| Web page shows results | live site | L14 |
| Chart and map | live site on a phone | - |
| README: method, data, licences, limitations, roles | repository | - |

In each checkpoint ask one member, chosen by you, to explain a part of the code they did not write.

## Week 13. Submission

**Demonstrate**: the **Submit** page; fill it for a team on the projector, show the green "All checks passed",
press **Open submission on GitHub**, and let the team press *Submit new issue*. Show the robot's reply a few minutes
later. Then follow [0. Start here](00-start-here.md), step 9, to publish it.

## Week 14. Demo day

Each team: 7 minutes demonstration on a phone and the projector, 5 minutes viva. Use the rubric in
[8. Teacher guide](08-teacher-guide.md). Published projects stay on the website for future batches.

---

## Tips that save time

* Keep the website open on the projector all semester; students follow the same pages on their phones.
* When a student's program fails, read the first line of the error message together; the table "Common mistakes"
  in L01 covers most first-week errors.
* Codespaces is the fallback when a lab PC lacks software: the same environment for everyone.
* Ask teams to commit at least once per checkpoint; the commit history shows who did what.
