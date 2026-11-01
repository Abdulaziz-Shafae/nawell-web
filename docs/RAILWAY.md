# Deploy Nawell on Railway

Source: [Abdulaziz-Shafae/nawall-web](https://github.com/Abdulaziz-Shafae/nawall-web), branch `main`.

Create three services in the **same project, environment and region**, named exactly `MySQL`, `backend`, and `frontend`. The names are used by the ready-to-paste reference variables. Railway manages MySQL credentials; do not paste your local database password or the Docker root password into the app variables.

| Service | Source / root directory | Health check | Network |
|---|---|---|---|
| MySQL | Railway MySQL database template | Template-managed | Private |
| backend | `nawall-web` / `/backend` | `/api/v1/health` | Private, port 8080 |
| frontend | `nawall-web` / `/frontend` | `/healthz` | Public HTTPS, target port 80 |

## Steps

1. Create a Railway project and add its MySQL template. Keep the generated database variables and persistent database volume.
2. Add two GitHub services from the same repository, choosing `main`. Set their names and root directories from the table. Grant Railway access to the private repository when prompted. Both services use their detected Dockerfiles; leave custom build/start commands empty. In each service’s Deploy settings, set its health-check path from the table, timeout 300 seconds, restart policy On Failure (10 retries), and one replica.
3. In `frontend` → Settings → Networking, generate a public domain targeting port **80**. This creates the `RAILWAY_PUBLIC_DOMAIN` used by backend references. The backend does not need its own public domain.
4. In `backend`, attach a persistent volume mounted at **`/app/uploads`**. This is required for uploaded profile pictures and logos to survive deployments. Keep one backend replica: sessions are in-memory and uploads use a single filesystem volume.
5. Open `backend` → Variables → Raw Editor. Paste the contents of [backend.env.example](../deploy/railway/backend.env.example). Replace the `REPLACE_WITH_...` values with your actual provider credentials in Railway only. `OPENAI_API_KEY` is required at startup by the existing OpenAI bean. For integrations you are not enabling yet, remove their placeholder variables or leave them blank; the related feature will need real credentials later. Brevo needs a verified sender; Zoom uses the supplied server-to-server account/client settings; email uses the existing Brevo instance/token; Apify enables LinkedIn import.
6. Open `frontend` → Variables → Raw Editor. Paste [frontend.env.example](../deploy/railway/frontend.env.example). It contains only runtime proxy settings. **Do not add any API keys or database passwords to the frontend. Do not set `VITE_API_BASE_URL`** for this deployment: the browser uses same-origin `/api/...`, and nginx forwards requests privately to Spring Boot.
7. Apply the staged changes. Deploy MySQL first, then backend, then frontend. Use the health-check paths and restart settings from step 2. Wait for successful health checks. The backend JVM timezone is explicitly Asia/Riyadh, matching the frontend’s negotiation and session dates. Both apps use `PORT`; the backend listens on IPv6 for Railway's private network, and nginx listens on IPv4/IPv6.
8. Open the frontend HTTPS domain. Check `/healthz`, then `/api/v1/health`; the latter should return `{"status":"UP"}`. Register a test account with a selected image, log in, refresh the profile, and confirm the image stays visible. Test email verification before learning requests, and verify AI and Zoom with configured credentials. Network requests should retain the documented original method/path, with multipart requests only for the new image-upload and existing CV-upload endpoints.

Railway’s current [configuration reference](https://docs.railway.com/config-as-code/reference) marks legacy `railway.json`/`railway.toml` deployment configuration as deprecated. This guide uses supported dashboard settings and Dockerfile detection for new services.

## Runtime behavior

nginx reads `BACKEND_HOST`, `BACKEND_PORT`, and `PORT` when the container starts. Its resolver comes from the container's DNS configuration and refreshes backend DNS every 30 seconds; the frontend can start while backend deployment is still pending. HTTPS forwarding and cookies stay on the public frontend origin. An unavailable backend produces a real HTTP error; no mock data is used.

The backend health route reports that the application started. It does not verify provider credentials or attempt a live AI/email/email/Zoom call. `DDL_AUTO=update` preserves the original schema bootstrap behavior. A fresh Railway MySQL database has no old local accounts, skills or balances: import your local data separately if you want to retain them, and do not commit database dumps containing personal data or credentials.

The uploaded backend's plaintext password persistence remains unchanged and documented in the README. Use test accounts until a password-storage migration is completed. Creating a Railway project may incur platform usage charges according to your account plan.

## Troubleshooting

| Symptom | Check |
|---|---|
| Backend fails to start | `OPENAI_API_KEY`, MySQL health, correct service names in variable references, and JDBC `DB_URL` |
| Frontend returns 502 for `/api` | Backend health/logs, `BACKEND_HOST`, `BACKEND_PORT`, same project/environment/region |
| Upload disappears after redeploy | Backend volume mounted at `/app/uploads` and `UPLOAD_DIR=/app/uploads` |
| Mutation returns “Request origin is not allowed” | `ALLOWED_ORIGINS` must match the actual public frontend HTTPS origin without a trailing slash |
| Login succeeds but session is missing | Use the frontend HTTPS domain consistently; keep `SESSION_COOKIE_SECURE=true` and one backend replica |
| Email/AI/Zoom/email action fails | Replace the relevant backend-only provider placeholders with actual credentials and satisfy provider setup requirements |

Deployment files have been prepared locally. Docker/Railway execution and real provider delivery still require the Railway project and credentials; they are not claimed as completed by local builds.

References: [Railway variables](https://docs.railway.com/variables/reference), [Dockerfiles](https://docs.railway.com/builds/dockerfiles), [configuration as code](https://docs.railway.com/config-as-code), [private networking](https://docs.railway.com/networking/private-networking), [official nginx entrypoint](https://github.com/nginx/docker-nginx/blob/master/entrypoint/docker-entrypoint.sh).
