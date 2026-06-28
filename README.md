# redis-grpc-client

A Quarkus extension that provides a **client** for the [`redis-grpc`](../quarkus-redis-grpc)
gateway — the north-south gRPC-over-Redis proxy. It lets a Quarkus application
consume that gateway ergonomically: inject a ready-to-use client, configure the
endpoint and credentials declaratively, and call the Redis command families
(KEY/VALUE, HASH, SET, KEY-general) as typed gRPC RPCs.

## Status

Early stage — **skeleton only**. The multi-module extension structure
(`runtime` + `deployment`) builds and the extension loads, but it carries no
client logic yet. The `.proto` contract is owned and versioned by the adjacent
`redis-grpc` proxy; this extension consumes it. Architecture and open decisions
are captured in [docs/DESIGN.md](docs/DESIGN.md).

## Modules

| Module | Artifact | Role |
|---|---|---|
| `runtime/` | `io.github.claudineyns:redis-grpc-client` | The dependency a consuming app adds; runtime code + extension descriptor. |
| `deployment/` | `io.github.claudineyns:redis-grpc-client-deployment` | Build-time augmentation (`@BuildStep`). |

## Build

```bash
mvn -B -ntp clean install
```

Built with **community Quarkus, 3.15 LTS** floor (tested on `3.15.7`) and
**Java 21**.

### Consumer compatibility

- **Minimum Quarkus: 3.15** (the LTS floor). Use any `3.x >= 3.15`.
- A consuming Quarkus app's own platform BOM governs the versions of the shared
  artifacts this extension brings transitively (Maven `dependencyManagement`
  overrides transitive versions). Red Hat or community consumers both work; the
  pinned version here is the build/test baseline, not an imposition.
- The build-time code generator (`quarkus-grpc-codegen`) is `optional` and never
  propagates to consumers.

## Metrics

Optional, opt-in via **Micrometer**: when the consumer has the Micrometer metrics
system active, the extension records a `Timer` `redis.grpc.client.call` per RPC,
tagged `service` / `method` / `status` (the Redis key is never a tag). Toggle with
`quarkus.redis-grpc-client.metrics.enabled` (default `true`).

Micrometer is a vendor-neutral facade, so the **export backend is the consumer's
choice** — add the matching registry: `quarkus-micrometer-registry-prometheus` for
Prometheus, `micrometer-registry-otlp` for **OpenTelemetry/OTLP**, etc. No
extension change is needed to switch backends.
