# CLAUDE.md

## Project Overview

This is an official Bonita connector for the Bonita BPM platform.

**Technology Stack:**
- Java 17
- Maven
- Bonita Runtime 10.2.0+

## Build and Test Commands

```bash
# Build all (skip tests)
./mvnw clean package

# Build with tests
./mvnw clean verify

# Run unit tests only
./mvnw test

# Run a single test class
./mvnw test -Dtest=MyConnectorTest
```

## Commit Message Format

Use conventional commits:
```
type(scope): description
```

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `perf`

## Release Process

Releases are managed via GitHub Actions:
1. Run the "Release" workflow with the target version
2. Optionally set `update_marketplace: true` to publish to the Bonita marketplace

## Icon Convention

Every connector ships **two** icon files in `src/main/resources/`:

| File | Size | Purpose | Referenced in `.def` |
|---|---|---|---|
| `<connector>.png` | **16×16** | Studio palette / connector picker | Yes — `<icon>` and `<category icon=...>` |
| `<connector>-logo.png` | **64×64** | Marketplace listing upload | No (separate field at upload time) |

**Why two sizes:** Bonita Studio's connector palette renders icons at 16-24 px; a 64×64 source is downscaled at runtime and looks fuzzy. The marketplace listing requires 64×64 minimum and reads it as a separate upload field, not from the bundled `.def`.

**`.def` example:**
```xml
<icon>myconnector.png</icon>
<category icon="myconnector.png" id="My Connector"/>
```

Do NOT reference `<connector>-logo.png` in any `.def` — it is for the marketplace toolchain only.

When migrating an old connector with a single oversized icon, generate the 16×16 from the 64×64 source (don't upscale a 16×16) so both look sharp.
