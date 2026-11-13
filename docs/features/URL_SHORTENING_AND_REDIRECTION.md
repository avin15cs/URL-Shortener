# URL Shortening and Redirection

> Canonical feature document for creating short links, generating public codes,
> validating custom aliases, and resolving redirects.

## Purpose and User Flow

A user enters an HTTP or HTTPS destination in the React form, optionally supplies a
title and custom alias, and submits it to `POST /api/urls/shorten`. The backend stores
the mapping and returns a public short URL. Opening that URL resolves the code, records
the click, and responds with `302 Found` to the original destination.

## API Contract

| Method | Path | Behavior |
| :--- | :--- | :--- |
| `POST` | `/api/urls/shorten` | Validate and create a short-link mapping |
| `GET` | `/{shortCode}` | Resolve an active, unexpired code and redirect |

Creation accepts `originalUrl`, optional `customAlias`, optional `title`, and an
optional ISO date-time `expiresAt`. The current frontend exposes the URL, alias, and
title fields; expiry remains available through the backend contract but is not yet a
complete frontend feature.

## Code Generation and Namespace

Automatic links use the persisted numeric database ID as Sqids input. The shared
`Sqids` bean enforces a minimum output length of seven and extends the standard
blocklist with application-reserved routes. Because the ID is assigned after the first
database flush, automatic records briefly receive a transaction-local pending value
that is replaced before the transaction completes.

Generated codes and custom aliases occupy one public namespace:

- Custom aliases must contain 3–50 letters, numbers, hyphens, or underscores.
- Blank aliases normalize to `null` and use automatic generation.
- Aliases are rejected if either `shortCode` or legacy `customAlias` already owns them.
- Reserved application paths such as `api`, `actuator`, `swagger-ui`, and `v3` cannot
  be claimed.
- The database unique constraint on `short_code` is the final collision guarantee.
- Automatic generation retries with a discriminator up to ten times if a collision is
  detected.

Sqids obscure sequential IDs but are not encryption, secrets, or authorization tokens.

## Redirect Lifecycle

1. Nginx proxies single-segment public codes to the backend in the containerized stack;
   native short URLs already use the backend base URL.
2. `RedirectController` rejects configured reserved routes.
3. `UrlService` looks up the canonical `shortCode`, with `customAlias` retained only as
   a compatibility fallback for older rows.
4. Missing, inactive, or expired mappings return not-found behavior.
5. The repository increments `clickCount` atomically.
6. The backend returns `302 Found` with the original URL in the `Location` header.

## Frontend Experience

`ShortenForm` provides URL validation, optional advanced alias/title inputs, loading
state, toast feedback, the resulting short link, and clipboard copy. Successful
creation inserts the new item into the displayed link collection without requiring a
full reload.

## Configuration

- `app.base-url` controls the public URL returned to clients.
- `app.reserved-paths` owns protected single-segment routes.
- `app.cors.allowed-origins` permits the native Vite origins in development and is
  overridden for production.

## Failure Behavior

- Invalid request fields produce validation errors.
- Duplicate aliases produce `409 Conflict`.
- Missing, inactive, expired, or reserved public codes do not redirect.
- Unexpected backend failures use the common API error response from
  `GlobalExceptionHandler`.

## Ownership Boundaries

Update this document for changes to creation, validation, custom aliases, Sqids,
reserved public paths, expiry enforcement, returned short URLs, or redirect behavior.
Link listing, deletion, and aggregate analytics belong in
`LINK_MANAGEMENT_AND_CLICK_ANALYTICS.md`.

## Primary Implementation Files

- `backend/src/main/java/com/urlshortener/controller/UrlController.java`
- `backend/src/main/java/com/urlshortener/controller/RedirectController.java`
- `backend/src/main/java/com/urlshortener/service/UrlService.java`
- `backend/src/main/java/com/urlshortener/config/SqidsConfig.java`
- `backend/src/main/java/com/urlshortener/config/AppProperties.java`
- `backend/src/main/java/com/urlshortener/model/Url.java`
- `frontend/src/components/ShortenForm.tsx`
- `frontend/src/hooks/useUrl.ts`
- `frontend/src/api/urlApi.ts`
