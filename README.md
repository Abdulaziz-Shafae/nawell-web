# Nawell · نوال

React + Vite frontend connected to the latest uploaded Capstone 3 Spring Boot backend. The projects live separately in `/frontend` and `/backend`. No Loot code or layout was reused.

The visual direction comes from the supplied **Nawell · نوال.pdf**: ivory `#f4f1e9`, navy `#17223b`, crimson `#ba1238`, condensed display lettering, and the presentation's city/handshake collage. The requested product/repository name is **Nawell**. Fonts are bundled locally. Arabic switches the document to RTL; English uses LTR. User content and backend validation messages retain their source language.

## Start locally

Requirements: Node 20.19+ or 22.12+, Java 17+, a running MySQL server, and the credentials for the integrations you want to exercise. The original Maven wrapper is included.

1. Create a MySQL database named `nawall`.
2. Copy `backend/.env.example` to `backend/.env` and replace placeholders. `OPENAI_API_KEY` is required by the uploaded OpenAI bean at startup. Brevo needs a verified sender. Do not commit this file.
3. On Windows, from the repository root:

   ```powershell
   .\scripts\start-backend.ps1
   ```

   The script reads `.env` into the current process without printing its values. Spring Boot does not automatically read a `.env` file. On other platforms export the same variables into the shell, then run `./mvnw spring-boot:run` inside `backend`.

4. In another terminal:

   ```powershell
   cd frontend
   Copy-Item .env.example .env
   npm ci
   npm run dev
   ```

5. Open [localhost:5173](http://localhost:5173). `/api` is proxied to the backend at port 8080. Login uses `JSESSIONID` cookies with `credentials: include`, not an invented JWT. No account credentials are stored in localStorage. Only the language preference is stored there.

To bootstrap an administrator in your own empty database, insert an account with `account_type=ADMIN` using your chosen local credentials. Public registration offers only INDIVIDUAL and COMPANY. There are no committed default accounts/passwords or seeded marketplace records.

## Docker setup

Docker files are included; Docker was not available in the authoring environment, so this route has not been executed here.

Set `DB_USERNAME` to a non-root database username, set separate `DB_PASSWORD` and `DB_ROOT_PASSWORD`, and set these in `backend/.env` for the container deployment:

```dotenv
APP_PUBLIC_BASE_URL=http://localhost:8088
ALLOWED_ORIGINS=http://localhost:8088
```

Then run from the repository root:

```powershell
docker compose --env-file backend/.env up --build
```

Open [localhost:8088](http://localhost:8088). Nginx serves the SPA and forwards `/api/` unchanged to Spring Boot. The database and backend are internal services. Database data persists in the `nawall-db` volume. For HTTPS deployment configure a TLS reverse proxy, set the actual public URL/origin and `SESSION_COOKIE_SECURE=true`; do not use wildcard credentialed CORS. The API base URL can remain empty with a same-origin proxy.

## Implemented workflows

| Area | Connected frontend behavior |
|---|---|
| Accounts | Individual/company registration, login/logout, cookie-session restoration, profile updates, verification email, verification confirmation, personal dashboard |
| Discovery | Active offers, skill/mode/text filters, provider skills/offers/reviews/rating, provider and request search |
| Skills | Add/remove owned catalog skills, AI questions and answer evaluation, assessment history/latest result, verified skill gating |
| Offers | Create verified teaching offers, list/edit/delete owned offers, offer session management |
| Requests | Create from an offer, sent/received/open/urgent lists, participant details, open-request edits/delete/cancel |
| Negotiation | Messages, teacher-first date proposals, proposal price preview from the server, counterpart acceptance, urgency/weekend costs |
| Exchanges | Learner creates after proposal acceptance; teacher accepts/reserves; participant cancellation/refund; learner confirms completion/payout; status filters/details |
| Agreements | Read saved content, AI draft, explicit save/edit, provider/receiver acceptance and acceptance status |
| Sessions | Provider schedules/edits/deletes eligible sessions, learner joins, provider records attendance, Zoom creation/retrieval, safe meeting links |
| Wallet | Real balance/history, simulated purchase, redemption ledger, teaching bonus, eligible refund recovery |
| Reviews | Participant review after completion, received reviews, provider average rating, administrative moderation |
| AI | Matching/explanation, PDF multipart CV extraction, ranked offers, LinkedIn retrieval/skill selection/import, skill relationships, related offers, assessment, fairness and agreement generation |
| Admin | Restricted maintenance of accounts, individual/company profiles, skills, account skills, offers, requests, negotiations, exchanges, agreements, sessions, participants, reviews and transactions; immutable assessments are read-only |

Every API action goes through `frontend/src/api.js` and real `fetch`. There is no mock API, fake logged-in state, demo dataset, fallback balance, or service-worker interception. Empty databases show empty states. Unavailable services show errors. View-only UI controls such as language switching, filters, navigation, and opening dialogs do not fabricate backend calls.

## Network tab

Enable **Preserve log** in Developer Tools → Network, select Fetch/XHR, then use the interface. The request URL and method match the supplied controllers. Examples:

* Login: `POST /api/v1/account/login` with `{email,password}`.
* Offer request: `POST /api/v1/learning-request/create/{offerId}` with `{description,mode}`.
* Negotiation: `POST /api/v1/request-negotiation/respond/{requestId}` with `{message,proposedDate}`.
* Accept a proposal: `PUT /api/v1/request-negotiation/{negotiationId}/accept`.
* Join a session: `POST /api/v1/session/{sessionId}/join/{exchangeId}`.
* CV: `POST /api/v1/ai/cv/extract-skills` with a real multipart `file` field; the browser supplies its boundary.

Negotiation dates use `yyyy-MM-dd HH:mm`; session dates use ISO local datetime. The interface labels and displays dates in Asia/Riyadh. Fetch preserves server error messages. Backend integration requests to Brevo, UltraMsg, Zoom, Apify and OpenAI happen **server-side**; browser Network shows the Nawell endpoint and response, not secret-bearing provider requests. WhatsApp notifications are triggered by supported request/exchange workflows. The uploaded session reminder scheduler sends email; there is no separate WhatsApp reminder endpoint, and none was invented.

The user's original Network-tab instruction was cut off after its arrow. The implementation follows the explicit real-request/exact-route requirements; any additional requested Network behavior still needs that missing text.

## Backend preservation and integration changes

All original packages, models, JPA relationships, repositories, business services and validation rules are retained. No controller path or HTTP method was renamed. [BACKEND-CHANGES.md](docs/BACKEND-CHANGES.md) records the narrow HTTP/configuration changes: credentials from environment, access/origin checks around previously unguarded maintenance routes, no serialized account passwords, admin session profile support, read-only association identities and Zoom flags, readable saved agreements, and frontend verification links. A stale uploaded test constructor was repaired.

The complete generated inventory is [API-CONTRACT.md](docs/API-CONTRACT.md), with machine-readable signatures in [endpoints.json](docs/endpoints.json). Important source-of-truth limitations:

* Purchases are explicitly **simulated** by the backend; no money is charged. Redemption records a token withdrawal/SAR equivalent; there is no payment or payout gateway. The UI states this.
* Legacy `POST /exchange/add` cannot construct the required request/offer relationships from its DTO. It is not exposed as a working create button; use the accepted-proposal workflow.
* Offer updates do not persist `status`, so no fake pause/reactivate action is offered. Zoom-linked sessions cannot be changed/deleted by the supplied service.
* Skills must pass at 70+. Level cutoffs are 70/80/90. Only the teacher initially proposes a date; the counterpart accepts; only the learner creates and completes an exchange. Friday/Saturday add 1 token; urgency is computed by the backend. Redemption needs at least 5 tokens and must leave 3.
* The uploaded backend stores/matches passwords in plaintext. That persistence/authentication behavior has been preserved rather than silently changing the database contract. Hiding serialized passwords and restricting HTTP access does not add password hashing. A production deployment still needs a password-storage migration.
* An AI agreement is a draft until explicitly saved and accepted. Agreement acceptance does not introduce new exchange status transitions.

## Validation

```powershell
cd frontend
npm run build
npm test
npm run check:contracts
npm audit
```

```powershell
cd backend
.\mvnw.cmd "-Dtest=ImageUploadTests,OfferEvaluationTests,ExchangeCompletionTests,WebIntegrationTests" test
.\mvnw.cmd -DskipTests package
```

Recorded results: production frontend build passed; 15 frontend tests passed; 135 method/path checks matched all 127 source endpoints; 19 targeted backend tests passed; dependency audit reported zero vulnerabilities after a compatible source-map-js patch. The normal uploaded `contextLoads` test requires a configured database and OpenAI environment and was not included in the isolated test run. Desktop/mobile English and Arabic rendering and unavailable-server feedback were inspected in the real browser.

**Live database/external-provider end-to-end verification remains pending.** No MySQL service was reachable in the authoring environment and no new external credentials were supplied. The frontend is wired to the original services; successful email/WhatsApp/Zoom/AI delivery has not been claimed without those services running. No mock data is substituted for this limitation.

Framework setup follows the [Vite guide](https://vite.dev/guide/) and [React Router declarative routing](https://reactrouter.com/start/declarative/routing).

## AI offer-price evaluation

Before creating an offer, providers can select **Evaluate price**. It sends the entered description, mode, tokenCost, and capacity to `POST /api/v1/skill-offer/create/{skillId}/evaluate`. Include duration and topics in the description. The server requires an active account and a verified owned skill, and compares up to 20 active mode-compatible offers. It returns FAIR, OVERPRICED, UNDERPRICED, or INSUFFICIENT_INFORMATION with a nullable suggested price, explanation and suggestions. This does not save or modify the offer. The separate **Create offer** action saves the provider’s entered price. Editing any field clears the previous evaluation.

The DTOs and service/controller addition follow the supplied update. Four isolated tests cover no-save behavior, insufficient evidence, inconsistent prices, and verification requirements. Actual provider output still requires configured MySQL and OpenAI credentials.

## Profile image and company logo uploads

Registration, profile editing and administrator profile forms use a file selector with an image preview and removal. Choose JPG or PNG up to 2 MB; the server also checks the decoded format and limits images to 10 million pixels. On form submission, `POST /api/v1/media/images` receives multipart `file`, then the existing registration/profile endpoint receives the saved image URL. Existing DTO fields and database relationships remain unchanged. Images are re-encoded with generated filenames, and served through `GET /api/v1/media/images/{name}`. Uploads are available before login for registration and follow the same trusted-origin/custom-header checks as other mutations.

**Railway:** attach a persistent volume to the backend at `/app/uploads` and set `UPLOAD_DIR=/app/uploads` before uploading. Without persistent storage, images will disappear after redeploys. Keep one backend replica with this filesystem storage. Compose includes an image volume; local storage defaults to `uploads` and is ignored by Git. Uploaded images are public profile media; abandoned registration uploads currently remain in storage.

## Railway deployment

Follow [docs/RAILWAY.md](docs/RAILWAY.md) for the prepared three-service deployment. Paste [backend variables](deploy/railway/backend.env.example) and [frontend variables](deploy/railway/frontend.env.example) into Railway’s Raw Editor. MySQL and domain values use Railway references; replace provider placeholders only in backend Variables. Runtime nginx settings preserve Docker Compose defaults and support Railway private networking. The deployment guide supplies each service’s root directory, health checks and single-replica settings. Attach the upload volume before using images.
