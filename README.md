# Bonita Connector Template

This is a template repository for creating official Bonita connectors.

## Getting Started

1. Click **"Use this template"** on GitHub to create a new repository
2. Update the following placeholders:
   - `pom.xml`: Set `groupId`, `artifactId`, `name`, `description`
   - `.github/workflows/release.yml`: Replace `CHANGE_ME` with your `artifactId` and `bonitaMinVersion`
   - `CLAUDE.md`: Update with your connector's specific details
   - `README.md`: Replace this content with your connector's documentation

## What's Included

| File | Purpose |
|------|---------|
| `.github/workflows/build.yml` | CI build on push (compile, test, publish snapshots) |
| `.github/workflows/build-pr.yml` | CI build on pull requests |
| `.github/workflows/claude-code-review.yml` | Automated code review via Claude Code on PRs |
| `.github/workflows/claude.yml` | Interactive `@claude` mentions in issues, PRs, and reviews |
| `.github/workflows/release.yml` | Release workflow with opt-in marketplace notification |
| `.github/dependabot.yml` | Automated dependency updates |
| `pom.xml` | Maven project configuration |
| `CLAUDE.md` | Claude Code instructions for this repo |

## Release

```bash
# Trigger from GitHub Actions > Release workflow
# Set version and optionally enable marketplace update
```

## Pre-release Checklist

Run through this list before triggering the Release workflow. Each item maps to a real bug found in past connectors.

### Packaging

- [ ] `src/assembly/*.xml` uses `<include>*.jar</include>` (with a dot, not `*:jar`). The colon variant matches nothing and produces empty zips.
- [ ] Connector palette icon is **16x16 PNG** (not 64x64). Keep the 64x64 marketplace logo as a separate `<connector>-logo.png` if needed.
- [ ] `.impl` lives under `src/main/resources-filtered/` (so `${connector-dependencies}` is substituted at build time).
- [ ] Every `<output name="X" type="Y"/>` in the `.def` is actually emitted by the connector class.

### Studio UX

- [ ] Every `<widget id="X" inputName="..."/>` declared in the `.def` has both `X.label=` and `X.description=` entries in the matching `.properties` file. Missing entries display widgets without a name in Studio.
- [ ] Inputs are described as 1-indexed when they refer to page numbers or any human-friendly counter; the connector code subtracts 1 before calling APIs that are 0-indexed.

### Behaviour

- [ ] No silent overrides of user-provided inputs (`model`, `baseUrl`, `mimeType`, etc.). If incompatible, throw `ConnectorValidationException` with a clear message — never rewrite the user's value behind the scenes.
- [ ] Every output declared in the `.def` is initialised before `doExecute()` runs, so a `.proc` mapping an output never crashes with `SExpressionEvaluationException: No value found for mandatory expression`. Use the pattern in `CLAUDE.md` § Connector Implementation Pattern.
- [ ] `success` and `errorMessage` are always set, regardless of which branch executed.

### Tests

- [ ] At least one **WireMock test per external HTTP call** that asserts the **request body shape** with `matchingJsonPath`. Mocked-client tests do not catch wrong-shape requests.
- [ ] Optional smoke test annotated `@EnabledIfEnvironmentVariable(named="<X>_API_KEY", matches=".+")`. Run locally before opening the release PR; CI skips it when the env var is absent.
- [ ] Standalone `XxxMockServer` `main` class for local Studio demos. Wired into `scripts/run-mock-server.cmd` and `scripts/run-mock-server.sh`.

### CI

- [ ] `build.yml` runs `chmod +x mvnw` before any `./mvnw` step (Windows-committed wrappers lose the bit).
- [ ] `build.yml` deploy step is conditioned to `develop` / `support/*` branches.
- [ ] `build-pr.yml` does **not** deploy.

## License

See [LICENSE](LICENSE).
