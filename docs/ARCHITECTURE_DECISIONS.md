# Architecture Decision Records (ADRs)

ADRs curtos das decisões relevantes. Formato: contexto → decisão → consequências.

---

## ADR-001 — Clean Architecture + MVI

**Contexto:** critério de manutenibilidade e testabilidade; UI precisa de estado
previsível.
**Decisão:** camadas domain/data/presentation com dependências para dentro; MVI
na apresentação (State imutável, Intents, reducer único, Effects one-shot).
**Consequências:** `:domain` testável sem Android; ViewModels determinísticos
(testados com Turbine). Custo: mais boilerplate (contratos por tela).

---

## ADR-002 — Multi-módulo com `:domain` puro Kotlin

**Contexto:** fronteiras explícitas e build incremental.
**Decisão:** `:domain` e `:core:common` são módulos **Kotlin puro** (sem Android);
`:data`, `:payment`, features e `:app` são Android.
**Consequências:** impossível vazar Android para o domínio (garantido pelo
compilador). Testes de domínio rodam como JUnit puro, rápidos.

---

## ADR-003 — Room, não backend próprio nem Firestore

**Contexto:** o case afirma que backend não será avaliado.
**Decisão:** persistência local com Room; eventos vêm de um seed
(`assets/events.json`) populado no primeiro boot.
**Consequências:** atrito zero para o avaliador rodar (sem credenciais). Room é a
camada natural para a idempotência (constraint UNIQUE). Trocar por API remota =
nova implementação de `EventRepository`/`PurchaseRepository`.

---

## ADR-004 — Integração Cielo via Deeplink (não SDK)

**Contexto:** a Cielo descontinuou o SDK e recomenda Deeplink; WebView não é
permitido na nova Cielo Smart.
**Decisão:** implementar `PaymentGateway` via Deeplink (Intent + callback). O app
não embarca o SDK.
**Consequências:** mais leve, sem lib de terceiros; **sem** a restrição de
`targetSdk 29` (que só vale para o SDK embarcado). O round-trip entre apps exige
um barramento de callback (`CieloCallbackBus`) para retomar o gateway suspenso.

---

## ADR-005 — `PaymentGateway` no `:payment`; modelos no `:domain`

**Contexto:** o enunciado pede a interface `PaymentGateway` no `:payment`, mas
também chama `PaymentResult` de "modelo de domínio" e exige `:domain` puro.
**Decisão:** a **interface** fica no `:payment`; os **modelos** (`PaymentRequest`,
`PaymentResult`, `PaymentCode`, `PaymentError`) ficam no `:domain`. A orquestração
(`gateway.pay()` → `ReconcilePaymentUseCase`) fica no `CheckoutViewModel`.
**Consequências:** honra o enunciado sem `:domain` depender de `:payment`. Custo:
o "use case de compra" não invoca o gateway diretamente — a lógica testável de
idempotência está em `CreatePendingPurchase`/`ReconcilePayment`, que consomem um
`PaymentResult` já mapeado.
**Alternativa considerada:** port `PaymentGateway` no `:domain` (ports & adapters
clássico). Rejeitada por divergir do texto do enunciado.

---

## ADR-006 — Design System via composite build com substituição explícita

**Contexto:** o DS é um build Gradle independente, ainda não publicado no Maven.
**Decisão:** `includeBuild` no `settings.gradle.kts`, declarando a dependência
pela coordenada Maven real (`io.github.matheusbrum:mns-design-system`), com
`dependencySubstitution` explícita para o projeto `:design_system`.
**Consequências:** a substituição automática não funciona porque o Gradle deriva
a coordenada do **nome** do projeto incluído (`:design_system`), não do
`artifactId` publicado. A substituição explícita resolve isso. Ao publicar no
Maven, remove-se o `includeBuild` e a mesma linha do catálogo resolve o remoto.

---

## ADR-007 — Tokens Cielo via `local.properties` → `BuildConfig` (leitura manual)

**Contexto:** os tokens são gerados manualmente pelo desenvolvedor e não podem
ser versionados.
**Decisão:** ler `local.properties` manualmente com `java.util.Properties` no
`build.gradle.kts` do `:payment` e expor via `buildConfigField`.
**Consequências:** independe da assinatura de `gradleLocalProperties` (que mudou
entre versões do AGP e quebra o sync). Compila com tokens vazios.

---

## ADR-008 — `:core:ui` para a base MVI

**Contexto:** evitar duplicar a infraestrutura MVI entre features.
**Decisão:** módulo `:core:ui` com `MviViewModel` (State/Intent/Effect).
**Consequências:** features só definem seus contratos e VMs. Custo: mais um módulo.

---

## ADR-009 — Estoque decrementado na conciliação (não reservado)

**Contexto:** evitar travar estoque em tentativas de pagamento abandonadas.
**Decisão:** `decrementAvailability` só na transição para APPROVED, de forma
condicional (não decrementa abaixo de zero).
**Consequências:** simples e sem "vazamento" de estoque reservado; aceita uma
janela de corrida teórica sob altíssima concorrência (fora do escopo do case).
