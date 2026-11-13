# Authentication and URL Ownership

> Canonical feature document for Auth Server integration, browser sessions, bearer
> tokens, per-subject URL ownership, URL administration, and service-client access.

## Purpose and Trust Boundary

The sibling `auth-server` is the only OAuth 2.0 Authorization Server and OpenID Connect
Provider. The URL Shortener never handles Google credentials or invited-user passwords.
Its Spring Boot backend is both:

- a confidential OAuth client/BFF for the React browser, using Authorization Code with
  PKCE and keeping access/refresh tokens server-side; and
- a JWT resource server for automation clients using bearer access tokens.

The integration is aligned with auth-server `dev` commit `c1f826a`, which supports Google
login, development-only local administrator login, and administrator-invited production
password accounts. Provider choice stays in the auth-server UI.

## Local Topology and Client Registration

| Component | Local address |
| :--- | :--- |
| Auth Server backend/issuer | `http://localhost:9000` |
| Auth Server login/admin UI | `http://localhost:5173` |
| URL Shortener backend | `http://localhost:8080` |
| URL Shortener Vite UI | `http://localhost:5174` |

The Auth Server client must be confidential and use:

- client ID `url-shortener`;
- callback `http://localhost:8080/login/oauth2/code/auth-server`;
- post-logout URI `http://localhost:5174/`;
- audience `url-shortener-api`;
- scopes `openid profile email urls.read urls.write`; and
- the same strong `URL_SHORTENER_CLIENT_SECRET` configured in both repositories.

Bootstrap configuration does not rewrite an already persisted Auth Server client. Use
its administration UI to update metadata or rotate the secret when the client exists.

## Browser Login and Session

The SPA links to `/oauth2/authorization/auth-server`. Spring Security creates the PKCE
request and completes the callback. The browser receives only the HttpOnly application
session cookie. `GET /api/auth/session` returns display identity, audience-scoped roles,
administrator status, and the CSRF header/token used by Axios mutations; OAuth tokens
are never returned to JavaScript or browser storage.

The URL management endpoints require authentication. Public `GET /{shortCode}` redirects
remain anonymous. Browser mutations require CSRF; requests carrying a bearer token are
stateless and exempt from browser CSRF checks.

## Token and Authorization Contract

JWT validation requires the exact issuer, signature, expiry, and audience
`url-shortener-api`. Application authorization additionally enforces:

- `urls.read` for listings, ID lookup, and statistics;
- `urls.write` for creation and soft deletion;
- `subject_type=user` plus a valid immutable UUID `uid` for people;
- `subject_type=client` plus `client_id` for machine identities; and
- no human roles on client-credentials tokens.

The audience-specific `URL_ADMIN` role grants cross-owner management. The unrelated
`AUTH_ADMIN` role controls the Auth Server and has no URL Shortener privilege.

## Ownership and CQRS

New rows populate exactly one ownership column:

| Actor | Stored owner | Visibility |
| :--- | :--- | :--- |
| Human user | `owner_uid=uid` | Own active, non-expired URLs |
| Service client | `owner_client_id=client_id` | Its own active, non-expired URLs |
| `URL_ADMIN` user | Own UID on creation | All URLs for management |

`UrlController` resolves the authenticated actor and places it in command/query messages.
`UrlCommandService` enforces write scope and ownership for create/delete operations;
`UrlQueryService` enforces read scope and visibility. `UrlAccessPolicy` centralizes the
owner-or-admin decision. Redirect queries and atomic click commands remain public and do
not expose management data.

Legacy rows with neither owner column are admin-only. Liquibase changeset
`002-url-ownership.sql` adds both nullable columns and their indexes without assigning
existing rows to an arbitrary identity.

## Service Clients

The optional `url-shortener-automation` Auth Server client uses Client Credentials and
requests `urls.read` and/or `urls.write`. Its links are isolated by client ID. Machine
tokens never receive a user UID or human roles and cannot impersonate users.

## Docker Networking

For the containerized URL Shortener, the browser-facing authorization URI and token
issuer remain `http://localhost:9000`. Backend token, UserInfo, and JWKS calls use
`host.docker.internal:9000`; Compose supplies the host-gateway mapping. The Auth Server
is started from its own repository and shares only the local PostgreSQL container—not
the application database or Liquibase history.

## Validation

Automated coverage verifies token claim parsing, user/client separation, scope gates,
owner filtering, administrator visibility, cross-owner rejection, command-side owner
persistence, public redirect behavior, and the PostgreSQL ownership migration.

Manual end-to-end checks should cover:

1. Login through Google and an enabled local/invited account.
2. User A cannot list, inspect, or delete User B's URLs.
3. A `URL_ADMIN` token can search and manage cross-owner URLs.
4. A service token sees only its client-owned rows and a read-only token cannot mutate.
5. Browser mutation without the session CSRF header returns `403`.
6. Anonymous short-code redirects still return `302` and increment clicks.

## Troubleshooting

- `invalid_client`: rotate/update the persisted Auth Server client secret.
- Redirect mismatch: register the exact backend callback and Vite post-logout URI.
- Issuer failure: keep token `iss` as `http://localhost:9000`; only internal endpoint
  addresses use `host.docker.internal`.
- Role changes require a new access token; sign out and sign in again.
- Browser mutation `403`: load `/api/auth/session` and send its returned CSRF header.

## Primary Implementation Files

- `backend/src/main/java/com/urlshortener/config/SecurityConfig.java`
- `backend/src/main/java/com/urlshortener/controller/AuthController.java`
- `backend/src/main/java/com/urlshortener/controller/UrlController.java`
- `backend/src/main/java/com/urlshortener/security/ActorContext.java`
- `backend/src/main/java/com/urlshortener/security/CurrentActorResolver.java`
- `backend/src/main/java/com/urlshortener/security/UrlAccessPolicy.java`
- `backend/src/main/java/com/urlshortener/application/command/UrlCommandService.java`
- `backend/src/main/java/com/urlshortener/application/query/UrlQueryService.java`
- `backend/src/main/resources/db/changelog/changes/002-url-ownership.sql`
- `frontend/src/App.tsx`
- `frontend/src/api/urlApi.ts`
- `frontend/src/components/Header.tsx`
- `docker-compose.yml`
