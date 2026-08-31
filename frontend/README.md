# Classify Frontend

React 18, TypeScript, Vite, and Tailwind provide the role-based UI for the Spring Boot API.

## Setup

```bash
cp .env.example .env
npm ci
npm run dev
```

The development server runs at `http://localhost:5173` and proxies `/api` to the backend at `http://localhost:8080`.

## Checks

```bash
npm run lint
npm run build
```

Teacher routes are under `/teacher/class-sections` and `/teacher/subjects`. Student routes are under `/student/class-section` and `/student/subjects`. Authentication state is stored locally and every protected API request receives the JWT through the shared Axios interceptor.
