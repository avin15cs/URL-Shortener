# Auth-Server Integration Test Guide

This guide verifies interactive login, JWT resource-server behavior, URL ownership,
`URL_ADMIN` access, service-client isolation, CSRF protection, and the production schema
migration. Run these checks with disposable local databases.

## 1. Prerequisites

- Java 21, Node.js 22.12+, Maven, Docker, and Docker Compose.
- This repository and the sibling `auth-server` repository checked out under the same
  GitHub parent directory.
- A Google OAuth Web client configured as described by `auth-server`.
- A private development JWK Set generated through the auth-server helper.
- Two Google accounts are helpful for verifying user isolation.

Use one strong, temporary value for `URL_SHORTENER_CLIENT_SECRET` in both applications.
Do not commit it.

## 2. Start the auth-server

In a PowerShell terminal opened in the sibling `auth-server` repository, configure the
required values from its developer guide. The relevant URL Shortener settings are:

```powershell
$env:URL_SHORTENER_CLIENT_ID = "url-shortener"
$env:URL_SHORTENER_CLIENT_SECRET = "replace-with-one-strong-shared-secret"
$env:URL_SHORTENER_REDIRECT_URI = "http://localhost:8080/login/oauth2/code/auth-server"
$env:URL_SHORTENER_POST_LOGOUT_URI = "http://localhost:5173/"
$env:URL_SHORTENER_AUDIENCE = "url-shortener-api"
$env:URL_SHORTENER_AUTOMATION_ENABLED = "true"
$env:URL_SHORTENER_AUTOMATION_CLIENT_ID = "url-shortener-automation"
$env:URL_SHORTENER_AUTOMATION_CLIENT_SECRET = "replace-with-a-different-strong-secret"
```

Also set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `AUTH_JWK_SET`, and
`AUTH_ADMIN_EMAILS`. The Google callback is:

```text
http://localhost:9000/login/oauth2/code/google
```

Start the auth-server database and application:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Verify discovery and health:

```powershell
Invoke-RestMethod http://localhost:9000/.well-known/openid-configuration
Invoke-RestMethod http://localhost:9000/actuator/health
```

If the client was previously bootstrapped with another secret, use the auth-server admin
UI to rotate/update it or recreate only its disposable local database. Changing the
environment variable does not rewrite an already persisted client secret.

## 3. Start the URL Shortener locally

Open a second PowerShell terminal:

```powershell
cd backend
$env:URL_SHORTENER_CLIENT_SECRET = "replace-with-one-strong-shared-secret"
mvn spring-boot:run
```

Open a third terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The signed-out page must show **Sign in with Google** and
must not show the URL form or URL list.

Useful unauthenticated checks:

```powershell
Invoke-RestMethod http://localhost:8080/api/auth/session
curl.exe -i http://localhost:8080/api/urls
```

Expected results:

- `/api/auth/session` returns `authenticated: false` and CSRF metadata.
- `/api/urls` returns `401 Unauthorized` without a session or bearer token.
- A public short-code redirect remains accessible without authentication.

## 4. Verify normal-user login and ownership

1. Select **Sign in with Google** and complete login.
2. Confirm the browser returns to `http://localhost:5173` and displays the signed-in
   identity.
3. Create two short URLs, including one custom alias.
4. Refresh the page and confirm both URLs remain visible.
5. Open each short URL in a private window and confirm the public redirect works and the
   click count increases.
6. Sign out. Confirm the management UI disappears.
7. Sign in with a second Google account. Confirm the first account's URLs are absent.
8. If you retained a URL database ID from the first account, request
   `/api/urls/{id}` while signed in as the second account. It must return `403 Forbidden`.

New human-owned rows must have `owner_uid` populated and `owner_client_id` empty.

## 5. Verify `URL_ADMIN` role-specific fetching

1. Sign in to the auth-server with an email listed in `AUTH_ADMIN_EMAILS`.
2. Open its admin UI at `http://localhost:9000/users`.
3. Find the intended URL Shortener administrator.
4. Assign role `URL_ADMIN` for audience `url-shortener-api`. Do not assign it under the
   `auth-server` audience.
5. Sign out of the URL Shortener and sign in again so it obtains a new access token.
6. Confirm the header displays **URL Admin**.
7. Confirm the list is titled **All Links** and contains links belonging to both test
   users.
8. Search by title, original URL, alias, and short code.
9. Confirm the administrator can view statistics and delete a link owned by another user.
10. Remove `URL_ADMIN` in the auth-server, sign in again, and confirm the account returns
    to its own-user view.

Legacy rows with neither ownership column populated must appear only to `URL_ADMIN`.

## 6. Verify service-client behavior

Request a read/write client-credentials token. In PowerShell:

```powershell
$pair = "url-shortener-automation:replace-with-a-different-strong-secret"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:9000/oauth2/token `
  -Headers @{ Authorization = "Basic $basic" } `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=client_credentials&scope=urls.read%20urls.write"
$token = $tokenResponse.access_token
```

Create and list machine-owned URLs:

```powershell
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/urls/shorten `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"originalUrl":"https://example.com/service-client-test"}'
Invoke-RestMethod -Uri http://localhost:8080/api/urls -Headers $headers
```

Expected results:

- The service client sees only rows owned by `url-shortener-automation`.
- It does not see human-owned rows and cannot use human `URL_ADMIN` privileges.
- Machine-owned rows have `owner_client_id=url-shortener-automation` and no `owner_uid`.
- Bearer-token mutations do not require the browser CSRF token.

Request a read-only token and confirm a mutation is rejected:

```powershell
$readOnlyResponse = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:9000/oauth2/token `
  -Headers @{ Authorization = "Basic $basic" } `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=client_credentials&scope=urls.read"
$readOnlyHeaders = @{ Authorization = "Bearer $($readOnlyResponse.access_token)" }
```

Use `$readOnlyHeaders` with `POST /api/urls/shorten`; it must return `403 Forbidden`
because `urls.write` is missing.

## 7. Verify browser CSRF protection

After browser login, inspect `GET /api/auth/session` in developer tools. It returns the
CSRF header name and token used by the Axios interceptor.

From a separate client, replay a session-authenticated `POST` or `DELETE` without that
header. It must return `403 Forbidden`. The same request with the exact CSRF header/token
must succeed. Never place the CSRF token or OAuth tokens in local or session storage.

## 8. Inspect ownership in PostgreSQL

For the URL Shortener Docker database:

```powershell
docker exec -it urlshortener-db psql -U postgres -d urlshortener
```

Then run:

```sql
SELECT id, short_code, owner_uid, owner_client_id, is_active
FROM urls
ORDER BY id;
```

Exactly one owner column should be populated for every newly created URL. Rows created
before this feature may have both columns empty and are intentionally admin-only.

## 9. Test the Docker URL Shortener stack

Keep the auth-server reachable on host port `9000`. In this repository:

```powershell
Copy-Item .env.example .env
# Edit .env so URL_SHORTENER_CLIENT_SECRET matches the auth-server.
docker compose up --build
docker compose ps
```

Open `http://localhost`. Repeat one interactive login, URL creation, logout, and login.
The backend container uses `host.docker.internal` for token, UserInfo, and JWKS calls but
continues to validate the public issuer `http://localhost:9000`.

## 10. Automated verification

Run before reporting a defect:

```powershell
cd backend
mvn test
cd ..\frontend
npm run lint
npm run build
cd ..
docker compose config --quiet
```

Expected baseline: 16 backend tests pass, frontend lint/build succeed, and Compose config
validation exits successfully.

## 11. Troubleshooting

- **`invalid_client`**: the persisted auth-server client secret and URL Shortener secret
  differ. Rotate the client or recreate the disposable auth database.
- **`redirect_uri` mismatch**: register exactly
  `http://localhost:8080/login/oauth2/code/auth-server`.
- **Issuer validation failure**: `AUTH_ISSUER` must exactly match the token's `iss` claim.
  Do not change it to `host.docker.internal`; use the separate internal endpoint settings.
- **JWKS/token connection failure in Docker**: ensure auth-server listens on host port
  `9000` and Docker supports the included host-gateway mapping.
- **Role change not visible**: sign out and sign in again to issue a token with the new
  audience-scoped role.
- **`403` on browser mutation**: load `/api/auth/session` first and send its CSRF header.
- **Production schema validation failure**: confirm the `prod` profile enables Liquibase
  and the database user can create `DATABASECHANGELOG` tables and alter `urls`.

## 12. Cleanup

Stop the URL Shortener stack without deleting data:

```powershell
docker compose down
```

Use `docker compose down -v` only when you intentionally want to delete the disposable
local database. Stop the auth-server stack separately from its repository.
