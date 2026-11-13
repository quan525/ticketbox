# Ticketbox

Movie reservations built with Spring Boot 4, Java 17, Spring Security, JPA and React/Vite. Spring Boot serves the built interface and API together under `/api/`.

## Local development

Java 17 and Node.js/npm are required to build. A local Java installation under `.tools/java/` is also supported by `scripts/run-local.ps1`.

```powershell
cd frontend
npm ci
npm run build
cd ..
./scripts/run-local.ps1
```

Open http://localhost:8080/api/ for the booking site and http://localhost:8080/api/#admin for operations. The `local` profile uses the persistent H2 database at `.local/ticketbox.mv.db`, disables automatic SQL initialization, and enables mock payments by default. Sign in with an account provisioned in your local database.

For frontend development, run `npm run dev` in `frontend` while the backend runs. http://localhost:5173 proxies `/api` requests to port 8080. Port 5173 is only the Vite development server; the packaged app uses port 8080.

To package the complete app after building the frontend:

```powershell
./gradlew.bat bootJar -x test
java -jar build/libs/ticketbox-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Configuration and deployment

The default profile uses PostgreSQL. Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a `JWT_SECRET` of at least 32 random characters. `PORT` or `SERVER_PORT` selects the HTTP port; `JWT_EXPIRATION_MS` defaults to 1800000. Hibernate schema updates use `JPA_DDL_AUTO=update`. Health: `/api/actuator/health`.

For Docker, copy `.env.example` to `.env`, replace the database password and JWT secret, then run `docker compose up --build -d`. Compose builds the frontend into the Spring Boot image and persists PostgreSQL in a named volume. `API_PORT` defaults to 8080. Render and Fly deployment manifests are included in `render.yaml` and `fly.toml`; configure deployment secrets separately.

On Render, create a Blueprint from the GitHub repository and select the branch that contains `render.yaml`. The Blueprint provisions the web service, PostgreSQL, and a private Redis-compatible Key Value instance. GitHub Actions builds the Docker image on pull requests and pushes to `main` or `codex/render-deploy`. Render waits for the linked commit's CI checks before automatic deployment.

After Render reports the service healthy, open `https://<service>.onrender.com/api/`. To enable SePay, configure the private service settings described in [SePay setup](docs/SEPAY.md), then point SePay at `https://<service>.onrender.com/api/payments/sepay/webhook`. The Blueprint defaults to free plans and disables demo seeding.


`src/main/resources/data.sql` is an opt-in demo seed with 20 movies. The default `SQL_INIT_MODE=never` preserves existing data. For the Compose PostgreSQL environment, `docker compose --profile seed run --rm seed` runs the SQL file. Review the SQL and back up existing data before using a seed. Seeded credentials are development-only; local account passwords may differ from the SQL seed.

## Google sign-in

Set `GOOGLE_CLIENT_ID` to a Google OAuth Web application client ID and allow the frontend origin in the Google client configuration. The backend verifies Google ID tokens and creates customer accounts. No client secret is required for this ID-token flow. Without a client ID, Google sign-in is disabled and username/password sign-in remains available.

## Reservations and transactions

Customers select a show and seats. A reservation atomically locks the selected show seats and holds them for ten minutes. A competing booking cannot reserve the same held seats. Payment amounts come from the booking total. Completing payment locks the booking, payment and seats, verifies that the hold is still valid, and confirms the booking and seats in one transaction. Cancellation releases pending held seats and cancels pending payment requests.

Expired holds are detected during seat availability/reservation and payment operations. The admin interface also displays effective expiry. There is currently no scheduled cleanup job.

## SePay payments

SePay QR checkout, authenticated bank webhooks, automatic confirmation, duplicate protection, and a payment-event review panel are implemented. Configure environment variables and a public webhook URL before enabling it. Start with SePay Test mode. See [SePay setup](docs/SEPAY.md). Local mock confirmation is disabled whenever SePay is enabled.

## QR demo payments

The local profile enables fake checkout via its existing configuration. Its QR encodes plain text for scanning practice. The local buttons simulate success or decline without verifying a transfer. SePay Test mode and live webhook confirmation are separate flows described in [SePay setup](docs/SEPAY.md). Production defaults to mock payments disabled. Do not send real money through the local preview.

Mock endpoints: `GET /api/payments/mock/config`, `GET /api/payments/{id}/mock-checkout`, and `POST /api/payments/{id}/mock-confirm`. Real bank transfers still require verification; scanning a QR does not confirm payment. Demo transaction references begin with `MOCK-` and are excluded from admin verified revenue.

## Admin workspace

Open `/api/#admin`. Overview summarizes screenings, active reservations and payments. Movies supports create, edit, archive and restore; movies with remaining screenings cannot be archived. Showtimes schedules screenings with room overlap checks and automatic seat generation in a transaction. Rooms creates a room and its physical seats together. Bookings provides history and pending-reservation cancellation. Payments provides manual verification and rejection. Customers provides account history, roles and permissions.

Advanced exports, notifications and a full audit history are deferred.

### Employees, customers and module permissions

Customers contains customer profiles and booking history; it has no role or permission controls. Employees is a separate administrator-only screen for staff roles and module access. Add employee promotes an existing customer account to Moderator (employee). Returning an employee to Customer removes staff access and resets overrides. Legacy customer-role accounts with custom operational access appear under Employees as well.

In Employees, select Permissions, turn off Use role defaults, then choose access by module:

| Module | View | Manage |
| --- | --- | --- |
| Movies | Catalog records | Create, edit, archive, restore |
| Showtimes | Screenings and occupancy | Schedule and generate show seats |
| Rooms & seats | Rooms and layouts | Create/update rooms and physical seats |
| Bookings | All reservation records | Create/cancel customer reservations |
| Payments | Payment records | Create, confirm, reject payments |
| Customers | Customer profiles | No separate manage action |
| Employees & access | Admin only | Admin only |

Manage includes View of the same module. Removing View removes Manage. Hidden modules are also filtered from the backend workspace response, so a Bookings employee does not receive payment or customer-profile data without those permissions. Scheduling may include public movie and room choices; public movie/show browsing remains available. Payment review eligibility is supplied independently so payment staff do not need the Bookings module. Customer booking history requires View bookings in addition to View customers.

Admins retain full access. Moderators default to all cinema modules; customers default to their own bookings/payments only. Custom permissions replace role defaults. Role changes reset overrides. Only admins can change roles or permissions, and they cannot change their own access. Backend permissions are loaded on every authenticated request; the interface refreshes access every 30 seconds. Existing legacy View operations grants preserve their previous read scope and are represented as individual module views in the editor.

Permission updates: `PATCH /api/admin/users/{id}/permissions` with `use_role_defaults` and a `permissions` array containing `VIEW_MOVIES`, `MANAGE_MOVIES`, `VIEW_SHOWS`, `MANAGE_SHOWS`, `VIEW_ROOMS`, `MANAGE_ROOMS`, `VIEW_BOOKINGS`, `MANAGE_BOOKINGS`, `VIEW_PAYMENTS`, `MANAGE_PAYMENTS`, and/or `VIEW_CUSTOMERS`. Role updates use `PATCH /api/admin/users/{id}/role`.

## Automated checks

Backend: `./gradlew.bat test` (or `./scripts/run-local.ps1 -Test`). Frontend: `npm test` from `frontend`. Tests were skipped for the latest admin permission work at the user's request; frontend production build and backend compilation/package builds are separate from tests.

## Refresh sessions

Redis stores hashed refresh tokens with a 14-day absolute lifetime. The browser receives an HttpOnly, SameSite=Strict cookie. Sign-in, signup and Google login issue a session. An expired access JWT triggers one refresh and one retry. Redis atomically rotates the cookie token and logout revokes it. Access JWTs already issued expire on their normal schedule.

Docker Compose starts Redis on its private network. For native development, run Redis on loopback before starting the local profile:

```powershell
docker run -d --name ticketbox-redis -p 127.0.0.1:6379:6379 -v ticketbox-redis-data:/data redis:7-alpine redis-server --appendonly yes
```

For a hosted deployment, set `TICKETBOX_REDIS_URL` through the host's private settings and use HTTPS. `REFRESH_TOKEN_TTL` defaults to `14d`. Secure cookies are enabled by default. The local profile allows HTTP on localhost. Sign in again after upgrading to obtain a refresh cookie.

### Upgrade existing PostgreSQL reservation deadlines

Before starting the version that uses Java `Instant`, pause booking traffic, stop the old app, and back up the database. Run the migration against the application schema with `psql` connected using your usual secure configuration:

```sh
psql -v legacy_zone='<previous JVM timezone>' -f src/main/resources/db/reservation-deadlines-timestamptz.sql
```

Use `UTC` if the old backend wrote these timestamps in UTC, or `Asia/Ho_Chi_Minh` if it wrote them in Vietnam time. The migration converts both deadline columns in one transaction and skips columns already converted. Start the updated app after the migration commits. Fresh PostgreSQL databases created by Hibernate already use the new types. The SQL script is PostgreSQL-specific.
