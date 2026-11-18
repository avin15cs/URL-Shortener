# Link Management and Click Analytics

> Canonical feature document for browsing, inspecting, copying, refreshing, deleting,
> and viewing aggregate click counts for short links.

## Purpose and User Flow

The application loads active, non-expired links into the React dashboard. Each card
shows its short URL, original destination, title when present, creation date, domain,
custom-alias marker, and current aggregate click count. Users can copy or visit a link,
refresh the collection, and soft-delete a mapping.

## API Contract

| Method | Path | Behavior |
| :--- | :--- | :--- |
| `GET` | `/api/urls` | List active, non-expired links newest first |
| `GET` | `/api/urls/{id}` | Return one mapping by database ID |
| `GET` | `/api/urls/{id}/stats` | Return the mapping with its aggregate click count |
| `DELETE` | `/api/urls/{id}` | Soft-delete the mapping by setting `isActive=false` |

Responses use the shared `ApiResponse` envelope and `UrlResponse` projection. The
collection excludes soft-deleted and expired mappings. Normal users and service clients
see only their owned rows; `URL_ADMIN` users see all rows. Direct ID/statistics lookup and
soft deletion enforce the same owner-or-admin boundary.

## Click Accounting

Every successful redirect issues a repository-level update:

```sql
UPDATE urls SET click_count = click_count + 1 WHERE id = ?
```

The update is atomic at the database level, avoiding lost increments from concurrent
read-modify-write operations. Failed redirects do not increment the counter. The
current analytics capability is aggregate-only; unique visitors, referrers, geography,
devices, and event history are roadmap work rather than implemented behavior.

The application layer uses code-level CQRS. Listing, ID lookup, statistics, and public
code resolution are explicit queries handled by `UrlQueryService` in read-only
transactions. Soft deletion and click recording are explicit commands handled by
`UrlCommandService`. Both sides intentionally share the same repository and database;
this feature does not introduce asynchronous events or a separate analytics store.

## Frontend State and Actions

- `useUrlList` owns loading, error, collection refresh, and local removal after delete.
- `UrlList` renders loading skeletons, empty/error states, the link count, and refresh.
- `UrlCard` provides clipboard copy, destination opening, deletion confirmation, and
  per-link metadata.
- The dashboard refreshes on initial application load and inserts newly shortened URLs
  at the top of the in-memory collection.

Deletion is soft at the backend even though the frontend confirmation describes the
user-visible action as irreversible. The row remains stored for data integrity but is
excluded from active listing and redirect resolution.

## Data Used by the Feature

The `Url` entity supplies `id`, `originalUrl`, `shortCode`, `customAlias`, `title`,
`clickCount`, timestamps, optional expiry, `isActive`, `ownerUid`, and `ownerClientId`.
The `updatedAt` timestamp is maintained by JPA lifecycle callbacks.

## Validation

Backend tests cover list projection, soft deletion, atomic click-command delegation,
owner/admin filtering, scope enforcement, cross-owner rejection, and the boundary that
prevents short-code queries from incrementing click counts.

## Ownership Boundaries

Update this document for link collection behavior, per-ID retrieval, soft deletion,
dashboard cards, refresh/copy actions, aggregate click counts, or statistics responses.
Creation and redirect-code behavior belong in `URL_SHORTENING_AND_REDIRECTION.md`.

## Primary Implementation Files

- `backend/src/main/java/com/urlshortener/controller/UrlController.java`
- `backend/src/main/java/com/urlshortener/application/command/UrlCommandService.java`
- `backend/src/main/java/com/urlshortener/application/query/UrlQueryService.java`
- `backend/src/main/java/com/urlshortener/application/mapper/UrlResponseMapper.java`
- `backend/src/main/java/com/urlshortener/repository/UrlRepository.java`
- `backend/src/main/java/com/urlshortener/model/Url.java`
- `frontend/src/App.tsx`
- `frontend/src/components/UrlList.tsx`
- `frontend/src/components/UrlCard.tsx`
- `frontend/src/hooks/useUrl.ts`
- `frontend/src/api/urlApi.ts`
