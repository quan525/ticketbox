# Ticketbox

Spring Boot API for movies, shows, seat reservations, bookings, and payments.

React + Vite provides a responsive cinema interface with movie search, date/language filters, watchlists, signup/login, interactive seat maps, ten-minute reservations, booking history, cancellation, and payment requests. Prices and seat availability are checked by the server. Payment requests remain pending until a cinema operator verifies payment; no live payment gateway is connected.

## Run locally

Requirements: Java 17 and Node.js 22.12+ (or 20.19+).

In one PowerShell terminal at the project root:

```powershell
.\scripts\run-local.ps1
```

The `local` profile uses a persistent H2 database under `.local/`. An empty database receives eight fictional movies, twelve future screenings, and sixty seats per screening. It does not run the legacy destructive `data.sql`. Create your own account in the interface. The helper recognizes Java in `JAVA_HOME`, on PATH, or under `.tools/java/`.

In another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open [Ticketbox](http://127.0.0.1:5173/). Vite forwards `/api` requests to Spring Boot on port 8080.

## Build the complete application

```powershell
cd frontend
npm ci
npm run build
cd ..
.\gradlew.bat bootJar
```

The frontend build is bundled into the Spring Boot jar. Run that jar with `--spring.profiles.active=local` and open [the bundled interface](http://localhost:8080/api/). Rebuild the frontend before packaging changes. Docker builds both parts automatically.

## Tests

```powershell
.\scripts\run-local.ps1 -Test
cd frontend
npm test
```

Backend tests use isolated, in-memory H2 databases and transactional rollback, with no development seed dependency. HTTP integration tests cover guest browsing, signup, account identity, seat serialization, server-calculated totals, payment permissions, cancellation, and cross-account booking access. Frontend tests exercise catalog filters, watchlists, authentication, seat selection, guest sign-in, payment states, and error handling.

## Payment flow

## Google sign-in

Google Identity Services is integrated into both login and signup. Spring Boot verifies Google's signed ID token (signature, audience, issuer, expiration, and verified email), creates or retrieves a user by Google's stable subject ID, and issues the normal Ticketbox bearer token. Email matches never automatically link an existing password account. A unique `google_subject` column is added by Hibernate when `JPA_DDL_AUTO=update`; deployments using schema migrations must add this nullable, unique column to `users`.

To enable it, create a **Web application** OAuth client in the [Google Auth Platform](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid). Configure branding, audience/test users as appropriate, and authorize JavaScript origins `http://localhost`, `http://localhost:8080`, and `http://localhost:5173`. Use localhost in your browser when testing. Add your HTTPS origin for deployment. This popup/callback integration needs no redirect URI or client secret.

For local PowerShell startup:

```powershell
$env:GOOGLE_CLIENT_ID = 'your-web-client-id.apps.googleusercontent.com'
.\scripts\run-local.ps1
```

For Docker Compose, set `GOOGLE_CLIENT_ID` in `.env` and recreate the API service. The frontend reads the public client ID from `/api/auth/google/config` at runtime, so it requires no separate npm environment setting or frontend rebuild. With no client ID, Google sign-in stays unavailable while password login works normally.

`POST /api/auth/google` accepts JSON `{ "credential": "google-id-token" }`. Google credentials are not stored or logged. The current integration has automated tests using mocked Google verification; a real Google account round trip requires configuring your client ID.

## Payment API

All routes are under `/api`. Signup, login, catalog/seat-map GET requests, frontend assets, and health are public. Booking and payment routes require a bearer token; booking access is restricted to the owner or cinema operators. Catalog changes require an operator account. `GET /api/auth/me` returns the signed-in account.

1. Create a booking with `POST /api/bookings/newbook`. The server calculates the total and holds its seats for ten minutes.
2. Create a pending payment with `POST /api/payments`:

   ```json
   { "booking_id": 1, "method": "CREDIT_CARD" }
   ```

3. After a gateway confirms payment, an admin or moderator completes it with `PATCH /api/payments/{id}/complete`:

   ```json
   { "transaction_id": "gateway-transaction-id" }
   ```

   This idempotently marks the payment paid, confirms the booking, and books the seats. Customers cannot mark their own payments as paid.

Other payment routes:

- `GET /api/payments/{id}`
- `GET /api/payments/booking/{bookingId}`
- `PATCH /api/payments/{id}/cancel`

Users may access only their own payments; admins and moderators may access all payments.

## Deploy with Docker Compose

Requirements: Docker with Compose.

```sh
cp .env.example .env
# Replace POSTGRES_PASSWORD and JWT_SECRET in .env before starting.
docker compose up --build -d
docker compose ps
```

The API is available at `http://localhost:8080/api`. Check readiness at
`http://localhost:8080/api/actuator/health`.

To deploy a new build:

```sh
docker compose build api
docker compose up -d api
```

Database data is persisted in the `postgres-data` volume. SQL seed is off by default.

## Seed with SQL

`src/main/resources/data.sql` is an idempotent PostgreSQL seed: demo movies, one 60-seat room, upcoming shows, and two accounts.

| username | password     | role  |
|----------|--------------|-------|
| admin1   | password123  | ADMIN |
| user     | password123  | USER  |

On app start (Docker, Render, Fly, or any PostgreSQL deploy):

```sh
# .env or hosting env
SQL_INIT_MODE=always
```

Hibernate creates or updates tables first (`JPA_DDL_AUTO=update`), then Spring runs `data.sql`. Re-running the seed skips rows that already exist.

To apply the same file with `psql` after the API has created the schema:

```sh
docker compose --profile seed up seed
```

A destructive wipe lives at `src/main/resources/db/legacy-reset.sql` and is not run automatically.

## Deploy online

The Docker image includes the bundled React UI. Hosting should set `JWT_SECRET` (32+ characters) and a PostgreSQL URL.

- **Render:** this repo includes `render.yaml`. In the Render dashboard choose **New → Blueprint** and connect the GitHub repository. The blueprint provisions Postgres, sets `SQL_INIT_MODE=always`, and deploys the API. After the first deploy, open `https://<service>.onrender.com/api/`.
- **Fly.io:** `fly launch` / `fly deploy` using `fly.toml` and `fly postgres create` / `fly postgres attach`. `DATABASE_URL` is converted to a JDBC URL automatically.
- **Any `postgres://` host** (Railway, Render, Neon): set `DATABASE_URL`; Ticketbox maps it to Spring's datasource. Or set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` yourself.

Add your public HTTPS origin to the Google OAuth client if you use Google sign-in.

## Local verification

```sh
./gradlew test
./gradlew bootJar
```
