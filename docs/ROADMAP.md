# 🗺️ URL Shortener Roadmap

> **Auto-sync enabled:** Tasks in this file are automatically synced to GitHub Issues
> when pushed to the `dev` branch. Use `- [ ]` for planned items, `- [x]` for done.
>
> **Format per phase header:** `## Phase Name [label:enhancement] [milestone:v1.1.0]`
> **Format per task:** `- [ ] Short title — optional longer description after em-dash`

---

## Phase 1 — Core Link Features [label:enhancement] [milestone:v1.1.0]

- [ ] Custom link expiry dates — allow users to set a TTL (e.g. 24h, 7d, never) per shortened URL
- [ ] Password-protected short links — require a passphrase before redirecting
- [ ] Bulk URL shortening API — accept up to 100 URLs in a single POST request and return all short codes
- [ ] Link preview page — show destination URL with a "Continue" button before redirecting (opt-in)
- [ ] QR code download — allow users to download the QR code as PNG or SVG from the UI

---

## Phase 2 — Analytics Dashboard [label:feature] [milestone:v1.2.0]

- [ ] Click analytics per link — track total clicks, unique visitors, and referrer breakdown
- [ ] Geographic breakdown — show top countries and cities for each short link
- [ ] Device and browser breakdown — desktop vs mobile vs tablet, browser distribution
- [ ] Analytics CSV export — download raw click data per link as a CSV file
- [ ] Real-time click counter — push live click count updates to the UI via WebSocket or SSE

---

## Phase 3 — Infrastructure & Performance [label:devops] [milestone:v2.0.0]

- [ ] Redis cache layer — cache hot short codes in Redis to reduce DB load on redirects
- [ ] Rate limiting — enforce per-IP and per-API-key rate limits to prevent abuse
- [ ] Kubernetes deployment manifests — provide Helm chart or raw k8s manifests for production
- [ ] CDN-friendly redirect headers — add proper Cache-Control and Vary headers for edge caching
- [ ] Horizontal scaling guide — document stateless backend scaling with shared PostgreSQL and Redis

---

## Phase 4 — Developer Experience [label:documentation] [milestone:v2.1.0]

- [ ] Public REST API documentation site — host a Redoc or Swagger UI on a dedicated docs URL
- [ ] API key management UI — allow users to generate and revoke API keys from the frontend
- [ ] Webhook support — fire a POST to a user-configured URL on each click event
- [ ] CLI tool — a simple `urlshort` CLI to shorten URLs from the terminal using the API

---

## Completed ✅

- [x] Sqids-based short code generation with 7-character minimum
- [x] Custom alias support with reserved path validation
- [x] Atomic click counter on redirects
- [x] Docker Compose stack with PostgreSQL 15
- [x] CI/CD with GitHub Actions (ci.yml + cd.yml)
- [x] SpringDoc OpenAPI 3.1.1 at /swagger-ui.html
- [x] Branch rulesets protecting dev and main
