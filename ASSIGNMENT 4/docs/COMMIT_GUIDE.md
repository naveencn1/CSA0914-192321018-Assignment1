# Commit progressively – at least one commit per task

Make each commit when you have actually finished and run that task (the faculty can see commit dates).

```bash
git init
git remote add origin https://github.com/<your-user>/CSA09-192321018-Assignment4.git
git add README.md .gitignore && git commit -m "Add README skeleton and .gitignore"

git add Task05 && git commit -m "Task 5: currency converter in four event styles + tests"
git add Task04 && git commit -m "Task 4: fix drawing pad (mouseClicked, stored points, repaint) + benchmark"
git add Task03 && git commit -m "Task 3: library search screen with nested layouts"
git add Task01 && git commit -m "Task 1: diagnose fee window and redesign with service/repository"
git add Task02 && git commit -m "Task 2: event table, containment tree, pseudocode, call sequence"
git add Task06 && git commit -m "Task 6: Swing + JDBC event registration, SQL scripts"
git add docs REPORT.md && git commit -m "Add report and screenshot evidence notes"
git branch -M main && git push -u origin main
```
Add your screenshots to each `TaskNN/screenshots/` folder and commit them with the matching task
(`git add Task03/screenshots && git commit -m "Task 3: resize screenshots"`).
Then: GitHub → Settings → Collaborators → add the faculty (or make the repository public) and paste the link on the report cover page.
