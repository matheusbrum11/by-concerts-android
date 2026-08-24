# By Concerts — Venda de Ingressos com Pagamento Cielo (Deeplink)

App Android nativo de venda de ingressos para eventos locais, com pagamento
presencial via **Cielo Smart / Cielo LIO** integrado por **Deeplink**. Desafio
técnico com foco em: tratamento explícito de erros, **prevenção de cobrança
duplicada (idempotência)**, testes automatizados dos cenários críticos,
organização/manutenibilidade e documentação do uso de IA na construção.

> Estado atual: o app **compila, roda e emite comprovante com QR**. A suíte de
> testes cobre os fluxos críticos (idempotência, mapeamento Cielo, MVI, Room).

---

## Sumário

- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Estrutura multi-módulo](#estrutura-multi-módulo)
- [Como executar](#como-executar)
- [Design System via composite build](#design-system-via-composite-build)
- [Integração Cielo (Deeplink)](#integração-cielo-deeplink)
- [Idempotência / prevenção de cobrança duplicada](#idempotência--prevenção-de-cobrança-duplicada)
- [Tratamento de erros](#tratamento-de-erros)
- [Testes](#testes)
- [Decisões arquiteturais](#decisões-arquiteturais)
- [Bibliotecas e justificativas](#bibliotecas-e-justificativas)
- [Trade-offs](#trade-offs)
- [O que faria com mais tempo](#o-que-faria-com-mais-tempo)
- [Uso de IA](#uso-de-ia)

---

## Stack

- **Kotlin** + **Jetpack Compose**
- **Clean Architecture** (domain / data / presentation) + **MVI** na apresentação
- **Navigation 3** (`androidx.navigation3`) — rotas **type-safe** por construção
- **Koin** (injeção de dependência)
- **Coroutines + Flow**
- **Room** (persistência local — sem backend, sem Firestore)
- **Multi-módulo** Gradle
- **JUnit + MockK + Turbine + Compose UI test (Robolectric)**
- `compileSdk 36`, `targetSdk 34`, `minSdk 24`, JVM 17
- AGP 8.9.1 / Kotlin 2.1.10 (mesmo AGP do design system → composite build preservado);
  Compose 1.9.5 (BOM 2025.11.01) e lifecycle 2.10.0, o mínimo exigido pelo Navigation 3.

### Por que targetSdk 34 (e não 29)?

A restrição de `targetSdk 29` na documentação da Cielo aparece **apenas** no
contexto da integração via **SDK embarcado** (order-manager), em que o app roda
dentro do Cielo OS (Android 10 do terminal). Esta solução usa **Deeplink**: o
app apenas dispara uma `Intent` (`lio://payment`) para o app de pagamento da
Cielo já instalado e recebe um callback — **sem embarcar SDK algum**. Logo, não
há acoplamento de versão e podemos usar `targetSdk 34`.

---

## Arquitetura

Clean Architecture com dependências apontando sempre para dentro. O `:domain` é
**Kotlin puro** (zero Android). MVI na apresentação: State único e imutável,
Intents do usuário, reducer como única via de mutação e Effects one-shot
(navegação, mensagens).

```
:app  ──▶ :feature:events ──▶ :domain ──▶ :core:common
  │       :feature:checkout ─▶ :payment ─▶ :domain
  ├──▶ :data ──▶ :domain
  └──▶ (Design System via composite build)

:domain  → puro Kotlin (nenhuma dependência Android)
:payment → adapter Cielo Deeplink, isolado atrás de PaymentGateway
:data    → Room + repositórios (troca por API remota = nova implementação)
```

**Fluxo de pagamento (ports & adapters):** a interface `PaymentGateway` vive no
`:payment`; os **modelos** trocados por ela (`PaymentRequest`, `PaymentResult`)
são de **domínio**. O `CheckoutViewModel` orquestra `gateway.pay()` →
`PaymentResult` → `ReconcilePaymentUseCase`. As telas nunca conhecem a Cielo.

---

## Estrutura multi-módulo

| Módulo | Responsabilidade |
|---|---|
| `:app` | Entrada, splash, navegação (Navigation 3 type-safe), setup do Koin, manifest com meta-data Cielo |
| `:core:common` | `AppResult`, dispatchers, `Clock`/`IdGenerator`, `coreModule` (Koin) |
| `:core:ui` | Base MVI (`MviViewModel`: State/Intent/Effect) |
| `:domain` | Entidades, use cases, interfaces de repositório, modelos de pagamento (puro Kotlin) |
| `:data` | Room (entities, DAOs, DB), mappers, repositórios, seed (`assets/events.json`) |
| `:payment` | `PaymentGateway`, `CieloDeeplinkGateway`, `PaymentResponseActivity`, codec/parser Cielo |
| `:feature:events` | Listagem e detalhe/seleção de quantidade (MVI + Compose) |
| `:feature:checkout` | Pagamento + comprovante com QR (MVI + Compose) |

---

## Navegação (Navigation 3) e splash

A navegação usa **Navigation 3** com rotas **type-safe**: cada destino é uma
chave tipada (`NavKey` `@Serializable`) empilhada num back stack observável —
os argumentos viajam como propriedades do objeto, não como strings/placeholders.

```kotlin
@Serializable data class EventDetailKey(val eventId: String) : NavKey
// navegar:  backStack.add(EventDetailKey(eventId))
// voltar:   backStack.removeLastOrNull()
```

O grafo vive em `app/.../navigation/AppNavDisplay.kt` (um `NavDisplay` + `entryProvider`).
Fluxo: **Splash → Eventos → Detalhe → Checkout → Comprovante**.

**Splash de carregamento:** a primeira rota é a `SplashKey`. A `SplashViewModel`
roda o seed do Room (idempotente) e aguarda a primeira emissão do catálogo antes
de liberar a navegação — assim a lista já entra populada. Há uma duração mínima
de exibição para a splash não "piscar" quando o seed é instantâneo.

**Scoping de ViewModel por destino:** o artefato `lifecycle-viewmodel-navigation3`
(decorator de ViewModel do Nav3) só existe a partir de 2.11.0, que exige AGP 9.1 —
incompatível com o AGP 8.9.1 do composite build. Para manter o AGP alinhado ao
design system, o scoping é feito via `koinViewModel(key = ...)` em cada rota
parametrizada (detalhe/checkout/comprovante), garantindo uma instância por argumento.

---

## Como executar

### Pré-requisitos
- Android Studio (Ladybug+) ou JDK 17
- Android SDK (o projeto usa `compileSdk 35`)
- O projeto do **design system** disponível localmente (ver seção própria)

### 1. Configurar `local.properties`
Copie `local.properties.example` para `local.properties` e ajuste:

```properties
sdk.dir=/CAMINHO/PARA/Android/sdk

# Credenciais Cielo — geradas manualmente no portal Cielo.
# Podem ficar vazias para compilar/rodar a UI; preencha para testar pagamento real.
CIELO_CLIENT_ID=
CIELO_ACCESS_TOKEN=
```

As credenciais são injetadas em `BuildConfig` do módulo `:payment` via leitura
manual de `local.properties` (independente da assinatura de
`gradleLocalProperties`, que mudou entre versões do AGP).

### 2. Rodar
```bash
./gradlew :app:assembleDebug        # gera o APK
./gradlew test                       # roda a suíte de testes
```
Ou abra no Android Studio e rode a config `app`.

### 3. Testar o pagamento (emulador Cielo)
A Cielo fornece um **APK emulador** que simula os retornos (Sucesso / Erro /
Cancelamento) sem hardware. Instale-o no mesmo device/emulador Android e o
deeplink `lio://payment` será tratado por ele. Link na documentação oficial da
Cielo (o APK não é baixado por este projeto). Segundo a doc, o emulador funciona
de forma confiável até o Android 10.

> **Validado com o emulador oficial** (v1.61.8) em um AVD Android 10: os três
> cenários (Sucesso / Cancelado / Erro), o retry após desfecho terminal e a
> baixa de estoque apenas na aprovação. Ver `docs/AI_USAGE.md`.

**Sem o emulador Cielo**, dá para exercitar o retorno injetando o callback via
`adb` — o app trata exatamente o mesmo payload:

```bash
adb shell am start -a android.intent.action.VIEW -d "order://payment?response=<BASE64_DA_ORDER>&responsecode=0"
```

Onde `<BASE64_DA_ORDER>` é o Base64 (percent-encoded) de uma Order de sucesso
cujo `reference` seja o `idempotencyKey` da compra PENDING. Sem o app da Cielo
instalado, tocar em "Pagar" exibe o erro tratado *"App de pagamento Cielo não
encontrado"* e mantém o botão disponível para retentativa.

---

## Design System via composite build

O design system (`mns-design-system`) é um **build Gradle independente**, ainda
não publicado no Maven. Ele é consumido **localmente via composite build**
(`includeBuild`), não via `include(":modulo")`.

`settings.gradle.kts`:
```kotlin
includeBuild("/Users/matheusbrum/StudioProjects/mns-design-system") {
    dependencySubstitution {
        substitute(module("io.github.matheusbrum:mns-design-system"))
            .using(project(":design_system"))
    }
}
```

A dependência é declarada pela **coordenada Maven real** da lib (confirmada no
`gradle.properties` dela): `io.github.matheusbrum:mns-design-system` — e **não**
`com.mns.designsystem:core` (a coordenada do enunciado era um exemplo).

> **Substituição explícita:** o Gradle deriva a coordenada de um projeto incluído
> do seu **nome** (`:design_system`), não do `artifactId` de publicação
> (`mns-design-system`). Por isso a substituição automática falha e usamos
> `dependencySubstitution` explícita. É o mesmo `group:artifact` que o artefato
> terá no Maven.

**Futuro (Maven):** ao publicar a lib, basta **remover o bloco `includeBuild`**;
a mesma dependência do catálogo passará a resolver o artefato remoto, sem tocar
no código de consumo das features.

Todo consumo visual passa pelo design system (`MnsTheme`, `MnsButton`,
`MnsScaffold`, `MnsStepper`, `MnsTicketCard`, `MnsQrCode`, `MnsCurrencyFormatter`…),
nunca Material cru. O **QR Code do ingresso** é gerado pelo componente
`MnsTicketCard`/`MnsQrCode` do próprio DS (que usa ZXing internamente).

### Coordenadas Maven finais (a preencher pós-publicação)
- Repositório público do artefato: _(a registrar)_
- Coordenadas Maven finais: `io.github.matheusbrum:mns-design-system:<versão>`

---

## Integração Cielo (Deeplink)

Modelo escolhido: **Deeplink** (recomendação oficial atual da Cielo; o SDK foi
descontinuado e o WebView não é permitido na nova Cielo Smart).

A implementação segue o contrato dos **samples oficiais** referenciados pela
documentação da Cielo ([Deep Link: exemplo de código][doc-deeplink]):
[LIO-Hybrid-Integration-Sample-Flutter][sample-flutter] (org DeveloperCielo) e
[cielo_sample][sample-rn] (React Native).

[doc-deeplink]: https://docs.cielo.com.br/cielo-smart/docs/deep-link-exemplo-de-codigo
[sample-flutter]: https://github.com/DeveloperCielo/LIO-Hybrid-Integration-Sample-Flutter
[sample-rn]: https://github.com/matheus-caldeira/cielo_sample

**Requisitos no manifest:**
- Permissão `INTERNET`
- `meta-data cs_integration_type=uri`
- `PaymentResponseActivity` exportada, com `BROWSABLE`, para `order://payment`
  (host do sample oficial) e `order://response`
- **`<queries>` com o scheme `lio`** — sem isso, no Android 11+ (targetSdk 30+)
  a visibilidade de pacotes faz `resolveActivity` devolver null mesmo com o app
  da Cielo instalado, gerando falso "Cielo não instalada"

**Requisição** (`CieloRequestCodec`) — campos conforme o sample oficial:

```json
{
  "accessToken": "...", "clientID": "...", "reference": "<chave de idempotência>",
  "email": "", "installments": 0, "merchantCode": "",
  "paymentCode": "CREDITO_AVISTA", "value": 24000,
  "items": [{ "name": "Rock na Praça", "quantity": 2, "sku": "evt-1",
              "unitOfMeasure": "unidade", "unitPrice": 12000 }]
}
```

- `clientID` com **D maiúsculo** (não é typo da doc).
- `value`/`unitPrice` são **numéricos, em centavos** (R$ 120,00 = `12000`).
- O JSON vira **Base64 `NO_WRAP`** e a URI é montada com **`Uri.Builder`**, não
  por concatenação: o Base64 contém `+`, `/` e `=`, que precisam ser
  percent-encoded no query param — concatenar faria o `+` chegar como espaço e
  corromper o payload (há teste de regressão para isso).

Resultado: `lio://payment?request=<base64>&urlCallback=order://payment`.

**Resposta** (`CieloResponseParser`):

| Caso | Formato |
|---|---|
| Sucesso | A **Order vem na raiz**: `id`, `reference`, `status`, `paidAmount`, `pendingAmount`, `price`, `items[]`, `payments[]` |
| Erro/cancelamento | Envelope `{ "code": int, "reason": string, "order": {...} }` — `code 1` = cancelado pelo usuário |

- **A distinção sucesso/erro é ESTRUTURAL** (presença de `code`/`reason`), e
  deliberadamente **não** usa o query param `responsecode`. O sample oficial
  decide por ele, mas isso é incorreto: verificado contra o emulador oficial
  (v1.61.8), o **cancelamento também chega com `responsecode=0`** — confiar
  nesse parâmetro fazia um cancelamento ser lido como sucesso.
- O Base64 chega com **`\n` embutido na própria URI** (o app da Cielo codifica
  com `Base64.DEFAULT`); daí a sanitização antes do decode.
- `payments[]` traz `authCode`, `cieloCode` (NSU), `brand`, `mask`, `amount`.
- **`paymentFields.statusCode` é STRING** (`"1"`), não número — no contrato da
  Cielo todos os `paymentFields` são string. (0 = PIX, 1 = autorizada, 2 = cancelamento.)
- O Base64 é sanitizado (remoção de `\n`) antes do decode, como no sample.
- **Pagamento parcial:** `pendingAmount != 0` **não** é aprovado — vira
  `PaymentError.PartialPayment`.
- O **`reference` volta na resposta** — é ele que amarra o retorno à compra local.

**Cancelamento/estorno:** `lio://payment-reversal` com `{ id, clientID,
accessToken, cieloCode, authCode, value }`, implementado em `PaymentGateway.cancel`.

### Resiliência: o callback é conciliado de forma persistente

A documentação alerta que "o retorno da transação pode falhar se o terminal for
desligado antes de o app de pagamento voltar ao primeiro plano" e recomenda
foreground service. Aqui o problema é resolvido pela **persistência**, não por
estado em memória:

`PaymentCallbackHandler` faz *parse → concilia pelo `reference` → persiste →
publica*. O `await` do gateway é apenas o atalho do caminho feliz; a fonte de
verdade é o Room, e o `CheckoutViewModel` **observa a compra do banco**.

Consequência: se o processo do app for morto enquanto o app da Cielo está em
primeiro plano, o callback reinicia o processo, concilia e persiste do mesmo
jeito — a compra nunca fica órfã em PENDING. *(Verificado no emulador: processo
encerrado com `am force-stop`, callback entregue, compra `PENDING → APPROVED`.)*

---

## Idempotência / prevenção de cobrança duplicada

Prevenção em **duas camadas**:

1. **Guard de estado (MVI):** enquanto o pagamento está `Processing`, novos
   cliques em "Pagar" são ignorados no `CheckoutViewModel` — o botão não
   redispara a intent. (Protegido no reducer, não só na UI.)
2. **Persistência (use cases + Room):**
   - Antes de pagar, cria-se uma `Purchase` **PENDING** com uma **chave de
     idempotência** (UUID), usada como `reference` do pedido Cielo.
   - A chave tem **constraint UNIQUE** no Room (`PurchaseEntity`).
   - `createPendingIfAbsent` devolve a compra existente em caso de conflito.
   - `ReconcilePaymentUseCase` trata **callback repetido** como **no-op** quando
     a compra já está em estado terminal (APPROVED/DENIED/CANCELED) — sem baixar
     estoque duas vezes nem sobrescrever a aprovação.
   - Erros transitórios/parciais mantêm a compra **PENDING**, permitindo retry
     com a **mesma chave** — nunca criando outra cobrança.

---

## Tratamento de erros

Erros são **tipos de domínio (sealed)**, não exceptions soltas:
`DomainError`, `PaymentResult` (Approved/Denied/Canceled/Error) e `PaymentError`
(GatewayNotAvailable, InvalidResponse, PartialPayment, Timeout, Unknown).

Cobertos: Cielo não instalada (intent falha), callback com erro, cancelamento,
pagamento parcial, timeout, retorno inesperado, evento sem estoque. O State do
MVI reflete cada caso (Loading, Approved, Failed, PendingRetry, mensagens
acionáveis via `MnsAlert`).

---

## Testes

```bash
./gradlew test
```

- **Use case de compra:** caminho feliz, negado, cancelado, erro transitório
  (`ReconcilePaymentUseCaseTest`, `CreatePendingPurchaseUseCaseTest`).
- **Idempotência:** duplo clique não gera duas compras (`CheckoutViewModelTest`);
  callback duplicado é no-op (`ReconcilePaymentUseCaseTest`); UNIQUE no Room
  (`PurchaseRepositoryImplTest`).
- **Mapeamento Cielo → PaymentResult:** sucesso/PIX/parcial/erro/cancelamento/
  inválido (`CieloResponseParserTest`).
- **Gateway Deeplink:** app ausente (não instalado / disparo falho), timeout,
  callback via barramento (`CieloDeeplinkGatewayTest`).
- **Codec Cielo:** campos do contrato oficial, valores em centavos, URI de
  estorno e **regressão de percent-encoding do Base64** (`CieloRequestCodecTest`).
- **Callback resiliente:** conciliação pelo `reference`, publicação no
  barramento e callback duplicado (`PaymentCallbackHandlerTest`).
- **MVI (Turbine):** transições de State e Effects (`CheckoutViewModelTest`,
  `EventsViewModelTest`).
- **Room:** UNIQUE + persistência de status (`PurchaseRepositoryImplTest`).
- **Compose UI (Robolectric):** fluxo seleção→pagamento (`EventDetailScreenTest`).

---

## Decisões arquiteturais

Ver [`docs/ARCHITECTURE_DECISIONS.md`](docs/ARCHITECTURE_DECISIONS.md) para os ADRs.
Resumo:

- **Clean + MVI:** separação de responsabilidades, testabilidade e um estado
  previsível na UI.
- **Multi-módulo:** fronteiras explícitas, build incremental, `:domain` puro.
- **Room (não backend/Firestore):** o case não avalia backend; Room mantém o
  projeto executável sem credenciais externas e é a camada natural da idempotência.
- **Deeplink (não SDK):** recomendação oficial da Cielo, mais leve, sem lib de
  terceiros, compatível com a nova Cielo Smart — e sem a restrição de targetSdk 29.
- **`PaymentGateway` no `:payment`, modelos no `:domain`:** honra "interface no
  `:payment`" sem quebrar "domain puro"; a orquestração fica no ViewModel.

---

## Bibliotecas e justificativas

| Lib | Por quê |
|---|---|
| **Koin** | DI leve, Kotlin-first, sem geração de código/KAPT |
| **Room** | Persistência local + constraint UNIQUE para idempotência |
| **kotlinx.serialization** | JSON do payload Cielo e do seed, sem reflection |
| **Turbine** | Asserção ergonômica sobre Flows/StateFlow nos testes MVI |
| **MockK** | Mocks idiomáticos de Kotlin (coEvery/coVerify) |
| **Robolectric** | `android.util.Base64`, Room in-memory e Compose test na JVM |
| **ZXing** | Geração do QR — via componente do design system (dep. transitiva) |

---

## Trade-offs

- **Barramento de callback single-slot:** assume no máximo uma tentativa PENDING
  por vez (garantido pelo guard de idempotência). Simples e testável; um cenário
  multi-pagamento simultâneo exigiria roteamento por `reference`.
- **Estoque decrementado na conciliação** (não reservado no PENDING): evita
  travar estoque em tentativas abandonadas, ao custo de aceitar uma checagem no
  momento da aprovação.
- **Interface `PaymentGateway` no `:payment`** (seguindo o enunciado) em vez de
  um port no `:domain`: exigiu manter a orquestração no ViewModel para preservar
  o `:domain` puro.

---

## O que faria com mais tempo

- Implementação alternativa de `PaymentGateway` via SDK legado (o design já
  suporta, é só outra implementação da interface).
- Repositório remoto (nova implementação de `EventRepository`).
- Cancelamento/estorno parcial completo no fluxo de UI.
- Mais `paymentCode` (parcelado, PIX dedicado, voucher).
- Publicação do design system no Maven e remoção do `includeBuild`.
- Testes instrumentados de ponta a ponta com o emulador Cielo.

---

## Uso de IA

A construção usou **Claude Code (Opus)** como harness de IA, sob restrições
explícitas do desenvolvedor. O registro honesto e rastreável está em
[`docs/AI_USAGE.md`](docs/AI_USAGE.md). Specs derivadas do case em
[`docs/SPECS.md`](docs/SPECS.md).

---

## Screenshots

_(placeholder — adicionar capturas da listagem, detalhe/seleção, checkout e
comprovante com QR.)_
