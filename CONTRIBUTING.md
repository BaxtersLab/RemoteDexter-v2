# Contributing to RemoteDexter-v2

Thank you for contributing! This document covers conventions you need to know before opening a pull request.

---

## GitHub Actions workflow permissions

### Principle of least privilege

Every workflow in `.github/workflows/` must declare the **minimal set of permissions** required for its steps. Never use `permissions: write-all` or omit the `permissions` key (which defaults to the repository-level setting and may be overly broad).

### Permission reference for existing workflows

| Workflow | Permission | Reason |
|---|---|---|
| `bootstrap.yml` | `contents: read` | Check out source code and read repository files. |
| `bootstrap.yml` | `issues: write` | Post PR comment with bootstrap diagnostics. GitHub exposes PR comments through the Issues API. |
| `bootstrap.yml` | `checks: write` | Annotate check runs with build/test results. |
| `wrapper-pr.yml` | `contents: write` | Commit the regenerated Gradle wrapper and open a PR branch. |

### Rules for modifying workflow permissions

1. **Adding a new permission** – document the reason in an inline comment directly above the permission line, for example:
   ```yaml
   permissions:
     # contents: read – check out source files
     contents: read
     # issues: write – post PR comment via Issues API
     issues: write
   ```
2. **Removing a permission** – verify that no step in the workflow (including third-party actions) relies on that token scope before removing it.
3. **Fallible write steps** – any step that writes to GitHub (e.g. posting a comment) **must** set `continue-on-error: true` so that a transient API error does not block the overall CI result.
4. **Explicit token** – always pass `github-token: ${{ secrets.GITHUB_TOKEN }}` explicitly to actions that call the GitHub API. Relying on ambient credentials has caused "Resource not accessible by integration" failures in the past.

### Smoke-check pattern

The `bootstrap.yml` workflow includes a lightweight smoke-check step that runs before any write step:

```yaml
- name: Smoke-check read permissions
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    test -f build.gradle.kts || test -f build.gradle
    echo "[ci] smoke-check: passed"
```

Add a similar step to any new workflow that performs write operations, so permission regressions are surfaced early with a clear error rather than a cryptic API response.

---

## Running tests locally

```bash
./gradlew :core:test
```

See `docs/HANDOFF.md` for additional setup instructions.
