# Adrive — Project Skill Guide

> **IMPORTANT**: Always read and follow ALL instructions in this file when working on the Adrive project.

---

## 0. Repository Overview

```
Adrive/                            ← monorepo root
├── .github/
│   └── workflows/
│       ├── android-ci-cd.yml      ← Mobile CI/CD (triggers on mobileversion/**)
│       └── web-ci-cd.yml          ← Web CI/CD    (triggers on webversion/**)
├── .gitignore                     ← root-level, covers both sub-projects
├── SKILL.md                       ← this file — always follow
├── mobileversion/                 ← Android (Kotlin) application
│   ├── app/
│   │   └── src/
│   │       ├── main/
│   │       ├── test/
│   │       └── androidTest/
│   ├── build.gradle.kts
│   ├── gradle/
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
└── webversion/                    ← Web application (to be bootstrapped)
    └── README.md
```

| Item | Value |
|---|---|
| GitHub Repo | https://github.com/pritam003/Adrive |
| Default Branch | `main` |
| CI/CD System | GitHub Actions |
| Mobile workflow | `.github/workflows/android-ci-cd.yml` |
| Web workflow | `.github/workflows/web-ci-cd.yml` |

---

## 1. CI/CD Pipelines

### Mobile (Android) pipeline

**Trigger:** any push/PR that changes files under `mobileversion/**`

```
Push touches mobileversion/**
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

Jobs:
1. **Build & Unit Test** – `cd mobileversion && ./gradlew clean test assembleDebug`
2. **Lint Check** – `cd mobileversion && ./gradlew lint`
3. **Release Build** – `cd mobileversion && ./gradlew clean assembleRelease` → creates GitHub Release tag `mobile-v1.0.<run#>`

### Web pipeline

**Trigger:** any push/PR that changes files under `webversion/**`

Currently contains placeholder steps.
Activate by adding `package.json` to `webversion/` and uncommenting the build/test/deploy steps in `web-ci-cd.yml`.

---

## 2. Development Workflow (always follow this)

### Push mobile changes

```bash
# 1. Make changes inside mobileversion/
# 2. Stage & commit
git add mobileversion/
git commit -m "feat(mobile): <description>"

# 3. Push → triggers Android CI/CD
git push origin main
```

### Push web changes

```bash
# 1. Make changes inside webversion/
# 2. Stage & commit
git add webversion/
git commit -m "feat(web): <description>"

# 3. Push → triggers Web CI/CD
git push origin main
```

### Push changes to both

```bash
git add -A
git commit -m "chore: <description covering both>"
git push origin main   # both pipelines trigger independently
```

### Clean deployment rule

> **Always run `./gradlew clean` (mobile) before a release build.**
> The CI/CD pipeline enforces this automatically. When building locally:
> ```bash
> cd mobileversion
> ./gradlew clean assembleRelease
> ```

---

## 3. Android / Mobile Conventions

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

- All versions in `mobileversion/gradle/libs.versions.toml` (version catalog).
- Never hardcode versions in `build.gradle.kts`; always use `libs.*` aliases.

### Code quality rules

1. **Remove unused imports** before every commit.
2. **No commented-out dead code** – delete it.
3. **No `TODO` without a GitHub Issue** – link or resolve.
4. Run lint before pushing: `cd mobileversion && ./gradlew lint`.
5. Fix all lint errors; suppress warnings with a comment only if intentional.

### Opening in Android Studio

Open the **`mobileversion/`** folder (not the repo root) in Android Studio.
The project will sync automatically from `mobileversion/settings.gradle.kts`.

---

## 4. Web Conventions (to be expanded)

- All web source lives in `webversion/`.
- Keep `webversion/` completely independent of `mobileversion/`.
- Use TypeScript strictly (`strict: true`).
- Never hardcode API endpoints – use environment variables.

---

## 5. Secrets & Signing

- Store all secrets in **GitHub Secrets** – never commit them.
- Mobile signing secrets:
  - `KEYSTORE_FILE` (base64-encoded keystore)
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
  - `STORE_PASSWORD`
- `mobileversion/local.properties` and `*.jks` / `*.keystore` are in `.gitignore`.
- Web API keys / tokens: store as GitHub Secrets and reference as env vars in `web-ci-cd.yml`.

---

## 6. Branching Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code (both platforms) |
| `feature/mobile-<name>` | Mobile-only feature (PR into `main`) |
| `feature/web-<name>` | Web-only feature (PR into `main`) |
| `fix/<name>` | Bug fixes (PR into `main`) |
| `release/<version>` | Release candidates |

---

## 7. Getting Started (fresh clone)

```bash
git clone https://github.com/pritam003/Adrive.git
cd Adrive

# — Mobile —
cd mobileversion
./gradlew assembleDebug   # build debug APK
./gradlew test            # run unit tests
cd ..

# — Web — (once bootstrapped)
cd webversion
npm install
npm run dev
```

Open `mobileversion/` in **Android Studio** for mobile development.
Open `webversion/` in **VS Code** or your preferred editor for web development.

---

## 8. CI/CD Artifacts

### Mobile

| Artifact | Retained |
|---|---|
| `adrive-debug-<run#>` | 14 days |
| `adrive-release-<run#>` | 30 days |
| `lint-report-<run#>` | 14 days |

GitHub Releases (tags `mobile-v1.0.<run#>`) contain the unsigned release APK.

### Web

| Artifact | Retained |
|---|---|
| `adrive-web-<run#>` | 14 days (once enabled) |

---

## 9. Clean Deployment Checklist

Before every release (run this mentally or as a script):

- [ ] `git status` — no uncommitted changes
- [ ] `cd mobileversion && ./gradlew clean` — stale artifacts removed
- [ ] Lint passes: `./gradlew lint`
- [ ] Unit tests pass: `./gradlew test`
- [ ] No unused imports, no dead code, no unlinked TODOs
- [ ] Secrets not hardcoded
- [ ] `git push origin main` — CI pipeline green ✅
