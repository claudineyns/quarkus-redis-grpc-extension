# contract/ — reference `.proto` for the redis-grpc gateway

This folder holds a **trusted, versioned reference copy** of the gRPC contract that
this extension targets. It exists so the implementation has a stable, in-repo
reference to design against — independent of `temp/` (whose contents must never be
assumed valid).

## Provenance

- **Obtained via gRPC Server Reflection** from the live proxy running in CRC
  (`redis-grpc.apps-crc.testing`, namespace `redis-grpc`), reconstructed from the
  server's `FileDescriptor`s with `protoprint`.
- **Extraction date:** 2026-06-28.
- **Server contract at extraction:** proxy `redis-grpc` v0.4.0 (repo HEAD
  `85ad622`).
- Only the **redis contract** package is vendored here
  (`io.github.claudineyns.redis.grpc.v1`): `string`, `hash`, `set`, `key`. The
  standard `grpc.health.v1.Health` service is also exposed by the server but is a
  well-known service (provided by gRPC libraries), so it is intentionally **not**
  vendored.

## Fidelity (validated)

Each file was validated **semantically identical** to the proxy's authoritative
source (`quarkus-redis-grpc/src/main/proto/**`, the source of truth) — comparing
messages, fields, types, enums, `oneof`s and services, ignoring comments,
declaration order and formatting. All four families matched.

> The reflection reconstruction is **wire-faithful** but not byte-identical to the
> original source: didactic comments and the original element ordering are not
> recoverable from descriptors. For the **client** these carry no semantic weight.

## Source of truth

The **proxy repository remains the single source of truth** for the contract
(`io.github.claudineyns:redis-grpc`, `src/main/proto/`, package `...v1`). This copy
is a **reference snapshot**, not the canonical definition. If the proxy contract
changes, re-extract and re-validate.

## Build input (kept in sync)

The extension generates its Mutiny gRPC stubs from an **identical copy** under
[../runtime/src/main/proto/](../runtime/src/main/proto/) (the layout `quarkus-grpc`
expects). That copy is the build input; this `contract/` folder is the
human-facing reference + provenance. **Keep both in sync:** on any re-extraction,
write to both (or copy `contract/proto/**` → `runtime/src/main/proto/**`).

## Status / open decision

This folder is a **reference**. The decision of *how* the contract feeds the build
was settled for now as a **vendored copy** (above); the broader sharing mechanism
(published artifact vs. direct reference) remains open in
[../docs/DESIGN.md](../docs/DESIGN.md) §7.

## Re-extraction (reproducibility)

Re-derive credentials authoritatively (never from `temp/`): read the HMAC
master-key from the cluster Secret `redis-grpc-auth`, take the lab `ACCESS_KEY`
from the proxy's committed `infra/ocp/27-credentials.sh`, recompute
`SECRET_KEY = HMAC-SHA256(masterKey, ACCESS_KEY)`, and confirm
`SHA-256(ACCESS_KEY)` is in the live `redis-grpc-acl` ConfigMap allowlist. Then run
the Go reflection extractor with `OUT_DIR` pointing here. Reflection is auth-gated,
so the credential pair (headers `x-grpc-access-key` / `x-grpc-secret-key`) is
required.
