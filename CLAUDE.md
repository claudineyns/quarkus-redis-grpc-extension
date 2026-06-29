# CLAUDE.md — Guia do agente (referência principal)

> Este é o documento **principal** de referência. Ele reúne as **orientações
> gerais ao agente** (premissas de operação e modo de trabalho) e aponta para os
> demais documentos do projeto.
>
> **Arquitetura, design técnico e decisões → [docs/DESIGN.md](docs/DESIGN.md).**

---

## Índice de documentos

| Documento | Conteúdo |
|---|---|
| **CLAUDE.md** (este) | Guia do agente: premissas de operação e modo de trabalho. Referência principal. |
| [docs/DESIGN.md](docs/DESIGN.md) | Arquitetura e design técnico — versão em **inglês** (canônica para leitura externa). |
| [docs/DESIGN.pt-BR.md](docs/DESIGN.pt-BR.md) | Arquitetura e design técnico — versão em **português**. |
| [README.md](README.md) | Visão geral pública do projeto (em **inglês**). |

> **DESIGN bilíngue:** `docs/DESIGN.md` (inglês) e `docs/DESIGN.pt-BR.md`
> (português) são **mantidos em sincronia**. Ver premissa na seção 1.

---

## O projeto em um parágrafo

`redis-grpc-client` é uma **extensão Quarkus client-side** do gateway `redis-grpc`
— implementado no projeto adjacente `quarkus-redis-grpc`, um **gateway gRPC sobre
Redis** (proxy norte-sul que expõe comandos Redis como RPCs gRPC 1:1, para que
clientes externos alcancem um Redis interno do cluster através de uma route
OpenShift com passthrough). Esta extensão **consome** esse gateway: ela não o
implementa nem redefine seu contrato `.proto` (esse é versionado no projeto do
proxy). Detalhes de arquitetura, modelagem e decisões da extensão estão em
[docs/DESIGN.md](docs/DESIGN.md).

---

## 1. Premissas de operação (ambiente de desenvolvimento)

- **Sempre usar `bash`** para executar comandos neste projeto (não PowerShell/cmd).
- Sempre que houver **referências de caminho Linux**, prefixar o comando com
  `MSYS_NO_PATHCONV=1` para evitar a conversão automática de caminhos do
  Git Bash/MSYS no Windows.

  ```bash
  MSYS_NO_PATHCONV=1 <comando com /caminho/linux>
  ```

- **Build sempre com o `mvn` do sistema** (não usar wrapper `mvnw` nem caçar
  binário alternativo). Ex.: `mvn -B -ntp clean compile`.
- **Comandos `oc` (OpenShift/CRC):** autenticar sempre como usuário `developer`
  e usar a estrutura
  `oc --context ${OC_CXT} -n ${OC_NAMESPACE} <command> [options]` (contexto e
  namespace via variáveis). Aplicar `MSYS_NO_PATHCONV=1` quando houver caminhos
  `/...` literais.
- **Dados temporários em `temp/`.** Usar a pasta `temp/` do projeto para qualquer
  arquivo/dado temporário (não o temp do sistema nem scratchpad). Já está no
  `.gitignore`, então não é versionada.
- **Conteúdo de `temp/` é NÃO-CONFIÁVEL (MANDATÓRIO).** Nada que esteja dentro de
  `temp/` pode ser assumido como válido/atual/correto — a única garantia é a
  **existência da pasta** como lugar temporário. Qualquer dado lido de `temp/`
  (credenciais, certificados, contratos, saídas geradas, etc.) deve ser
  **re-derivado ou re-verificado** a partir de uma fonte autoritativa antes de
  embasar qualquer decisão; nunca tratar artefato de `temp/` como fonte de verdade.
- **Scripts utilitários do servidor gRPC em Go (MANDATÓRIO).** Todo script
  utilitário para **exercitar/executar o servidor gRPC** (clientes de teste,
  smoke tests, automações de validação contra o servidor no CRC) deve ser escrito
  em **Go** — escolha deliberada com **objetivo de aprendizado de Go pelo autor**.
  (O projeto adjacente regulador usa Python para esse papel; aqui a linguagem é
  Go.) Go já está disponível no ambiente (`go1.26.4`, no PATH).
- **`README.md` sempre em inglês.** O conteúdo do README é mantido exclusivamente
  em inglês. O `CLAUDE.md` permanece em português.
- **DESIGN bilíngue, editado em paralelo (MANDATÓRIO).** A documentação de
  arquitetura existe em dois arquivos espelhados:
  - `docs/DESIGN.md` → **inglês**
  - `docs/DESIGN.pt-BR.md` → **português**

  Toda alteração de design DEVE ser aplicada **aos dois arquivos na mesma
  edição**, cada um no seu idioma, mantendo-os sincronizados em conteúdo,
  estrutura e numeração de seções. Nunca alterar um sem o outro.

---

## 2. Modo de trabalho: discutir antes de implementar (MANDATÓRIO)

- **Toda intenção de implementação DEVE ser submetida à discussão ANTES de ser
  aplicada.** Não escrever/alterar código de produção sem aprovação prévia da
  abordagem. Apresentar trade-offs e uma recomendação; só implementar após o "ok".
- **Construção de extensões é objetivo de aprendizado (MANDATÓRIO discutir).**
  Este projeto também serve como **aprendizado do autor sobre como se constrói uma
  extensão Quarkus**. Por isso, TODA intenção de implementação **específica do
  mecanismo de extensão** — split `runtime`/`deployment`, `@BuildStep`/`BuildItem`,
  `@Recorder` e bytecode recording, geração de código a partir do `.proto`,
  registro para *native image*, `@ConfigMapping`/`@ConfigRoot`, beans sintéticos,
  etc. — DEVE ser trazida à discussão antes de aplicar, explicando o **papel de
  cada peça** no ciclo de build/augmentation (o *porquê*, não só o *quê*).
- **Aulas didáticas são sob demanda.** Por padrão, NÃO dar aulas/explicações
  conceituais extensas — ir direto à discussão de implementação. O autor solicita
  uma **"aula excepcional"** quando tiver dúvida; aí sim explicar em profundidade —
  **gRPC/protobuf** ancorando em **JSON-RPC** e **SOAP/WSDL**, e a **mecânica de
  extensões Quarkus** (build-time vs. runtime, augmentation, recorders) — sempre
  ancorando em modelos que ele já conhece.
- **Escopo do fluxo de discussão:** editar `CLAUDE.md`, `docs/DESIGN.md` /
  `docs/DESIGN.pt-BR.md` e demais arquivos de config/documentação para **registrar
  decisões** é permitido durante a conversa; **código-fonte da aplicação, não** —
  esse passa pelo fluxo de discussão primeiro.
