Title: Finalize release notes for 2026-02-18 changelog entry

Background
We recently merged deterministic END/recovery semantics and expanded telemetry contract tests into `main`. The entry in `docs/CHANGELOG-2026-02-18.md` is present but needs polishing into a concise release note suitable for end-users and release communications.

Goal
Produce a short, user-facing release note and ensure the changelog entry on `main` is final and linked from any top-level release notes page.

Checklist
- [ ] Edit `docs/CHANGELOG-2026-02-18.md` to read as a polished release note (audience: users and release managers).
- [ ] Add a one-paragraph summary to `docs/RELEASE_NOTES.md` (or create it) that highlights: what changed, why it matters, and any upgrade notes.
- [ ] Confirm the final changelog text is committed to `main` with author attribution.
- [ ] Link the changelog entry from the top-level release notes or README if applicable.

Acceptance criteria
- Changelog entry in `docs/CHANGELOG-2026-02-18.md` is rewritten in release-note tone and merged to `main`.
- A one-paragraph release note exists and is discoverable from the docs index.

Labels: docs, release, chore
Assignee: @maintainer

Notes
If you'd like, provide final wording here and I will commit it directly to `main` (small edits only). Otherwise I can propose a polished draft for review.