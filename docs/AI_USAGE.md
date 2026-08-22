# AI_USAGE — Como a IA foi usada

Registro honesto e rastreável do uso de IA na construção desta solução, conforme
exigido pelo case.

## Ferramenta / harness

- **Claude Code (modelo Opus)** como agente de codificação, operando no
  repositório com acesso a shell (Gradle), leitura/escrita de arquivos e
  inspeção do projeto do design system.
- O desenvolvedor atuou como **revisor e decisor**: forneceu o case como spec
  detalhada, impôs restrições e validou/ajustou a saída.

## Método de trabalho (o que efetivamente aconteceu)

1. **Gate de planejamento antes de código.** O primeiro passo pedido foi
   *confirmar o plano de módulos e a estrutura de pastas antes de gerar código*.
   A IA inspecionou o ambiente, propôs a estrutura e só então implementou.
2. **Verificação de fatos do ambiente, não suposição.** A IA leu o
   `build.gradle.kts` e o `gradle.properties` reais do `mns-design-system` para
   confirmar as **coordenadas Maven** (`io.github.matheusbrum:mns-design-system`)
   e a toolchain (AGP 8.9.1, Kotlin 2.1.10, Compose BOM 2024.12.01, SDK 24/35),
   em vez de usar o exemplo `com.mns.designsystem:core` do enunciado.
3. **Construção por camadas, com compilação a cada etapa.** Após cada camada
   (setup → domain → data → payment → features → wiring → testes), a IA rodou
   `./gradlew` para compilar/testar antes de seguir. Ex.: `:app:assembleDebug`,
   `:domain:test`, etc.
4. **Descoberta guiando a implementação.** Ao inspecionar o DS, a IA achou
   `MnsQrCode`/`MnsTicketCard`/`MnsStepper`/`MnsCurrencyFormatter` e ajustou o
   plano para cumprir o requisito de QR **pelo componente do DS** (ZXing
   transitivo), sem adicionar ZXing nas features.

## Restrições impostas à IA

- **Não gerar tokens Cielo.** Apenas deixar as variáveis prontas
  (`local.properties` → `BuildConfig`), com leitura manual via `Properties`.
- **`:domain` puro** (sem Android) — verificado pela ausência de dependências
  Android nos módulos `:domain`/`:core:common`.
- **Todo visual pelo design system** (sem Material cru nas features).
- **Composite build** pela coordenada Maven (troca futura pelo artefato remoto
  deve ser quase transparente).
- **Não baixar o APK emulador da Cielo** — apenas referenciar na doc.
- Idempotência e tratamento de erros como critérios de primeira classe.

## Decisões da IA aceitas

- Estrutura multi-módulo proposta (com adição de `:core:ui` para a base MVI e
  remoção do placeholder `:core:designsystem`, já que o DS real está acessível).
- Modelagem de erros como sealed (`DomainError`, `PaymentResult`, `PaymentError`).
- Idempotência em duas camadas (guard de estado no VM + UNIQUE/no-op no domínio).
- Barramento de callback single-slot para o round-trip do deeplink.

## Decisões em que a IA foi corrigida/redirecionada pelo desenvolvedor

- **Local da interface `PaymentGateway`.** A IA recomendou um *port* no `:domain`
  (ports & adapters). O desenvolvedor optou por manter a **interface no
  `:payment`**, seguindo o enunciado ao pé da letra. A IA então reconciliou:
  interface no `:payment`, **modelos no `:domain`**, orquestração no ViewModel —
  preservando o `:domain` puro. (Ver ADR-005.)
- **QR Code.** Confirmada a opção de usar o componente do DS (`MnsQrCode`) em vez
  de ZXing direto nas features.

## Saídas da IA rejeitadas/ajustadas durante a implementação

- **Substituição automática do composite build.** A primeira tentativa
  (`includeBuild` sem `dependencySubstitution`) **falhou na compilação**
  (`Could not find io.github.matheusbrum:mns-design-system`). A IA diagnosticou a
  causa (Gradle usa o nome do projeto incluído, `:design_system`) e corrigiu com
  substituição explícita. Documentado no README e no ADR-006.
- **Import deprecado do `viewModel` DSL do Koin 4** — ajustado para
  `org.koin.core.module.dsl.*` após warning de compilação.
- **`Json` duplicado no grafo Koin** — centralizado em `coreModule` para evitar
  definição duplicada entre `:data` e `:payment`.

## Validação (o que dá confiança na saída da IA)

- `:app:assembleDebug` verde (app compila e empacota, consumindo o DS via
  composite build).
- Suíte de testes verde cobrindo: idempotência (duplo clique + callback repetido
  + UNIQUE), mapeamento Cielo (sucesso/PIX/parcial/erro/cancelamento/inválido),
  transições MVI (Turbine), Room in-memory e um Compose UI test (Robolectric).

## Limitações conhecidas

- Fluxo de pagamento real não exercitado com o emulador Cielo neste registro
  (requer o APK instalado no device) — os retornos foram cobertos por testes de
  mapeamento.
- Screenshots ainda não capturadas (placeholder no README).
