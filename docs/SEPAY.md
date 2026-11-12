# SePay QR bank transfer integration

Ticketbox supports SePay bank webhooks (not the separate hosted Payment Gateway product). The existing local mock checkout remains available when SePay is disabled. When SePay is enabled, local mock confirmation endpoints are disabled even if MOCK_PAYMENTS_ENABLED=true.

## Demo and live checkout

Local demo QR codes contain plain text. Use the demo checkout buttons to simulate a payment. In SePay Test mode, use the dashboard simulator with the amount and reference shown in checkout. Live checkout generates a bank transfer QR from the configured receiving account, exact total and payment reference.

## Enable without environment files

Run this yourself in PowerShell from the repository root (AI must not run this secret-entry step):

```powershell
./scripts/configure-sepay.ps1 -Mode LIVE -Auth API_KEY -AccountNumber "<receiving-account>" -AccountName "<account-holder>"
```

Use `-Mode TEST` instead for SePay dashboard Test mode. The command asks for the webhook API key with hidden input. Choose API Key authentication in the matching SePay dashboard and use that same key (16+ characters). For HMAC-SHA256 choose `-Auth HMAC` and provide the matching signing secret instead. Neither key is the ngrok authtoken.

Bank and gateway default to VPBank. Supply your receiving account and account holder, and pass -SubAccount if your bank uses a virtual account. QR checkout uses the virtual account when supplied. Webhook validation checks the configured receiving account and virtual account.

The script writes `secrets/sepay.properties`, limits its Windows permissions to the current user and Local System, and restarts the tracked Ticketbox process. The file stays ignored by Git and denied to AI. It is loaded by the optional Spring configuration import. Real values are neither embedded in the JAR nor printed. If settings already exist, it refuses to read/overwrite them unless you explicitly pass `-Force`. `-NoRestart` configures only; `./scripts/restart-local.ps1` restarts separately.

After startup, newly created checkouts use SePay references and automatic webhook settlement. Keep the URL `https://YOUR_NGROK_HOST/api/payments/sepay/webhook` in SePay. Replay the existing failed event instead of sending another transfer. Legacy `TBTEST...` references are recorded as LEGACY_DEMO_REFERENCE for operator review; they never auto-confirm or resurrect expired seats.

A disabled receiver now returns HTTP 503 with a configuration message instead of an ambiguous 404. Invalid webhook authentication returns 401. Successful receipt returns 201.

## Configuration

Do not paste bank credentials or webhook secrets into source code. Supply these environment variables to Spring Boot:

| Variable | Value |
| --- | --- |
| SEPAY_ENABLED | true to enable; false by default |
| SEPAY_MODE | TEST (default) or LIVE |
| SEPAY_AUTH | HMAC (default) or API_KEY |
| SEPAY_WEBHOOK_SECRET | HMAC secret, or the webhook API key; at least 16 characters |
| SEPAY_BANK | QR bank short code accepted by vietqr.app (e.g. MBBank) |
| SEPAY_GATEWAY | Exact SePay payload gateway name, compared case-insensitively |
| SEPAY_ACCOUNT_NUMBER | AccountNumber reported by SePay for the receiving bank account |
| SEPAY_ACCOUNT_NAME | Receiving account name shown to the customer |
| SEPAY_SUB_ACCOUNT | Optional VA account for the QR; when configured, webhook subAccount must match |

TEST and LIVE must use different webhook secrets and the matching SePay dashboard environment. Mode is a server configuration, not a flag customers may send. The webhook payload itself does not identify the environment. A valid test webhook must never reach a live deployment with the same secret.

Enabling SePay without complete settings fails startup rather than falling back to fake settlement. SePay checkout never uses the old fixed VietQR sample account. It creates a QR URL with the configured account, exact booking amount and a random persistent payment reference. TEST QR codes are labeled test-only: simulate transactions in SePay; do not scan and transfer real money, especially if you copied a real bank account into Test mode.

For Docker Compose, set values in `.env` (see `.env.example`) and restart the api service. For native PowerShell, set the variables in the process environment before `./scripts/run-local.ps1` or launching the JAR; `.env` is not automatically loaded by Spring Boot or that script.

## SePay dashboard setup

1. Use SePay **Test mode** first and create a test receiving account. Configure its bank/gateway/account details above.
2. Create an incoming-money webhook in the same mode, using JSON format.
3. Set the URL to `https://YOUR_PUBLIC_HOST/api/payments/sepay/webhook`. SePay cannot call localhost directly. Use a deployed HTTPS endpoint or an explicitly configured local tunnel. The code does not expose your computer automatically.
4. Select HMAC-SHA256 authentication and use the same secret as SEPAY_WEBHOOK_SECRET. Alternatively select API Key and set SEPAY_AUTH=API_KEY; SePay sends `Authorization: Apikey <secret>`. A webhook API key is distinct from a SePay user API bearer token.
5. Set the payment-code prefix to TB. Ticketbox uses references matching `TB[A-F0-9]{20}`. If SePay's code parser cannot be configured for that format, leave the payment-code filter off; the handler also extracts the exact reference from the raw transfer content. Never filter out relevant transfers because the dashboard expects a numeric-only suffix.
6. Enable retry on non-2xx errors. Accepted requests return HTTP 201 with `{"success":true}`.
7. In customer checkout, reserve seats and generate a QR. In SePay Test mode, simulate an incoming transaction to the configured account with the **exact** displayed amount and transfer reference. This is a provider configuration step; no automated tests or real transfers are run by this implementation.

## ngrok for the local webhook

A dedicated helper exposes only `POST /api/payments/sepay/webhook`; the public tunnel returns 404 for other paths/methods. The Traffic Policy is `scripts/ngrok-sepay-policy.yml`. Local request inspection is disabled so bank payloads and authentication headers are not retained by the local inspector. The backend still verifies the SePay webhook signature/key.

1. Start Ticketbox normally.
2. Install ngrok (the project-local client, if present, is `.tools/ngrok/ngrok.exe`). Authenticate it yourself with your ngrok account using `ngrok config add-authtoken <YOUR_TOKEN>` or the equivalent project-local executable. Do not paste the token into chat or commit it. The helper does not read or print the ngrok credential configuration.
3. Run `./scripts/start-sepay-webhook.ps1`. It prints the HTTPS URL including `/api/payments/sepay/webhook`. Add `-Port 8080` if needed. It refuses to run if the local ngrok API port 4040 is occupied or the route policy is missing. It never falls back to exposing the entire app when the policy fails.
4. In the matching SePay Test/Live dashboard, create an incoming-money JSON webhook at that URL. Configure HMAC/API-key authentication with the same locally configured server secret. Enable retry for non-2xx errors.
5. SePay sends the transaction through ngrok. Spring Boot matches the reference/account/amount against the database and confirms a valid pending hold atomically. The customer page polls payment status. Receipt outcomes are listed under Admin → Payments → Webhook activity.
6. Stop the tunnel with `./scripts/stop-sepay-webhook.ps1`. It verifies the saved process identity before stopping it. Only non-secret PID/start-time/path/public-URL metadata is saved in `.local/ngrok-sepay.json`.

The tunnel does not configure the SePay account or activate the backend. With only the uploaded static QR preview, SePay is still disabled and the webhook returns 404. Configure the authenticated SePay integration above to enable it; do not remove webhook authentication to make a test pass. Update the dashboard URL if ngrok assigns a different URL on a subsequent launch. Free/account plan limits and Traffic Policy availability are controlled by ngrok.

Official tunnel docs: https://ngrok.com/docs/gateway/traffic-policy/getting-started/agent-endpoints

## Payment flow

- GET `/api/payments/config`: authenticated provider/mode availability; no secrets.
- GET `/api/payments/{id}/sepay-checkout`: ownership/Manage payments check; returns exact amount, persistent reference, recipient, expiry and QR URL.
- GET `/api/payments/{id}`: customer polls every three seconds for status; it cannot confirm a real payment.
- POST `/api/payments/sepay/webhook`: SePay authentication, raw JSON payload verification and processing.
- GET `/api/payments/sepay/events`: View payments permission; latest 100 webhook outcomes, also visible under Admin → Payments → Webhook activity.

HMAC verifies `sha256=HMAC_SHA256(secret, timestamp + "." + raw_body)` with `X-SePay-Timestamp` and `X-SePay-Signature`, a five-minute timestamp tolerance, and constant-time comparison. API-key mode also uses constant-time comparison. Unauthenticated requests never enter the settlement service.

The handler accepts incoming transfers only, matches gateway, receiving account (and configured VA), exact amount, unique payment reference, provider environment, pending booking/payment state and a valid hold. It locks booking, then payment, then seats, using the existing settlement transaction. Confirmed event receipt, payment, booking and seats commit together. A unique database key on mode + SePay transaction ID prevents duplicate settlement, including concurrent deliveries.

Authenticated but unmatched, wrong-account, incorrect-amount, ambiguous-reference, cancelled/already-paid and late transfers are saved with an outcome and acknowledged; they do not auto-confirm a booking. Inspect Webhook activity for reconciliation. Late transfers never reclaim seats or resurrect expired bookings. Refunds and manual reconciliation remain a cinema responsibility; no automatic refund is implemented. Transient database/locking errors roll back and return an error for retry.

Test transactions use `SEPAY-TEST-<id>` references and are excluded from verified revenue. Live ones use `SEPAY-LIVE-<id>`. The receipt list stores minimal transaction metadata rather than the full bank payload.

The app adds nullable checkout_provider and unique sepay_reference fields to payments and a sepay_events table through the existing Hibernate update setup. Back up the database before deploying. For deployments using schema validation instead of update, apply equivalent additive schema changes first.

## Official references

- https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook
- https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc
- https://developer.sepay.vn/vi/sepay-webhooks/tao-qr-va-form-thanh-toan
- https://developer.sepay.vn/vi/sepay-webhooks/test-mode/bat-dau-nhanh

No live SePay account, secret, public webhook URL or bank transfer has been configured or verified by this code change. Automated tests remain skipped at the user's request.
