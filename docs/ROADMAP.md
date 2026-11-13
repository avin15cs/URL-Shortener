# URL Shortener Roadmap

> Tasks in this file are synchronized to GitHub Issues after changes reach `dev`.
> Labels and milestones come from each phase header. Every task requires a stable,
> unique `roadmap-id`; do not change it after the issue has been created.
>
> Phase: `## Name [label:enhancement] [milestone:v1.1.0]`
> Task: `- [ ] Title -- optional description <!-- roadmap-id: stable-id -->`
>
> Checking a managed task closes its issue. Reopening it in this file reopens the
> issue. Existing issues without the generated ownership marker are never modified.
> See `docs/DEVELOPER_GUIDE.md` for optional GitHub Project setup.

---

## Phase 0 -- Identity, Ownership, and Administration [label:security] [milestone:v1.1.0]

- [x] Integrate the backend as an OAuth 2.0 resource server -- validate JWT issuer, audience, scopes, user subjects, and service identities from the standalone auth server <!-- roadmap-id: oauth2-resource-server -->
- [x] Enforce per-user URL ownership -- persist the immutable auth-server `uid` on every managed URL and restrict reads, updates, statistics, and deletion to its owner <!-- roadmap-id: per-user-url-ownership -->
- [x] Add authenticated login and user dashboard -- use Authorization Code with PKCE through the backend and show each signed-in user only their own links <!-- roadmap-id: authenticated-user-dashboard -->
- [x] Add a URL administration dashboard -- allow users with the audience-specific `URL_ADMIN` role to search and manage links across owners <!-- roadmap-id: url-admin-dashboard -->
- [x] Define service-client access rules -- keep client-credentials identities distinct from users and authorize automation through explicit scopes without user impersonation <!-- roadmap-id: service-client-access-rules -->

---

## Phase 1 -- Core Link Features [label:enhancement] [milestone:v1.1.0]

- [ ] Custom link expiry dates -- allow users to set a TTL (for example, 24h, 7d, or never) per shortened URL <!-- roadmap-id: custom-link-expiry -->
- [ ] Password-protected short links -- require a passphrase before redirecting <!-- roadmap-id: password-protected-links -->
- [ ] Bulk URL shortening API -- accept up to 100 URLs in one request and return all short codes <!-- roadmap-id: bulk-url-api -->
- [ ] Link preview page -- show the destination URL with a Continue button before redirecting (opt-in) <!-- roadmap-id: link-preview-page -->
- [ ] QR code download -- allow users to download the QR code as PNG or SVG from the UI <!-- roadmap-id: qr-code-download -->

---

## Phase 2 -- Analytics Dashboard [label:feature] [milestone:v1.2.0]

- [ ] Click analytics per link -- track total clicks, unique visitors, and referrer breakdown <!-- roadmap-id: click-analytics -->
- [ ] Geographic breakdown -- show top countries and cities for each short link <!-- roadmap-id: geographic-analytics -->
- [ ] Device and browser breakdown -- show device and browser distribution <!-- roadmap-id: device-browser-analytics -->
- [ ] Analytics CSV export -- download raw click data per link as a CSV file <!-- roadmap-id: analytics-csv-export -->
- [ ] Real-time click counter -- push live click count updates to the UI via WebSocket or SSE <!-- roadmap-id: realtime-click-counter -->

---

## Phase 3 -- Infrastructure and Performance [label:devops] [milestone:v2.0.0]

- [ ] Redis cache layer -- cache hot short codes in Redis to reduce database load on redirects <!-- roadmap-id: redis-cache -->
- [ ] Rate limiting -- enforce per-IP and per-API-key limits to prevent abuse <!-- roadmap-id: rate-limiting -->
- [ ] Kubernetes deployment manifests -- provide a Helm chart or raw Kubernetes manifests <!-- roadmap-id: kubernetes-manifests -->
- [ ] CDN-friendly redirect headers -- add appropriate Cache-Control and Vary headers <!-- roadmap-id: cdn-redirect-headers -->
- [ ] Horizontal scaling guide -- document stateless backend scaling with shared PostgreSQL and Redis <!-- roadmap-id: horizontal-scaling-guide -->

---

## Phase 4 -- Developer Experience [label:documentation] [milestone:v2.1.0]

- [ ] Public REST API documentation site -- host Redoc or Swagger UI on a dedicated documentation URL <!-- roadmap-id: public-api-docs-site -->
- [ ] API key management UI -- allow users to generate and revoke API keys from the frontend <!-- roadmap-id: api-key-management-ui -->
- [ ] Webhook support -- send a POST request to a configured URL on each click event <!-- roadmap-id: webhook-support -->
- [ ] CLI tool -- provide a `urlshort` CLI that uses the API <!-- roadmap-id: cli-tool -->

---

## Completed

- [x] Sqids-based short code generation with 7-character minimum <!-- roadmap-id: sqids-short-codes -->
- [x] Custom alias support with reserved path validation <!-- roadmap-id: custom-aliases -->
- [x] Atomic click counter on redirects <!-- roadmap-id: atomic-click-counter -->
- [x] Docker Compose stack with PostgreSQL 15 <!-- roadmap-id: docker-compose-postgres -->
- [x] CI/CD with GitHub Actions <!-- roadmap-id: github-actions-cicd -->
- [x] SpringDoc OpenAPI at `/swagger-ui.html` <!-- roadmap-id: springdoc-openapi -->
- [x] Branch rulesets protecting `dev` and `main` <!-- roadmap-id: branch-rulesets -->
