# Issue tracker: GitHub

Issues and specs for this repository live in GitHub Issues. Use the `gh` CLI and infer the repository from `git remote -v`.

## Operations

- Create: `gh issue create --title "..." --body "..."`
- Read with comments: `gh issue view <number> --comments`
- List: `gh issue list --state open --json number,title,body,labels,comments`
- Comment: `gh issue comment <number> --body "..."`
- Label: `gh issue edit <number> --add-label "..."` or `--remove-label "..."`
- Close: `gh issue close <number> --comment "..."`

Use a heredoc for multiline bodies. When a skill says to publish a spec or ticket, create a GitHub issue. When it says to fetch a ticket, read the issue and its comments.

## Pull requests as a triage surface

**PRs as a request surface: no.** Pull requests are not included in the issue-triage queue.

## Wayfinding

Use one issue labelled `wayfinder:map` as the map and GitHub sub-issues as its tickets. Label child tickets `wayfinder:research`, `wayfinder:prototype`, `wayfinder:grilling`, or `wayfinder:task`.

Represent blocking relationships with GitHub issue dependencies. If unavailable, put `Blocked by: #<number>` at the top of the child issue. A ticket is ready when all blockers are closed and it has no assignee. Claim it with `gh issue edit <number> --add-assignee @me`; resolve it with a summary comment followed by closing the issue.
