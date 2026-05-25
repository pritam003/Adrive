# Adrive — Project Skill Guide

> **IMPORTANT**: Always follow all instructions in this file when working on the Adrive project.

---

## 1. Repository & CI/CD

| Item | Value |
|---|---|
| GitHub Repo | https://github.com/pritam003/Adrive |
| Default Branch | `main` |
| CI/CD System | GitHub Actions |
| Workflow file | `.github/workflows/android-ci-cd.yml` |

### How the pipeline works

```
Developer pushes to `main`
        │
        ▼
 ┌──────────────┐     ┌──────────────┐
 │  Build &     │     │    Lint      │
 │  Unit Test   │     │    Check     │
 └──────┬───────┘     └──────┬───────┘
        │                    │
        └────────┬───────────┘
                 ▼
         ┌───────────────┐
         │ Release Build │  (main branch only)
         │ + GitHub Tag  │
         └───────────────┘
```

**Jobs:**

1. **Build & Unit Test** – Runs `./gradlew clean test assembleDebug`.  Uploads debug APK as artifact.
2. **Lint Check** – Runs `./gradlew lint`. Uploads HTML lint report as artifact.
3. **Release Build** – Runs `./gradlew clean assembleRelease`, uploads release APK and creates a versioned GitHub Release.

**Triggers:**
- Every push to `main` → all 3 jobs run.
- Every pull request targeting `main` → jobs 1 & 2 run (no release).
- Every push to `release/**` branch → all 3 jobs run.

---

## 2. Development Workflow (always follow this)

### Daily push flow

```bash
# 1. Stage all changes
git add -A

# 2. Commit with a descriptive message
git commit -m "feat: <description of change>"

# 3. Push to GitHub → triggers CI/CD automatically
git push origin main
```

### Clean deployment rule

> **Always run `./gradlew clean` before a release build.**
> The CI/CD pipeline enforces this automatically.  When building locally for
> release, also run clean first:
> ```bash
> ./gradlew clean assembleRelease
> ```

---

## 3. Android Project Conventions

| Setting | Value |
|---|---|
| Language | Kotlin |
| Min SDK | 35 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Build System | Gradle (Kotlin DSL) |
| Java Version | 11 (source & target), JDK 17 on CI |
| Package | `com.example.adrive` |

### Dependency management

- All versions are declared in `gradle/libs.versions.toml` (version catalog).
- Never hardcode dependency versions in `build.gradle.kts`; always use `libs.*` aliases.

### Code quality rules

1. **Remove unused imports** before every commit.
2. **No commented-out dead code** – delete it.
3. **No `TODO` without a GitHub Issue** – link or resolve.
4. Run lint locally before pushing: `./gradlew lint`.
5. Fix all lint errors; warnings must be reviewed and suppressed with a comment if intentional.

---

## 4. Secrets & Signing

- Store signing keystore passwords in GitHub Secrets, **never in source code**.
- Required secrets for signed release builds:
  - `KEYSTORE_FILE` (base64-encoded keystore)
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
  - `STORE_PASSWORD`
- `local.properties` and `*.jks` / `*.keystore` are in `.gitignore` and must never be committed.

---

## 5. Branching Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code |
| `feature/<name>` | New features (PR into `main`) |
| `fix/<name>` | Bug fixes (PR into `main`) |
| `release/<version>` | Release candidates |

---

## 6. Getting Started (fresh clone)

```bash
git clone https://github.com/pritam003/Adrive.git
cd Adrive
./gradlew assembleDebug   # build debug APK
./gradlew test            # run unit tests
```

Open in **Android Studio** → the project will sync automatically.

---

## 7. CI/CD Artifacts

After every successful CI run you can download:

| Artifact | Retained |
|---|---|
| `adrive-debug-<run#>` | 14 days |
| `adrive-release-<run#>` | 30 days |
| `lint-report-<run#>` | 14 days |

GitHub Releases (tags `v1.0.<run#>`) are created on every push to `main` and contain the unsigned release APK.

