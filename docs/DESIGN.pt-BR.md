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

---

## 3. Stack

- **Quarkus** — build da Red Hat (`com.redhat.quarkus.platform`), plataforma
  **3.27.3.redhat-00003**, igual à do proxy.
- **Java 21**.
- **gRPC** reativo com **Mutiny** (`Uni`/`Multi`) — mesmo modelo do proxy.

### 3.1 Número de build: plataforma vs. core na Red Hat (nota de build)

A BOM de **plataforma** (`com.redhat.quarkus.platform:quarkus-bom`) está em
`3.27.3.redhat-00003`. Já o **ferramental de build de extensão**
(`io.quarkus:quarkus-extension-maven-plugin`, `quarkus-extension-processor`)
segue o build do **core** upstream, que na produtização Red Hat carrega um
**número de build diferente** — `3.27.3.redhat-00001`. Uma aplicação comum nunca
vê isso (usa o `quarkus-maven-plugin` de plataforma), mas uma extensão referencia
o ferramental alinhado ao core diretamente. Os dois são fixados em propriedades
separadas: `quarkus.version` (plataforma, -00003) e `quarkus.core.version`
(core, -00001).

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
  podado para o par runtime + deployment e alinhado à plataforma Red Hat.
- **Ainda não incluído:** o módulo `integration-tests` do gerador foi descartado
  até haver comportamento real de cliente para exercitar; será re-adicionado com a
  primeira vertical.
- **Status:** o esqueleto builda e a extensão carrega
  (`Installed features: [cdi, redis-grpc-client]`); **ainda não há lógica de
  cliente** além do `FeatureBuildItem`.

---

## 5. Split build-time vs. runtime (intenção de design — ainda não implementado)

O split é a razão de isto ser uma extensão. Responsabilidades esperadas, a serem
projetadas em rodadas seguintes (todas **[OPEN]**):

- **Módulo runtime:** o(s) cliente(s) injetável(is) sobre os stubs gRPC Mutiny
  gerados; mapeamento de configuração (endpoint, TLS, headers de credencial); um
  `@Recorder` para construir/configurar clientes a partir da config registrada em
  static/runtime init.
- **Módulo deployment:** `@BuildStep`s que registram as classes protobuf/gRPC
  geradas para **reflection** (native image), produzem os beans CDI para injeção e
  validam a configuração em build-time.

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

## 7. Decisões em aberto (resolver a seguir, em ordem)

1. **[OPEN] Compartilhamento do contrato `.proto`** (§2) — artefato vs. referência
   direta vs. vendorizado; e onde roda a geração de stubs (este projeto vs. app
   consumidora).
2. **[OPEN] Geração de stubs** — usar a geração de código do `quarkus-grpc` a
   partir do `.proto`, ou consumir stubs pré-gerados; stubs Mutiny reativos
   assumidos.
3. **[OPEN] Superfície pública do cliente** — stubs gRPC crus por família vs. uma
   fachada fina de mais alto nível; como ausência/nil e status gRPC voltam ao
   chamador.
4. **[OPEN] Superfície de configuração** — chaves `@ConfigMapping` para endpoint,
   TLS, headers e valores de ACCESS_KEY/SECRET_KEY; defaults de dev-mode.
5. **[OPEN] Injeção de credencial** — interceptor que anexa a metadata
   ACCESS_KEY/SECRET_KEY em toda chamada.
6. **[OPEN] Native image** — build steps de registro de reflection/proxy e um
   teste de integração nativo.

---

## 8. Decisões (tracker)

- [x] O lado cliente é uma **extensão Quarkus de verdade** (split runtime +
  deployment), não uma lib simples — herdado do DESIGN §3.1 do proxy.
- [x] Identidade: `io.github.claudineyns:redis-grpc-client` (runtime),
  `-deployment`, `-parent`; pacote `...redis.grpc.client`; `0.1.0-SNAPSHOT`.
- [x] Scaffolding via o gerador oficial `create-extension` do Quarkus, podado para
  runtime + deployment.
- [x] Plataforma `com.redhat.quarkus.platform` 3.27.3.redhat-00003 / Java 21;
  ferramental de extensão fixado no core `3.27.3.redhat-00001` (§3.1).
- [x] Convenções de código do DESIGN §10 do proxy **ratificadas como vinculantes**
  (§6).
- [ ] Princípios mandatórios reenquadrados / convenções de teste (proxy §2/§9) —
  **ainda não ratificados**; Sonar/Jacoco diferido até a primeira vertical.
- [ ] Tudo na §7 permanece **[OPEN]**.
