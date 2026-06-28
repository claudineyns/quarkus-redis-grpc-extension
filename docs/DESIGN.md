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

- **Quarkus** — Red Hat build (`com.redhat.quarkus.platform`), platform
  **3.27.3.redhat-00003**, matching the proxy.
- **Java 21**.
- **gRPC** reactive with **Mutiny** (`Uni`/`Multi`) — same model as the proxy.

### 3.1 Red Hat platform vs. core build numbers (build note)

The **platform** BOM (`com.redhat.quarkus.platform:quarkus-bom`) is at
`3.27.3.redhat-00003`. The **extension build tooling**
(`io.quarkus:quarkus-extension-maven-plugin`, `quarkus-extension-processor`)
follows the upstream **core** build, which in the Red Hat productization carries a
**different build number** — `3.27.3.redhat-00001`. A plain application never sees
this (it uses the platform `quarkus-maven-plugin`), but an extension references the
core-aligned tooling directly. The two are pinned via separate properties:
`quarkus.version` (platform, -00003) and `quarkus.core.version` (core, -00001).

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

## 7. Open decisions (to settle next, in order)

1. **[OPEN] `.proto` contract sharing** (§2) — artifact vs. direct reference vs.
   vendored; and where stub generation runs (this project vs. consuming app).
2. **[OPEN] Stub generation** — use `quarkus-grpc` code generation from the
   `.proto`, or consume pre-generated stubs; reactive Mutiny stubs assumed.
3. **[OPEN] Public client surface** — raw per-family gRPC stubs vs. a thin
   higher-level facade; how absence/nil and gRPC status map back to the caller.
4. **[OPEN] Configuration surface** — `@ConfigMapping` keys for endpoint, TLS,
   ACCESS_KEY/SECRET_KEY headers and values; dev-mode defaults.
5. **[OPEN] Credential injection** — interceptor that attaches the
   ACCESS_KEY/SECRET_KEY metadata on every call.
6. **[OPEN] Native image** — reflection/proxy registration build steps and a
   native integration test.

---

## 8. Decisions (tracker)

- [x] Client side is a **proper Quarkus extension** (runtime + deployment split),
  not a plain library — inherited from the proxy DESIGN §3.1.
- [x] Identity: `io.github.claudineyns:redis-grpc-client` (runtime),
  `-deployment`, `-parent`; package `...redis.grpc.client`; `0.1.0-SNAPSHOT`.
- [x] Scaffolding via the official Quarkus `create-extension` generator, pruned
  to runtime + deployment.
- [x] Platform `com.redhat.quarkus.platform` 3.27.3.redhat-00003 / Java 21;
  extension tooling pinned to core `3.27.3.redhat-00001` (§3.1).
- [x] Code conventions from the proxy DESIGN §10 **ratified as binding** (§6).
- [x] Reference `.proto` snapshot vendored in `contract/` (reflection-derived,
  validated semantically against the proxy source); build-wiring still open
  (§2, §7).
- [ ] Reframed mandatory principles / testing conventions (proxy §2/§9) — **not
  yet ratified**; Sonar/Jacoco deferred to the first vertical.
- [ ] Everything in §7 remains **[OPEN]**.
