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
  then pruned to the runtime + deployment pair and aligned to the Red Hat platform.
- **Not included yet:** the generator's `integration-tests` module was dropped
  until there is real client behavior to exercise; it will be re-added with the
  first vertical.
- **Status:** the skeleton builds and the extension loads
  (`Installed features: [cdi, redis-grpc-client]`); it carries **no client logic
  yet** beyond the `FeatureBuildItem`.

---

## 5. Build-time vs. runtime split (design intent — not yet implemented)

The split is the reason this is an extension. Expected responsibilities, to be
designed in subsequent rounds (all **[OPEN]**):

- **Runtime module:** the injectable client(s) over the generated Mutiny gRPC
  stubs; configuration mapping (endpoint, TLS, credential headers); a `@Recorder`
  to construct/configure clients from recorded config at static/runtime init.
- **Deployment module:** `@BuildStep`s that register the generated
  protobuf/gRPC classes for **reflection** (native image), produce the CDI beans
  for injection, and validate configuration at build time.

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

1. **[DECIDED] `.proto` contract sharing** (§2, §4) — **vendored** reference in
   `contract/`, copied to `runtime/src/main/proto` as the build input. A published
   contract artifact remains a possible later evolution.
2. **[DECIDED] Stub generation — 2a** — **client-only**: generate the Mutiny stubs
   from the vendored `.proto` via the `generate-code` goal, depending on
   `quarkus-grpc-stubs` (no server) and `quarkus-grpc-codegen` as an **`optional`,
   build-only** provider (excluding `quarkus-core-deployment`). No `grpc-server`
   feature; consumer-facing runtime stays clean.
3. **[DECIDED] Public client surface** — expose the per-family **Mutiny stubs
   directly** (injectable), no higher-level facade for now.
4. **[DECIDED] Configuration surface — 2b** — see §8.
5. **[OPEN] Channel transport & wiring — 2c** — a `@Recorder` builds the `io.grpc`
   channel; transport leans **Vert.x** (native TLS Registry integration); CDI
   producers expose the stubs via build steps.
6. **[OPEN] Credential injection — 2d** — a `ClientInterceptor` attaches the
   ACCESS_KEY/SECRET_KEY metadata when both are configured.
7. **[OPEN] Native image — 2e** — reflection/proxy registration build steps and a
   native integration test.

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
- **Auth semantics:** the interceptor (2d) attaches both headers **only if both**
  access-key and secret-key are present; otherwise it calls without credentials
  (the server answers `UNAUTHENTICATED` if it requires them).
- **Phase RUN_TIME:** endpoint, credentials and TLS come from env/secret — never
  baked at build (the proxy's "config-driven, environment-agnostic" principle).

**Implications locked by this surface:**

- **New dependency** `quarkus-tls-registry` (runtime) ⇒ by the runtime↔deployment
  mirror rule (learned in 2a), `quarkus-tls-registry-deployment` in `deployment`.
- **Nudges 2c toward a Vert.x transport** (the TLS Registry integrates natively
  with Vert.x options; grpc-netty would need converting the registry config to a
  Netty `SslContext`).

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
- [x] **Stub generation (2a)** — client-only Mutiny codegen via `quarkus-grpc-stubs`
  + `optional` `quarkus-grpc-codegen`; no server (§7).
- [x] **Public surface** — per-family Mutiny stubs directly, no facade (§7).
- [x] **Configuration surface (2b)** — single default client under
  `quarkus.redis-grpc-client.*`, RUN_TIME, TLS via the Quarkus TLS Registry (§8).
- [ ] Reframed mandatory principles / testing conventions (proxy §2/§9) — **not
  yet ratified**; Sonar/Jacoco deferred to the first vertical.
- [ ] Open: channel/transport (2c), credential injection (2d), native image (2e)
  — see §7.
