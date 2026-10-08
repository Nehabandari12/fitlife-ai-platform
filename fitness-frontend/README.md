# FitLife frontend

React (Vite) single-page app for FitLife: sign-in through Keycloak (OAuth 2.0 authorization code with
PKCE), activity entry and history, and the AI recommendation for each activity.

```bash
npm ci
npm run dev      # http://localhost:5173
npm run lint
npm run build
```

Settings, all optional (Vite reads them from `.env.local` or the environment):

| Variable | Default |
|---|---|
| `VITE_API_URL` | `http://localhost:8080/api` (the gateway) |
| `VITE_KEYCLOAK_URL` | `http://localhost:8181` |
| `VITE_REDIRECT_URI` | `http://localhost:5173` |

Requires Node.js 20.19+ or 22.12+ (Vite 7). Full setup: the [project README](../README.md).
