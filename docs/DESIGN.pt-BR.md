# DESIGN.pt-BR.md — arquitetura e design do `redis-grpc-client`

> Documento de arquitetura e design técnico do projeto (versão em **português**).
> Versão em inglês: [DESIGN.md](DESIGN.md). **Os dois arquivos são mantidos em
> sincronia** — toda alteração de design deve ser refletida em ambos.
> A referência principal e as diretrizes de trabalho do agente estão em
> [../CLAUDE.md](../CLAUDE.md).
> Itens marcados **[OPEN]** ainda não foram decididos — não implementar assumindo
> um lado sem confirmação.

---

## 1. Propósito e premissa

Este projeto é a **extensão Quarkus client-side** do gateway `redis-grpc` — o
proxy norte-sul gRPC-sobre-Redis implementado no projeto adjacente
`quarkus-redis-grpc`. A extensão permite que uma aplicação Quarkus **consuma**
esse gateway de forma ergonômica: injetar um cliente pronto, configurar endpoint
e credenciais declarativamente e chamar as famílias de comandos Redis
(KEY/VALUE, HASH, SET, KEY-geral) como RPCs gRPC tipados.

**Por que uma extensão "de verdade" (não uma lib/producer simples).** O DESIGN do
proxy (§3.1) decidiu que o lado cliente seria uma **extensão Quarkus de verdade**,
com o split obrigatório **`runtime` + `deployment`**, `@BuildStep`/`@Recorder` e
suporte a **native image** como objetivo. O augmentation em build-time (wiring,
registro de reflection para as classes gRPC/protobuf geradas, validação de config)
é a razão de ser uma extensão, e não um auxiliar só de runtime.

---

## 2. Relação com o proxy (governança)

- **O proxy é a fonte de verdade.** O **contrato** `.proto` é definido e versionado
  no repositório do proxy (`src/main/proto/`, pacote
  `io.github.claudineyns.redis.grpc.v1`). Esta extensão **consome** esse contrato;
  nunca o redefine (papel análogo ao WSDL/XSD compartilhado no SOAP).
- **Famílias do contrato:** `StringService`, `HashService`, `SetService`,
  `KeyService` — um serviço por família de comandos Redis, versionado por diretório
  (`.../v1`).
- **Modelo de auth a honrar:** o proxy autentica chamadores por um par
  **ACCESS_KEY / SECRET_KEY** carregado na metadata gRPC (headers padrão
  `x-grpc-access-key` / `x-grpc-secret-key`), sobre TLS de borda obrigatório
  (one-way). O cliente precisa anexar essas credenciais por chamada.
- **[OPEN] Mecanismo de compartilhamento do contrato.** Como o `.proto` chega a
  este projeto — publicar um artefato de contrato vs. referência direta vs. cópia
  vendorizada — é a **primeira decisão de design a resolver após o esqueleto**. O
  DESIGN do proxy explicitamente adiou isso para "a fase da extensão".
- **Snapshot de referência (vendorizado).** Uma cópia fiel ao contrato, obtida via
  reflection, está em [../contract/](../contract/), validada semanticamente contra
  a fonte do proxy. É **referência de design**, não input de build — o mecanismo de
  compartilhamento acima permanece **[OPEN]**.

---

## 3. Stack

- **Quarkus** — **community** (`io.quarkus`), **piso 3.15 LTS**; construído/testado
  contra o último patch 3.15 (`3.15.7`). Deliberadamente community (não o build
  Red Hat do proxy), para mirar o ecossistema amplo; uma única versão coerente
  dirige o BOM e todos os plugins Quarkus (sem o split de build-number
  plataforma/core para administrar).
- **Java 21** (`maven.compiler.release=21`).
- **gRPC** reativo com **Mutiny** (`Uni`/`Multi`) — mesmo modelo do proxy.

### 3.1 Estratégia de versão e compatibilidade do consumidor

- **Piso = 3.15 LTS.** Conservador dentro do major 3.x: um piso LTS (não o 3.x mais
  antigo, que antecede o suporte sólido a Java 21 e usa tooling EOL).
- **O consumidor governa as próprias versões.** Uma app Quarkus consumidora importa
  o BOM de plataforma dela; no Maven, o `dependencyManagement` do consumidor
  sobrescreve as versões de todos os artefatos compartilhados (`io.quarkus:*`,
  `io.grpc:*`, protobuf, mutiny) que esta extensão traz transitivamente. Logo, um
  consumidor em qualquer `3.x >= 3.15` — Red Hat *ou* community — realinha tudo à
  versão dele; a nossa versão fixa é só a baseline de build/teste e o fallback
  quando o consumidor não gerencia nada.
- **Dois níveis de compatibilidade (a nuance real):** as APIs de runtime são
  estáveis dentro do major (e realinhadas pelo BOM do consumidor), então compilar
  contra um piso baixo é seguro aí; já a **SPI de deployment** (`@BuildStep`/
  `BuildItem`) **não** tem ABI garantida entre minors do 3.x. Mitigação: manter o
  módulo `deployment` **enxuto** e usar só os build items mais estáveis
  (`AdditionalBeanBuildItem`, `SyntheticBeanBuildItem`, `ReflectiveClassBuildItem`).
- **O tooling de codegen não propaga.** `quarkus-grpc-codegen` é `optional` (e
  exclui `quarkus-core-deployment`), então os consumidores nunca o recebem — só o
  nosso build o usa para o `generate-code` (ver §6/§7 e as notas do 2a).
- **Versão mínima de Quarkus suportada: 3.15** (documentada no README).

---

## 4. Layout de módulos (decidido — esqueleto atual)

Estrutura canônica de extensão Quarkus, Maven multi-módulo:

```
redis-grpc-client-parent        (pom raiz — agregador, dependencyManagement, config de plugins)
├── runtime/      → io.github.claudineyns:redis-grpc-client
│     O artefato que a app consumidora adiciona. Contém o código de runtime + o
│     descritor META-INF/quarkus-extension.yaml.
└── deployment/   → io.github.claudineyns:redis-grpc-client-deployment
      Augmentation em build-time: processadores @BuildStep (FeatureBuildItem hoje;
      reflection/wiring de beans depois). Depende do runtime.
```

- **Identidade:** groupId `io.github.claudineyns`; pacote base
  `io.github.claudineyns.redis.grpc.client`; versão `0.1.0-SNAPSHOT`
  (early stage, distinta do versionamento do proxy).
- **Gerado pelo** gerador oficial `quarkus-maven-plugin:create-extension`, depois
  podado para o par runtime + deployment e alinhado ao Quarkus community (§3).
- **Módulo `integration-tests`:** descartado no scaffolding; **re-adicionado sob o
  P1** para validação JVM + native — um app "como consumidor" hospedando o fake
  server hermético (§7, 2e/2f).
- **Status:** o esqueleto builda e a extensão carrega
  (`Installed features: [cdi, redis-grpc-client]`); **ainda não há lógica de
  cliente** além do `FeatureBuildItem`.

---

## 5. Split build-time vs. runtime (decidido — P1)

- **Módulo runtime:** as **classes de alto nível por família** (Nível 1 — §7) sobre
  o `GrpcClient` do Vert.x; o config root `@ConfigMapping` (§8); um **producer CDI**
  (sem `@Recorder`) que monta o `GrpcClient` a partir do `Vertx` gerenciado + o TLS
  Registry e produz os quatro clientes; o helper de chamada compartilhado que injeta
  os headers de credencial (§7, 2d).
- **Módulo deployment:** `@BuildStep`s — `FeatureBuildItem`;
  `AdditionalBeanBuildItem` (para o consumidor sempre descobrir o producer/clientes);
  `ReflectiveClassBuildItem` (native: as classes de mensagem geradas — §7, 2e).
  **Sem `@Recorder`:** nada exige trabalho em build-time/static-init, então o
  deployment fica enxuto (aplicação deliberada da premissa "enxuto").

---

## 6. Convenções de código (ratificadas)

Herdadas do DESIGN §10 do proxy adjacente e **ratificadas como vinculantes** neste
projeto:

- **Mutiny reativo é prioridade** — fim-a-fim; não bloquear o event loop; sem
  `@Blocking` no hot path.
- **Virtual threads só quando necessário.** Preferir `@RunOnVirtualThread` ao pool
  de workers para offload de bloqueio inevitável/tarefa de fundo — a exceção, não
  o padrão do hot path. Nesses pontos, evitar `synchronized` (usar
  `ReentrantLock`) para não causar pinning no Java 21.
- **Field injection (CDI) é aceito** (Sonar `java:S6813`).
- **`final` onde aplicável** — locais e parâmetros sem reatribuição (campos
  injetados por CDI são exceção).
- **Comentários didáticos.** Também é um projeto de aprendizado: favorecer o
  *porquê* (decisões gRPC/protobuf, escolhas de mapeamento) sobre o óbvio *o quê*.
- **Preferir guard clauses a `if/else`** — early returns para reduzir aninhamento.
- **Constantes de string** para literais repetidos (Sonar `java:S1192`), incluindo
  rótulos de `case` em `switch`.

> **Não ratificado (em aberto):** os princípios mandatórios reenquadrados e as
> convenções de teste do DESIGN do proxy (§2/§9) — a revisitar quando a lógica de
> cliente começar. O ferramental Sonar/Jacoco fica diferido até a primeira
> vertical.

---

## 7. Decisões de implementação (status, em ordem)

**Premissas balizadoras (flexíveis, não rígidas):** **Mutiny** — manter a extensão
"Quarkus reactive friendly" sempre que fizer sentido; **enxuto** — evitar código
desnecessário (daí: sem `@Recorder`, sem stubs gerados).

**Roadmap de implementação (estágios P1).** O código entra nesta ordem; cada
estágio é pequeno e verificado de forma independente. (São as unidades de trabalho;
as decisões numeradas abaixo dão a justificativa.)

| Estágio | Escopo |
|---|---|
| **2a** | Codegen **só mensagens** — `protoc --java_out` via plugin Maven; largar `quarkus-grpc-stubs`/`-codegen`/grpc-netty. |
| **2b** | Superfície de config — o root `@ConfigMapping` (§8) + deps `quarkus-tls-registry` (runtime + `-deployment`). |
| **2c** | Canal + clientes — `GrpcClient` do `vertx-grpc-client` via producer CDI (`Vertx` gerenciado + TLS Registry); os quatro clientes de alto nível Nível 1; `AdditionalBeanBuildItem`. |
| **2d** | Credenciais — injeção central dos headers ACCESS_KEY/SECRET_KEY no helper de chamada compartilhado. |
| **2e** | Config de native — `@BuildStep`s no deployment registrando as classes de mensagem para reflection (**execução** do build native **adiada**). |
| **2f** | Testes — re-adicionar o módulo `integration-tests` (fake server Vert.x hermético + testes JVM/native + teste opt-in ao vivo no CRC). |

1. **[DECIDIDO] Compartilhamento do contrato `.proto`** (§2, §4) — referência
   **vendorizada** em `contract/`, copiada para `runtime/src/main/proto` como input
   de build.
2. **[DECIDIDO] Arquitetura — P1: Vert.x gRPC puro, sem stubs, só cliente.**
   *(Substitui o 2a anterior com `quarkus-grpc-stubs` + grpc-netty.)* A prioridade é
   **máxima integração Vert.x** + enxuto, então largamos o `quarkus-grpc` inteiro
   (servidor **e** seus stubs Mutiny) e construímos direto sobre o
   `io.vertx.grpc.client.GrpcClient` no `Vertx` gerenciado. O TLS Registry alimenta
   as opções Vert.x nativamente; native é coberto em grande parte pelo suporte
   Vert.x do Quarkus.
3. **[DECIDIDO] Codegen (refaz 2a)** — gerar **só as classes de mensagem** protobuf
   (`protoc --java_out` via plugin Maven). **Sem stubs de serviço** (nem o plugin
   Mutiny do quarkus nem o `vertx-grpc-protoc-plugin2`): gRPC só precisa de
   transporte + descritores de método + marshallers; stubs são açúcar opcional, e o
   consumidor toca nas **nossas** classes de alto nível, que encapsulam o baixo nível.
4. **[DECIDIDO] Superfície pública — classes de alto nível por família (Nível 1).**
   Uma classe por família (String/Hash/Set/Key), métodos **tipados por mensagem**
   retornando `Uni`, dirigindo o `GrpcClient` via descritores `ServiceMethod` +
   marshallers protobuf. Encapsula canal/descritores/marshalling/headers; **fiel 1:1**
   (sem lógica de negócio). *(Substitui "expor stubs crus".)* Um Nível 2 ergonômico
   (tipado por primitivos) é um futuro **aditivo**, não agora.
5. **[DECIDIDO] Superfície de configuração — 2b** — ver §8.
6. **[DECIDIDO] Fiação — 2c** — um **producer CDI** (sem `@Recorder`; nada exige
   trabalho em build-time/static-init) monta o `GrpcClient` + os quatro clientes;
   `AdditionalBeanBuildItem` os registra para o consumidor sempre descobrir.
7. **[DECIDIDO] Credenciais — 2d** — injeção de header **central** no helper de
   chamada compartilhado (somos donos do `GrpcClientRequest`): anexa
   ACCESS_KEY/SECRET_KEY **só quando ambas** estão configuradas. Sem `ClientInterceptor`.
8. **[DECIDIDO] Native — 2e** — a extensão **nunca builda native**; ela contribui
   *config* de native via `@BuildStep`s do deployment (registrar as classes de
   mensagem para reflection, + o que o `vertx-grpc-client` exigir). O binário native
   é construído pelo **consumidor** (ou pelo nosso módulo `integration-tests`, que faz
   o papel dele). **Execução do build native adiada.**
9. **[DECIDIDO] Testes — 2f** — re-adicionar o módulo `integration-tests` (app "como
   consumidor"): um **fake server** Vert.x gRPC hermético no teste + `@QuarkusTest`
   (JVM) e `@QuarkusIntegrationTest` (native — execução adiada), mais um teste
   **opt-in** ao vivo contra o gateway no CRC.

---

## 8. Superfície de configuração do cliente (decidida — 2b)

`@ConfigMapping(prefix = "quarkus.redis-grpc-client")`, fase **RUN_TIME**, um
**cliente único default** (chaves planas). Migrar para multi-cliente (map nomeado)
depois seria quebra de config, então fica deliberadamente adiado.

> **Escolha do namespace (decidida):** o prefixo `quarkus.` é usado de propósito.
> Como extensão Quarkus, registramos um config root ali — legítimo mesmo para uma
> extensão privada/não-oficial — o que dá reconhecimento de chaves + validação de
> typo, a referência de config gerada e a Dev UI. A orientação "não use `quarkus.`"
> mira config de *aplicação* (ex.: `proxy.auth.*` do proxy), não config root de
> *extensão*. Um prefixo corporativo próprio foi considerado e descartado (perde
> esse tooling).

| Chave | Tipo | Default | Notas |
|---|---|---|---|
| `host` | String | — (obrigatório) | host do gateway |
| `port` | int | `443` | porta do gateway |
| `tls-configuration-name` | Optional&lt;String&gt; | — | nome de um `quarkus.tls.<nome>` no **Quarkus TLS Registry** |
| `authority` | Optional&lt;String&gt; | — | override SNI/authority (CN do cert folha = host da Route) |
| `auth.access-key` | Optional&lt;String&gt; | — | **segredo**, nunca logado |
| `auth.secret-key` | Optional&lt;String&gt; | — | **segredo**, nunca logado |
| `auth.access-key-header` | String | `x-grpc-access-key` | nome de header configurável no servidor |
| `auth.secret-key-header` | String | `x-grpc-secret-key` | nome de header configurável no servidor |

- **Semântica de TLS:** `tls-configuration-name` setado → TLS usando essa config
  nomeada do registry (CA/truststore lá, recarregável); não setado → a config TLS
  **default** do registry se existir, senão **plaintext** (dev). Produção exige uma
  config TLS.
- **Semântica de auth:** o helper de chamada compartilhado (2d) anexa ambos os
  headers **só se ambos** access-key e secret-key estiverem presentes; caso
  contrário chama sem credenciais (o servidor responde `UNAUTHENTICATED` se as exigir).
- **Fase RUN_TIME:** endpoint, credenciais e TLS vêm de env/secret — nunca assados
  no build (princípio "config-driven, agnóstico ao ambiente" do proxy).

**Implicações travadas por esta superfície:**

- **Nova dependência** `quarkus-tls-registry` (runtime) ⇒ pela regra do espelhamento
  runtime↔deployment (aprendida no 2a), `quarkus-tls-registry-deployment` no
  `deployment`.
- **TLS alimenta as opções Vert.x.** Sob o P1 o transporte é Vert.x gRPC puro (§7),
  então a `TlsConfiguration` resolvida do registry é aplicada diretamente nas opções
  do `GrpcClient`/HTTP do Vert.x — sem conversão para `SslContext` do Netty.

---

## 9. Decisões (tracker)

- [x] O lado cliente é uma **extensão Quarkus de verdade** (split runtime +
  deployment), não uma lib simples — herdado do DESIGN §3.1 do proxy.
- [x] Identidade: `io.github.claudineyns:redis-grpc-client` (runtime),
  `-deployment`, `-parent`; pacote `...redis.grpc.client`; `0.1.0-SNAPSHOT`.
- [x] Scaffolding via o gerador oficial `create-extension` do Quarkus, podado para
  runtime + deployment.
- [x] Base **community Quarkus, piso 3.15 LTS** (build/teste em `3.15.7`) /
  Java 21 — conservador, BOM do consumidor governa as versões compartilhadas
  (§3, §3.1). *(Substituiu a baseline inicial Red Hat 3.27.3.)*
- [x] Convenções de código do DESIGN §10 do proxy **ratificadas como vinculantes**
  (§6).
- [x] Snapshot de referência do `.proto` vendorizado em `contract/`
  (derivado de reflection, validado semanticamente contra a fonte do proxy) e usado
  como input de build em `runtime/src/main/proto` (§2, §4, §7).
- [x] **Arquitetura P1 (refaz 2a)** — Vert.x gRPC puro, **sem stubs**, só cliente:
  largar `quarkus-grpc`/`-stubs`/`-codegen`/grpc-netty; construir sobre
  `vertx-grpc-client` + o `Vertx` gerenciado; codegen = **só mensagens** (§7).
  *(Substituiu o 2a com quarkus-grpc-stubs + grpc-netty.)*
- [x] **Superfície pública** — **classes de alto nível por família (Nível 1)**,
  tipadas por mensagem, retornando `Uni`; sem stubs crus (§7). *(Substituiu "stubs Mutiny crus".)*
- [x] **Superfície de configuração (2b)** — cliente único default sob
  `quarkus.redis-grpc-client.*`, RUN_TIME, TLS via Quarkus TLS Registry (§8).
- [x] **Fiação (2c)** — producer CDI, sem `@Recorder`; `AdditionalBeanBuildItem` (§7).
- [x] **Credenciais (2d)** — injeção de header central no helper compartilhado (§7).
- [x] **Native (2e)** — build steps no deployment (reflection nas mensagens); a
  extensão contribui config, o consumidor builda native; **execução adiada** (§7).
- [x] **Testes (2f)** — módulo `integration-tests`: fake server Vert.x hermético +
  testes JVM/native + teste opt-in ao vivo no CRC (§7).
- [ ] Princípios mandatórios reenquadrados / convenções de teste (proxy §2/§9) —
  **ainda não ratificados**; Sonar/Jacoco diferido até a primeira vertical.
- [ ] **Tudo na §7 está decidido; a implementação (código) está pendente.**
