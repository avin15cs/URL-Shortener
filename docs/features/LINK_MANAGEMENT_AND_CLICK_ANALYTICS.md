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
public collection excludes soft-deleted and expired mappings; direct ID lookup exposes
the stored record and is not currently an authenticated ownership boundary.

## Click Accounting

Every successful redirect issues a repository-level update:

```sql
UPDATE urls SET click_count = click_count + 1 WHERE id = ?
```

The update is atomic at the database level, avoiding lost increments from concurrent
read-modify-write operations. Failed redirects do not increment the counter. The
current analytics capability is aggregate-only; unique visitors, referrers, geography,
devices, and event history are roadmap work rather than implemented behavior.

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
`clickCount`, timestamps, optional expiry, and `isActive`. The `updatedAt` timestamp is
maintained by JPA lifecycle callbacks.

## Ownership Boundaries

Update this document for link collection behavior, per-ID retrieval, soft deletion,
dashboard cards, refresh/copy actions, aggregate click counts, or statistics responses.
Creation and redirect-code behavior belong in `URL_SHORTENING_AND_REDIRECTION.md`.

## Primary Implementation Files

- `backend/src/main/java/com/urlshortener/controller/UrlController.java`
- `backend/src/main/java/com/urlshortener/service/UrlService.java`
- `backend/src/main/java/com/urlshortener/repository/UrlRepository.java`
- `backend/src/main/java/com/urlshortener/model/Url.java`
- `frontend/src/App.tsx`
- `frontend/src/components/UrlList.tsx`
- `frontend/src/components/UrlCard.tsx`
- `frontend/src/hooks/useUrl.ts`
- `frontend/src/api/urlApi.ts`
