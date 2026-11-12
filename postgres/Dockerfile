# Rebuild gosu with a patched Go toolchain while preserving its upstream 1.19
# source exactly. The official PostgreSQL entrypoint uses gosu only to drop
# privileges before starting the database server.
FROM golang:1.27.4-alpine3.24 AS gosu-builder

ARG GOSU_COMMIT=6456aaa0f3c854d199d0f037f068eb97515b7513
RUN apk add --no-cache git \
    && git clone https://github.com/tianon/gosu.git /src/gosu \
    && cd /src/gosu \
    && git checkout --detach "${GOSU_COMMIT}" \
    && CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/gosu .

FROM postgres:15.19-alpine3.24

# Apply security fixes published for the pinned Alpine 3.24 package line.
RUN apk upgrade --no-cache
COPY --from=gosu-builder /out/gosu /usr/local/bin/gosu
