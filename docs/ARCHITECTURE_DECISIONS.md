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

## ADR-006 — Design System declarado por coordenada Maven (composite build → artefato publicado)

**Contexto:** no início o DS não estava publicado; era um build Gradle
independente, fora da árvore deste projeto.

**Decisão:** declarar a dependência **sempre pela coordenada Maven**, nunca por
`project(":…")`, e usar `includeBuild` + `dependencySubstitution` apenas como
mecanismo temporário de resolução local. A substituição precisou ser explícita
porque o Gradle deriva a coordenada de um projeto incluído do **nome** dele
(`:design_system`), não do `artifactId` publicado.

**Estado atual:** o DS foi publicado no Maven Central como
`io.github.matheusbrum11:mns-design-system:0.1.0`. O `includeBuild` e o
repositório de snapshots foram removidos; `mavenCentral()` basta.

**Consequências:** a migração custou exatamente o que a decisão prometia —
remover o bloco `includeBuild` e trocar a versão no catálogo, **sem alterar uma
linha do código de consumo**. Efeito colateral positivo: o projeto deixou de
depender de um caminho absoluto na máquina do desenvolvedor
(`/Users/matheusbrum/StudioProjects/mns-design-system`), então clona e compila em
qualquer máquina — inclusive na do avaliador.

**Nota:** o `group` mudou de `io.github.matheusbrum` para
`io.github.matheusbrum11` na publicação (namespace verificado no Central); o
catálogo e o filtro de repositório foram ajustados junto.

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

---

## ADR-012 — Conciliação do callback persistida (resiliência a morte de processo)

**Contexto:** no modelo deeplink o app sai de foreground enquanto o app da Cielo
processa o pagamento. A própria documentação alerta que o retorno pode falhar se
o app for encerrado nesse intervalo (e sugere foreground service). Se o desfecho
dependesse apenas do `CompletableDeferred` em memória, um processo morto
deixaria a compra órfã em PENDING — cobrada, sem registro.

**Decisão:** o callback é processado por `PaymentCallbackHandler`, que faz
*parse → concilia pelo `reference` → **persiste** → publica no barramento*, em um
**escopo de aplicação** (a Activity de callback finaliza imediatamente). O
`CheckoutViewModel` passa a **observar a compra do Room**; o retorno do
`gateway.pay()` vira apenas o atalho do caminho feliz.

**Consequências:**
- O desfecho sobrevive à morte do processo (verificado com `am force-stop` +
  entrega do callback: a compra foi de PENDING para APPROVED e o estoque baixou).
- O desfecho pode chegar por dois caminhos (Room + retorno do gateway); como
  `ReconcilePaymentUseCase` é idempotente, o segundo é no-op, e o ViewModel
  guarda a navegação para emitir `OpenReceipt` uma única vez.
- Um timeout no gateway **não** significa que o pagamento não ocorreu — a compra
  segue PENDING e é conciliada quando o retorno chegar.

---

## ADR-013 — Contrato Cielo derivado dos samples oficiais

**Contexto:** a página "Deep Link: exemplo de código" não traz o payload; ela
aponta para dois repositórios de exemplo (um deles na org DeveloperCielo), que
são a especificação executável do contrato.

**Decisão:** modelar request/response a partir desses samples, e não do exemplo
resumido do enunciado. Diferenças que isso corrigiu:

| Item | Suposição inicial | Contrato real |
|---|---|---|
| Sucesso | envelope com `payments[]` | **Order na raiz** (`reference`, `status`, `paidAmount`…) |
| Erro | `{code, reason}` | `{code, reason, **order**}` |
| Sucesso × erro | via `statusCode` | query param **`responsecode`** |
| `statusCode` | `Int` | **String** |
| `value` | string | **numérico** (centavos) |
| Request | sem `merchantCode` | tem `merchantCode` |
| URI | concatenação | **`Uri.Builder`** (Base64 tem `+`, `/`, `=`) |
| Callback host | `response` | `payment` (aceitamos ambos) |

**Consequências:** o parser aceita campos desconhecidos (`ignoreUnknownKeys`) e
`statusCode` como string ou número, para não quebrar quando a Cielo evoluir o
payload. O `<queries>` do Android 11+ foi adicionado para a detecção de "app não
instalado" funcionar de verdade.

---

## ADR-014 — Discriminação sucesso/erro do callback é estrutural, não por `responsecode`

**Contexto:** o sample oficial (Flutter) decide o desfecho pela presença do
query param `responsecode` na URI de callback (`if responsecode != null →
sucesso`). A primeira implementação seguiu isso.

**Problema observado** (emulador oficial da Cielo v1.61.8, cenário "Cancelado"):

```
order://payment?response=eyJjb2RlIjoxLCJyZWFzb24iOiJDQU5DRUxBRE8g…J9⏎&responsecode=0
                                                                    ↑ \n literal na URI
```

O cancelamento **também** traz `responsecode=0`. Com a regra do sample, o
payload `{"code":1,"reason":"CANCELADO PELO USUÁRIO"}` era interpretado como
Order de sucesso; como `ignoreUnknownKeys` descarta `code`/`reason`, virava uma
Order vazia sem `payments[]` → `InvalidResponse`. Um **cancelamento aparecia
como erro genérico**.

**Decisão:** discriminar pela ESTRUTURA do payload — `code`/`reason` ⇒ envelope
de falha; caso contrário, Order. O `responsecode` é ignorado.

**Consequências:** cancelamento e erro passam a mapear corretamente
(`Canceled` / `Denied`). Há teste de regressão com a URI real capturada do
emulador (com o `\n` e o `responsecode=0`). Também confirmou-se por que o sample
sanitiza `\n`: o Base64 chega quebrado dentro da própria URI.

---

## ADR-015 — Retry reusa a chave só enquanto PENDING

**Contexto:** o retry deve reaproveitar a chave de idempotência para não gerar
segunda cobrança. Mas a primeira implementação reusava a compra **sempre**.

**Problema:** após um desfecho **terminal** (negado/cancelado), reusar a mesma
chave fazia a conciliação virar no-op (a compra já está em estado final) — o
usuário ficava preso no resultado antigo, sem conseguir comprar de novo.

**Decisão:** reusar a compra apenas enquanto `isPending`; após um desfecho
terminal, uma nova tentativa cria uma nova compra com nova chave.

**Consequências:** preserva a proteção onde ela importa (tentativa em aberto,
possível cobrança pendente do outro lado) sem travar o fluxo. Verificado no
device: DENIED seguido de retry gerou chave nova e APPROVED, com baixa de
estoque só na aprovação. Complemento: o fim de uma tentativa nunca deixa a UI em
`Processing` (senão o botão ficaria travado se o desfecho voltasse sem motivo).

---

## ADR-010 — Navigation 3 com rotas type-safe

**Contexto:** requisito de migrar para Navigation 3 e trocar rotas por strings
por rotas type-safe.
**Decisão:** usar `androidx.navigation3` (`NavDisplay` + `entryProvider` +
`rememberNavBackStack`). Cada destino é uma chave `@Serializable` que implementa
`NavKey`; os argumentos são propriedades tipadas da chave. Navegar = `add(Key)`,
voltar = `removeLastOrNull()`.
**Consequências:** type-safety garantida pelo compilador (sem placeholders de
string nem casts de argumentos); back stack observável e testável. O scoping de
ViewModel por destino usa `koinViewModel(key=...)` porque o decorator oficial
(`lifecycle-viewmodel-navigation3`) exige AGP 9.1 (ver ADR-011).

---

## ADR-011 — Bump mínimo de toolchain para o Navigation 3

**Contexto:** o Navigation 3 estável (1.1.6) exige, na cadeia transitiva, Compose
≥ 1.9.5 e lifecycle ≥ 2.10 (compileSdk 36). As versões mais novas (lifecycle 2.11
/ Compose 1.10) exigem **AGP 9.1 / compileSdk 37**. À época, isso quebraria o
composite build do design system, que fixava AGP 8.9.1.
**Decisão:** adotar o conjunto **mínimo** compatível com AGP 8.9.1: `compileSdk 36`,
Compose **1.9.5** (BOM 2025.11.01), lifecycle **2.10.0**; **não** usar
`lifecycle-viewmodel-navigation3` (que puxaria AGP 9.1).
**Consequências:**
- O design system foi compilado com o próprio catálogo (Compose 1.7) e roda
  **forward-compatible** sobre o Compose 1.9.5 do app.
- **Revisão pendente (ADR-006):** com o DS agora consumido como AAR publicado, o
  AGP deste projeto não precisa mais casar com o da lib — a restrição que
  motivou o "mínimo" deixou de existir. Subir para AGP 9.1 / compileSdk 37 e
  adotar `lifecycle-viewmodel-navigation3` (o decorator oficial de ViewModel do
  Nav3, no lugar do `koinViewModel(key=…)`) passou a ser viável. Não foi feito
  aqui por ser um salto de toolchain sem ganho funcional para o case.
- A partir do Compose 1.9 o `material3` não puxa mais `material-icons`
  transitivamente. Como o design system usa `Icons.Filled.*` (ex.: MnsTopBar),
  foi necessário declarar `androidx.compose.material:material-icons-core`
  explicitamente nos módulos que consomem o DS (versão 1.7.8, congelada, vinda do
  BOM). **Esse crash de runtime (`NoClassDefFoundError: Icons$Filled`) foi
  capturado pelo Compose UI test** antes de chegar ao app — evidência do valor do
  teste de UI sobre o design system.
