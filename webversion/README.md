# Adrive — Web Version

This folder will contain the web application for Adrive.

## Planned Stack (update when finalised)

- Framework : React / Next.js (TBD)
- Language  : TypeScript
- Styling   : Tailwind CSS
- Package manager: npm / pnpm

## Getting Started

```bash
cd webversion
npm install
npm run dev
```

## CI/CD

Pushes to `main` that touch files inside `webversion/**` automatically trigger
the **Web CI/CD** GitHub Actions workflow (`.github/workflows/web-ci-cd.yml`).

