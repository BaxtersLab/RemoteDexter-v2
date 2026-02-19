Title: Document and harden bootstrap workflow permissions

Background
The `bootstrap` workflow previously failed with a "Resource not accessible by integration" error when attempting to post comments. We applied a minimal permissions fix, but we should document the rationale and harden the workflow to reduce future regressions.

Goal
Make the permission requirements and the comment-posting step explicit and documented, and add guidance for contributors and maintainers when modifying workflows.

Checklist
- [ ] Add inline comments to `.github/workflows/bootstrap.yml` explaining why `contents: read`, `issues: write`, and `checks: write` are required.
- [ ] Ensure the comment-posting step explicitly uses `${{ secrets.GITHUB_TOKEN }}` (or equivalent) and is marked non-fatal (`continue-on-error: true`).
- [ ] Add a short note in `CONTRIBUTING.md` or `docs/HANDOFF.md` describing workflow permission expectations and how to safely change them.
- [ ] Add a smoke-check in the PR template or CI job that verifies the bootstrap job can execute basic read/check operations (no write) when appropriate.

Acceptance criteria
- Workflow YAML includes explanatory comments and uses explicit token input for comment steps.
- Documentation describing the permission model is added to the repo docs.

Labels: ci, documentation, security
Assignee: @maintainer

Optional: I can submit a follow-up patch that inserts the inline comments and the small GitHub Actions job snippet for review.