# SPECS — Requisitos derivados do case

Requisitos extraídos do enunciado, organizados para rastreabilidade contra a
implementação.

## Funcionais
1. **Listar eventos** disponíveis (tela inicial). → `:feature:events` (EventsList)
2. **Selecionar evento + quantidade**, validando disponibilidade. → EventDetail + `MnsStepper`
3. **Iniciar e concluir pagamento** via Cielo (deeplink). → `:payment` + Checkout
4. **Registrar resultado** (APPROVED/DENIED/CANCELED) no Room. → `ReconcilePaymentUseCase` + `PurchaseRepository`
5. **Exibir comprovante** (evento, qtd, valor, authCode, NSU, bandeira, status). → Receipt (`MnsTicketCard`)
6. **QR Code** contendo o **id da compra aprovada** (requisito firme). → `MnsTicketCard(qrContent = purchase.id)` (ZXing via DS)

## Não-funcionais / técnicos
- Kotlin + Jetpack Compose.
- Clean Architecture (domain/data/presentation) + MVI (State único imutável,
  Intents, reducer, Effects one-shot).
- Koin (DI); Coroutines + Flow.
- Room (sem backend próprio, sem Firestore).
- Multi-módulo, `:domain` puro (sem Android).
- `compileSdk 34/35`, `targetSdk 34`, `minSdk 24`.
- Integração Cielo via **Deeplink** (não SDK); documentar a distinção de targetSdk.
- Design system consumido via **composite build** (`includeBuild`), pela
  coordenada Maven (para troca futura pelo artefato remoto ser transparente).
- Tokens Cielo lidos de `local.properties` → `BuildConfig` (leitura manual).
- **Não** gerar tokens; deixar variáveis prontas.

## Idempotência (crítico)
- Chave de idempotência única (UUID) por tentativa; `Purchase` PENDING antes de pagar.
- Mesma chave como `reference` do pedido Cielo.
- Bloquear reenvio enquanto PENDING (no ViewModel/reducer, não só na UI).
- Conciliação pela chave; UNIQUE no Room; callback repetido idempotente.
- Testes: duplo clique e callback repetido.

## Tratamento de erros (crítico)
- Erros como tipos de domínio (sealed), não exceptions soltas.
- Cobrir: Cielo não instalada, callback com erro, cancelamento, pagamento
  parcial, timeout/retorno inesperado, evento sem estoque.
- State do MVI reflete cada estado (Loading, Success, Error acionável, PaymentPending).

## Testes (crítico)
- Use case de compra: feliz / negado / cancelado.
- Idempotência: duplo disparo; callback duplicado.
- Mapeamento Cielo (sucesso/erro/parcial) → PaymentResult.
- ViewModel/reducer MVI (Turbine).
- Room: constraint de idempotência, persistência de status.
- ≥1 Compose UI test no fluxo seleção→pagamento.

## Entregáveis de documentação
- README rico (execução, decisões, libs, integração Cielo, trade-offs, futuro, IA).
- `/docs`: SPECS.md, ARCHITECTURE_DECISIONS.md, AI_USAGE.md.
