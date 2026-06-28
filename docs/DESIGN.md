# DESIGN.md — `redis-grpc-client` architecture and design

> Project architecture and technical design document (**English** version).
> Portuguese version: [DESIGN.pt-BR.md](DESIGN.pt-BR.md). **Both files are kept in
> sync** — every design change must be reflected in both.
> The main reference and the agent working guidelines are in
> [../CLAUDE.md](../CLAUDE.md).
> Items marked **[OPEN]** are not yet decided — do not implement assuming a side
> without confirmation.

---

## 1. Purpose and premise

This project is the **client-side Quarkus extension** for the `redis-grpc`
gateway — the north-south gRPC-over-Redis proxy implemented in the adjacent
project `quarkus-redis-grpc`. The extension lets a Quarkus application **consume**
that gateway ergonomically: inject a ready-to-use client, configure the endpoint
and credentials declaratively, and call the Redis command families (KEY/VALUE,
HASH, SET, KEY-general) as typed gRPC RPCs.

**Why a "real" extension (not a plain library/producer).** The proxy's DESIGN
(§3.1) decided that the client side would be a **proper Quarkus extension** with
the mandatory **`runtime` + `deployment`** split, `@BuildStep`/`@Recorder`, and
**native image** support as a goal. Build-time augmentation (wiring, reflection
registration for the generated gRPC/protobuf classes, config validation) is the
reason to be an extension rather than a runtime-only helper.

---

## 2. Relationship with the proxy (governance)

- **The proxy is the source of truth.** The `.proto` **contract** is defined and
  versioned in the proxy repository (`src/main/proto/`, package
  `io.github.claudineyns.redis.grpc.v1`). This extension **consumes** that
  contract; it never redefines it (role analogous to a shared WSDL/XSD in SOAP).
- **Contract families:** `StringService`, `HashService`, `SetService`,
  `KeyService` — one service per Redis command family, versioned by directory
  (`.../v1`).
- **Auth model to honor:** the proxy authenticates callers via an
  **ACCESS_KEY / SECRET_KEY** pair carried in gRPC metadata (default headers
  `x-grpc-access-key` / `x-grpc-secret-key`), over mandatory one-way edge TLS.
  The client must be able to attach these credentials per call.
- **[OPEN] Contract sharing mechanism.** How the `.proto` reaches this project —
  publishing a contract artifact vs. direct reference vs. vendored copy — is the
  **first design decision to settle after the skeleton**. The proxy DESIGN
  explicitly deferred it to "the extension phase".
- **Reference snapshot (vendored).** A wire-faithful copy obtained via reflection
  lives in [../contract/](../contract/), validated semantically against the proxy
  source. It is a **design reference**, not the build input — the sharing mechanism
  above remains **[OPEN]**.

---

## 3. Stack

- **Quarkus** — **community** (`io.quarkus`), **3.15 LTS** floor; built/tested
  against the latest 3.15 patch (`3.15.7`). Deliberately community (not the proxy's
  Red Hat build) so the extension targets the broad ecosystem; a single coherent
  version drives the BOM and all Quarkus plugins (no platform/core build-number
  split to juggle).
- **Java 21** (`maven.compiler.release=21`).
- **gRPC** reactive with **Mutiny** (`Uni`/`Multi`) — same model as the proxy.

### 3.1 Version strategy and consumer compatibility

- **Floor = 3.15 LTS.** Conservative within the 3.x major: an LTS floor (not the
  oldest 3.x, which predates solid Java 21 support and uses EOL tooling).
- **Consumers govern their own versions.** A consuming Quarkus app imports its own
  platform BOM; in Maven, the consumer's `dependencyManagement` overrides the
  versions of all shared artifacts (`io.quarkus:*`, `io.grpc:*`, protobuf, mutiny)
  this extension brings transitively. So a consumer on any `3.x >= 3.15` — Red Hat
  *or* community — aligns those to its own version; our pinned version is just the
  build/test baseline and the fallback when the consumer manages nothing.
- **Two compatibility tiers (the real nuance):** runtime APIs are stable within the
  major (and realigned by the consumer's BOM), so building against a low floor is
  safe there; the **deployment SPI** (`@BuildStep`/`BuildItem`) is **not** ABI-
  guaranteed across 3.x minors. Mitigation: keep the `deployment` module **thin**
  and use only the most stable build items (`AdditionalBeanBuildItem`,
  `SyntheticBeanBuildItem`, `ReflectiveClassBuildItem`).
- **Codegen tooling does not propagate.** `quarkus-grpc-codegen` is `optional`
  (and excludes `quarkus-core-deployment`), so consumers never receive it — only
  our own build uses it for `generate-code` (see §6/§7 and the 2a notes).
- **Minimum supported Quarkus: 3.15** (documented in the README).

---

## 4. Module layout (decided — current skeleton)

Canonical Quarkus extension structure, multi-module Maven:

```
redis-grpc-client-parent        (root pom — aggregator, dependencyManagement, plugin config)
├── runtime/      → io.github.claudineyns:redis-grpc-client
│     The artifact the consuming app adds. Holds runtime code + the
│     META-INF/quarkus-extension.yaml descriptor.
└── deployment/   → io.github.claudineyns:redis-grpc-client-deployment
      Build-time augmentation: @BuildStep processors (FeatureBuildItem today;
      reflection/bean wiring later). Depends on runtime.
```

- **Identity:** groupId `io.github.claudineyns`; base package
  `io.github.claudineyns.redis.grpc.client`; version `0.1.0-SNAPSHOT`
  (early stage, distinct from the proxy's own versioning).
- **Generated via** the official `quarkus-maven-plugin:create-extension` generator,
  then pruned to the runtime + deployment pair and aligned to community Quarkus (§3).
- **`integration-tests` module:** dropped at scaffolding; **re-added under P1** for
  JVM + native validation — a consumer-like app hosting the hermetic fake server
  (§7, 2e/2f).
- **Status:** the skeleton builds and the extension loads
  (`Installed features: [cdi, redis-grpc-client]`); it carries **no client logic
  yet** beyond the `FeatureBuildItem`.

---

## 5. Build-time vs. runtime split (decided — P1)

- **Runtime module:** the **`RedisGrpcClient` aggregator** and its four family
  clients (Level 1 — §7) over the Vert.x `GrpcClient`; the `@ConfigMapping` config
  root (§8); a **CDI producer** (no `@Recorder`) that builds the `GrpcClient` from
  the Quarkus-managed `Vertx` + the TLS Registry and produces the aggregator; the
  shared call helper that injects the credential headers (§7, 2d).
- **Deployment module:** `@BuildStep`s — `FeatureBuildItem`;
  `AdditionalBeanBuildItem` (so consumers always discover the producer/clients);
  `ReflectiveClassBuildItem` (native: the generated protobuf message classes — §7,
  2e). **No `@Recorder`:** nothing needs build-time/static-init work, so the
  deployment stays lean (a deliberate application of the "enxuto" premise).

---

## 6. Code conventions (ratified)

Inherited from the adjacent proxy's DESIGN §10 and **ratified as binding** for
this project:

- **Reactive (Mutiny) is the priority** — end-to-end; do not block the event loop;
  no `@Blocking` on the hot path.
- **Virtual threads only when necessary.** Prefer `@RunOnVirtualThread` over the
  worker pool for unavoidable blocking offload/background work — the exception, not
  the hot-path default. There, avoid `synchronized` (use `ReentrantLock`) to avoid
  pinning on Java 21.
- **Field injection (CDI) is accepted** (Sonar `java:S6813`).
- **`final` wherever applicable** — locals and parameters without reassignment
  (CDI-injected fields excepted).
- **Didactic comments.** This is also a learning project: favor the *why*
  (gRPC/protobuf decisions, mapping choices) over the obvious *what*.
- **Prefer guard clauses over `if/else`** — early returns to reduce nesting.
- **String constants** for repeated literals (Sonar `java:S1192`), including
  `switch` case labels.

> **Not ratified (left open):** the reframed mandatory principles and the testing
> conventions from the proxy DESIGN (§2/§9) — to be revisited when client logic
> begins. Sonar/Jacoco tooling is deferred until the first vertical.

---

## 7. Implementation decisions (status, in order)

**Guiding premises (flexible, not rigid):** **Mutiny** — keep the extension
"Quarkus reactive friendly" wherever sensible; **enxuto** — avoid unnecessary code
(hence: no `@Recorder`, no generated stubs).

**Implementation roadmap (P1 stages).** Code lands in this order; each stage is
small and independently verified. (These are the work units; the numbered
decisions below give the rationale.)

| Stage | Scope |
|---|---|
| **2a** | Codegen **messages only** — `protoc --java_out` via a protobuf Maven plugin; drop `quarkus-grpc-stubs`/`-codegen`/grpc-netty. |
| **2b** | Config surface — the `@ConfigMapping` root (§8) + `quarkus-tls-registry` deps (runtime + `-deployment`). |
| **2c** | Channel + clients — `vertx-grpc-client` `GrpcClient` via a CDI producer (managed `Vertx` + TLS Registry); the `RedisGrpcClient` aggregator + its four family clients (Level 1); `AdditionalBeanBuildItem`. |
| **2d** | Credentials — central ACCESS_KEY/SECRET_KEY header injection in the shared call helper. |
| **2e** | Native config — deployment `@BuildStep`s registering the message classes for reflection (native build **execution deferred**). |
| **2f** | Tests (official) — server-less **mock coverage** per family + the wiring test (committed); **functional** validation via **ephemeral, gitignored live tests** against the CRC gateway (env-gated, run per family). |
| **2g** | Metrics (**deferred to the end**) — optional Micrometer `Timer` per RPC (tags `service`/`method`/`status`); the extension records, the consumer exposes Prometheus. |

1. **[DECIDED] `.proto` contract sharing** (§2, §4) — **vendored** reference in
   `contract/`, copied to `runtime/src/main/proto` as the build input.
2. **[DECIDED] Architecture — P1: pure Vert.x gRPC, stub-less, client-only.**
   *(Supersedes the earlier `quarkus-grpc-stubs` + grpc-netty 2a.)* Priority is
   **maximum Vert.x integration** + leanness, so we drop `quarkus-grpc` entirely
   (server **and** its Mutiny stubs) and build directly on
   `io.vertx.grpc.client.GrpcClient` over the Quarkus-managed `Vertx`. The TLS
   Registry feeds the Vert.x client options natively; native is largely covered by
   Quarkus's Vert.x support.
3. **[DECIDED] Codegen (2a redo)** — generate **only the protobuf message classes**
   (`protoc --java_out` via a protobuf Maven plugin). **No service stubs** (neither
   the quarkus Mutiny plugin nor `vertx-grpc-protoc-plugin2`): gRPC only needs a
   transport + method descriptors + marshallers; stubs are optional sugar, and the
   consumer touches **our** high-level classes, which encapsulate the low level.
4. **[DECIDED] Public surface — a single `RedisGrpcClient` aggregator (Level 1).**
   One injectable bean `RedisGrpcClient` exposing the four families via
   `.string()` / `.hash()` / `.set()` / `.key()`; each returns a family client whose
   **message-typed** methods return Mutiny `Uni`, driving `GrpcClient` via
   `ServiceMethod` descriptors + protobuf marshallers. Encapsulates
   channel/descriptors/marshalling/headers; **faithful 1:1** (no business logic).
   *(Supersedes "expose raw stubs" and the earlier "four separate beans".)* A
   Level-2 ergonomic (primitive-typed) API is a possible **additive** future.
5. **[DECIDED] Configuration surface — 2b** — see §8.
6. **[DECIDED] Wiring — 2c** — a **CDI producer** (no `@Recorder`; nothing requires
   build-time/static-init work) builds the `GrpcClient` + the four clients;
   `AdditionalBeanBuildItem` registers them so consumers always discover them.
7. **[DECIDED] Credentials — 2d** — **central** header injection in the shared call
   helper (we own the `GrpcClientRequest`): attach ACCESS_KEY/SECRET_KEY **only when
   both** are configured. No `ClientInterceptor` needed.
8. **[DECIDED] Native — 2e** — the extension **never builds native itself**; it
   contributes native *config* via deployment `@BuildStep`s (register the generated
   message classes for reflection, + whatever `vertx-grpc-client` needs). The native
   binary is built by the **consumer** (or by our `integration-tests` module, which
   stands in as one). **Native build execution is deferred.**
9. **[DECIDED] Tests — 2f** — **official tests are server-less, coverage-focused via
   mocks**: a mocked `GrpcInvoker` (`MockGrpc`); per-family tests exercise every RPC
   (delegation + descriptor init), plus the wiring test. **Functional** correctness
   is guaranteed by **ephemeral, gitignored live tests** against the CRC gateway
   (env-gated via `REDIS_GRPC_ACCESS_KEY`, run per family, removed at the end).
   *(Replaces the earlier hermetic fake-server plan.)* The `integration-tests`
   module is reserved for native validation (2e, deferred).
10. **[DECIDED] Metrics — 2g (deferred to the end).** **Optional** Micrometer
   integration: a neutral `RedisGrpcMetrics` interface (NOOP default) instrumented in
   the shared call helper; a `MicrometerRedisGrpcMetrics` bean registered **only when
   `Capability.MICROMETER` is present** (`quarkus-micrometer` as an `optional` dep).
   One **`Timer "redis.grpc.client.call"`** per RPC, tags `service`/`method`/`status`
   (never the Redis key — cardinality). Toggle
   `quarkus.redis-grpc-client.metrics.enabled` (default true). The extension
   **records**; the consumer adds the Prometheus registry and exposes the endpoint.

---

## 8. Client configuration surface (decided — 2b)

`@ConfigMapping(prefix = "quarkus.redis-grpc-client")`, **RUN_TIME** phase, a
**single default client** (flat keys). Going multi-client (named map) later would
be a breaking config change, so it is deferred deliberately.

> **Namespace choice (decided):** the `quarkus.` prefix is used on purpose. As a
> Quarkus extension we register a config root there — legitimate even for a
> private/unofficial extension — which buys key recognition + typo validation, the
> generated config reference and Dev UI. The "don't use `quarkus.`" guidance targets
> *application* config (e.g. the proxy's `proxy.auth.*`), not *extension* config
> roots. A custom corporate prefix was considered and rejected (loses that tooling).

| Key | Type | Default | Notes |
|---|---|---|---|
| `host` | String | — (required) | gateway host |
| `port` | int | `443` | gateway port |
| `tls-configuration-name` | Optional&lt;String&gt; | — | name of a `quarkus.tls.<name>` config in the **Quarkus TLS Registry** |
| `authority` | Optional&lt;String&gt; | — | SNI/authority override (the leaf cert CN = route host) |
| `auth.access-key` | Optional&lt;String&gt; | — | **secret**, never logged |
| `auth.secret-key` | Optional&lt;String&gt; | — | **secret**, never logged |
| `auth.access-key-header` | String | `x-grpc-access-key` | server-configurable header name |
| `auth.secret-key-header` | String | `x-grpc-secret-key` | server-configurable header name |

- **TLS semantics:** `tls-configuration-name` set → TLS using that named registry
  config (CA/truststore there, reloadable); unset → the registry's **default** TLS
  config if present, else **plaintext** (dev). Production requires a TLS config.
- **Auth semantics:** the shared call helper (2d) attaches both headers **only if
  both** access-key and secret-key are present; otherwise it calls without
  credentials (the server answers `UNAUTHENTICATED` if it requires them).
- **Phase RUN_TIME:** endpoint, credentials and TLS come from env/secret — never
  baked at build (the proxy's "config-driven, environment-agnostic" principle).

**Implications locked by this surface:**

- **New dependency** `quarkus-tls-registry` (runtime) ⇒ by the runtime↔deployment
  mirror rule (learned in 2a), `quarkus-tls-registry-deployment` in `deployment`.
- **TLS feeds the Vert.x client options.** Under P1 the transport is pure Vert.x
  gRPC (§7), so the resolved `TlsConfiguration` from the registry is applied to the
  Vert.x `GrpcClient`/HTTP options directly — no Netty `SslContext` conversion.

---

## 9. Decisions (tracker)

- [x] Client side is a **proper Quarkus extension** (runtime + deployment split),
  not a plain library — inherited from the proxy DESIGN §3.1.
- [x] Identity: `io.github.claudineyns:redis-grpc-client` (runtime),
  `-deployment`, `-parent`; package `...redis.grpc.client`; `0.1.0-SNAPSHOT`.
- [x] Scaffolding via the official Quarkus `create-extension` generator, pruned
  to runtime + deployment.
- [x] Base **community Quarkus, 3.15 LTS floor** (built/tested on `3.15.7`) /
  Java 21 — conservative, consumer BOM governs shared versions (§3, §3.1).
  *(Superseded the initial Red Hat 3.27.3 baseline.)*
- [x] Code conventions from the proxy DESIGN §10 **ratified as binding** (§6).
- [x] Reference `.proto` snapshot vendored in `contract/` (reflection-derived,
  validated semantically against the proxy source) and used as the build input
  in `runtime/src/main/proto` (§2, §4, §7).
- [x] **Architecture P1 (2a redo)** — pure Vert.x gRPC, **stub-less**, client-only:
  drop `quarkus-grpc`/`-stubs`/`-codegen`/grpc-netty; build on
  `vertx-grpc-client` + the managed `Vertx`; codegen = **messages only** (§7).
  *(Superseded the quarkus-grpc-stubs + grpc-netty 2a.)*
- [x] **Public surface** — single **`RedisGrpcClient` aggregator** exposing four
  family clients (Level 1), message-typed, returning `Uni`; no raw stubs (§7).
  *(Superseded "raw Mutiny stubs" and "four separate beans".)*
- [x] **Configuration surface (2b)** — single default client under
  `quarkus.redis-grpc-client.*`, RUN_TIME, TLS via the Quarkus TLS Registry (§8).
- [x] **Wiring (2c)** — CDI producer, no `@Recorder`; `AdditionalBeanBuildItem` (§7).
- [x] **Credentials (2d)** — central header injection in the shared call helper (§7).
- [x] **Native (2e)** — deployment build steps (reflection on messages); extension
  contributes config, consumer builds native; **native execution deferred** (§7).
- [x] **Tests (2f)** — official: server-less **mock coverage** per family + wiring
  test; functional: **ephemeral, gitignored live tests** vs the CRC gateway;
  integration-tests reserved for native (§7).
- [x] **Metrics (2g)** — **decided** (optional Micrometer `Timer` per RPC, tags
  service/method/status); **implementation deferred to the end** (§7).
- [ ] Reframed mandatory principles / testing conventions (proxy §2/§9) — **not
  yet ratified**; Sonar/Jacoco deferred to the first vertical.
- [ ] **All of §7 is decided; implementation (code) is pending.**
