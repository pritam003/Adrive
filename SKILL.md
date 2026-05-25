# Adrive — Project Skill Guide

> **IMPORTANT**: Always read and follow ALL instructions in this file when working on the Adrive project.
>
> 🔒 **ACTIVE WORK ZONE: `mobileversion/` only.**
> Do NOT modify any files inside `webversion/` unless explicitly asked.

---

## 0. Repository Overview

```
Adrive/                            ← monorepo root
├── .github/
│   └── workflows/
│       ├── android-ci-cd.yml      ← Mobile CI/CD  (triggers on mobileversion/**)
│       └── web-ci-cd.yml          ← Web CI/CD     (triggers on webversion/**)
├── .gitignore                     ← root-level, covers both sub-projects
├── SKILL.md                       ← this file — always follow
│
├── mobileversion/                 ← ✅ ACTIVE — Android (Kotlin) application
│   ├── app/
│   │   └── src/
│   │       ├── main/              ← source code, resources, AndroidManifest
│   │       ├── test/              ← JVM unit tests
│   │       └── androidTest/       ← instrumented tests
│   ├── build.gradle.kts
│   ├── gradle/
│   │   ├── libs.versions.toml     ← version catalog (ALL versions declared here)
│   │   └── wrapper/
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat
│   └── settings.gradle.kts
│
└── webversion/                    ← 🔒 READ-ONLY for mobile work
    ├── src/                       ← React + TypeScript components
    │   └── components/            ← FileGrid, Topbar, Sidebar, UploadProgress …
    ├── api/                       ← Azure Functions (Node/TypeScript)
    │   └── src/                   ← auth.ts, storage.ts, index.ts
    ├── public/                    ← static assets (sw.js service worker)
    ├── index.html
    ├── package.json               ← React 18, React Router 6, Vite 5, TypeScript
    ├── vite.config.ts
    ├── staticwebapp.config.json   ← Azure SWA routing config
    └── tsconfig.json
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

**Trigger:** push / PR that changes any file under `mobileversion/**`

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
3. **Release Build** – `cd mobileversion && ./gradlew clean assembleRelease` → GitHub Release tag `mobile-v1.0.<run#>`

### Web pipeline (Azure Static Web Apps)

**Trigger:** push / PR that changes any file under `webversion/**`

```
Push touches webversion/**
        │
        ▼
 ┌──────────────────────────────────┐
 │  npm ci → eslint → vite build    │
 │  → Azure SWA Deploy              │
 └──────────────────────────────────┘
```

- App location  : `webversion/`
- API location  : `webversion/api/`
- Output folder : `webversion/dist/`
- Hosting       : Azure Static Web Apps
- Secret needed : `AZURE_STATIC_WEB_APPS_API_TOKEN` (in GitHub Secrets)

---

## 2. Development Workflow

### 🔒 Mobile-only work (default mode)

```bash
# 1. Make changes inside mobileversion/  ← ONLY here
git add mobileversion/
git commit -m "feat(mobile): <description>"
git push origin main    # triggers Android CI/CD only
```

### Web changes (only when explicitly asked)

```bash
git add webversion/
git commit -m "feat(web): <description>"
git push origin main    # triggers Web CI/CD only
```

### Clean deployment rule

> **Always run `./gradlew clean` before a release build.**
> CI enforces this. Locally:
> ```bash
> cd mobileversion && ./gradlew clean assembleRelease
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

- All versions declared in `mobileversion/gradle/libs.versions.toml` (version catalog).
- Never hardcode versions in `build.gradle.kts`; always use `libs.*` aliases.

### Code quality rules

1. **Remove unused imports** before every commit.
2. **No commented-out dead code** – delete it.
3. **No `TODO` without a GitHub Issue** – link or resolve.
4. Run lint before pushing: `cd mobileversion && ./gradlew lint`.
5. Fix all lint errors; suppress warnings with a comment only if intentional.

### Opening in Android Studio

Open the **`mobileversion/`** folder (not the repo root) in Android Studio.
The project syncs from `mobileversion/settings.gradle.kts`.

---

## 4. Web App Stack (reference — do not modify unless asked)

| Layer | Technology |
|---|---|
| Framework | React 18 |
| Language | TypeScript 5 |
| Bundler | Vite 5 |
| Router | React Router DOM 6 |
| Icons | lucide-react |
| Code style | ESLint + Prettier |
| API backend | Azure Functions (Node.js 20, TypeScript) |
| Hosting | Azure Static Web Apps |
| Auth | Entra ID — Device Code Flow + JWT cookie |
| File storage | Azure Blob Storage (via `api/src/storage.ts`) |
| Upload strategy | Service Worker (`public/sw.js`) — survives page reload |

### Key web source files

| File | Purpose |
|---|---|
| `src/App.tsx` | Root component + routes |
| `src/AuthGate.tsx` | Authentication wrapper |
| `src/api.ts` | HTTP client for API calls |
| `src/swUpload.ts` | Service-worker upload coordination |
| `src/components/FileGrid.tsx` | Main file browser grid |
| `src/components/UploadProgress.tsx` | Upload progress indicator |
| `api/src/auth.ts` | Entra Device Code Flow |
| `api/src/storage.ts` | Azure Blob Storage operations |
| `staticwebapp.config.json` | SWA routing rules |

---

## 5. Secrets & Signing

| Secret | Where used |
|---|---|
| `KEYSTORE_FILE` | Mobile signing (base64 keystore) |
| `KEY_ALIAS` | Mobile signing |
| `KEY_PASSWORD` | Mobile signing |
| `STORE_PASSWORD` | Mobile signing |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | Web — Azure SWA deploy token |

- `mobileversion/local.properties` and `*.jks` / `*.keystore` are in `.gitignore` — never commit.
- Web API keys: store as GitHub Secrets, reference in `web-ci-cd.yml`.

---

## 6. Branching Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready (both platforms) |
| `feature/mobile-<name>` | Mobile-only feature → PR into `main` |
| `feature/web-<name>` | Web-only feature → PR into `main` |
| `fix/<name>` | Bug fix → PR into `main` |
| `release/<version>` | Release candidates |

> The `master` branch is **deprecated** — all content has been migrated into
> `main` under `mobileversion/` and `webversion/`. Do not push to `master`.

---

## 7. Getting Started (fresh clone)

```bash
git clone https://github.com/pritam003/Adrive.git
cd Adrive

# ── Mobile (Android) ──────────────────────────
cd mobileversion
./gradlew assembleDebug   # build debug APK
./gradlew test            # run unit tests
cd ..

# ── Web ──────────────────────────────────────
cd webversion
npm install
npm run dev               # http://localhost:5173
```

- Mobile: open `mobileversion/` in **Android Studio**
- Web: open `webversion/` in **VS Code**

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

Deployed automatically to Azure Static Web Apps on every push to `main` that touches `webversion/**`.
PR previews are created automatically and torn down on PR close.

---

## 9. Clean Deployment Checklist

- [ ] `git status` — no uncommitted changes
- [ ] `cd mobileversion && ./gradlew clean` — stale artifacts removed
- [ ] Lint passes: `./gradlew lint`
- [ ] Unit tests pass: `./gradlew test`
- [ ] No unused imports, no dead code, no unlinked TODOs
- [ ] Secrets not hardcoded anywhere
- [ ] `git push origin main` — CI pipeline green ✅
