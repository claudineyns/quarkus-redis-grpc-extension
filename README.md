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
