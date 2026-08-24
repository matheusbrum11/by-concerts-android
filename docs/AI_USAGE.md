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

- `:app:assembleDebug` verde (app compila e empacota).
- Suíte de testes verde cobrindo: idempotência (duplo clique + callback repetido
  + UNIQUE), mapeamento Cielo (sucesso/PIX/parcial/erro/cancelamento/inválido),
  transições MVI (Turbine), Room in-memory e um Compose UI test (Robolectric).

### Integração contínua como verificação independente

O ponto mais frágil de um projeto escrito com auxílio de IA é a possibilidade de
o código "parecer certo" e passar despercebido em leitura. A malha de defesa
contra isso não é a revisão da própria IA — é uma verificação que **não depende
dela nem da máquina em que ela rodou**.

Por isso o `.github/workflows/ci.yml` executa `assembleDebug` + `test` em todo
pull request para `main`. O que essa esteira acrescenta, concretamente:

- **Tira a validação da máquina do autor.** Todo "está verde" registrado neste
  documento foi obtido localmente. O CI reexecuta em runner limpo, sem
  `local.properties`, sem credenciais Cielo, sem SDK pré-configurado, sem cache
  quente — o que transforma "compila aqui" em "compila em qualquer lugar".
- **Torna a suíte obrigatória, não decorativa.** Os testes deixam de ser um
  anexo do PR e passam a ser condição para o merge.
- **Fecha o ciclo que este documento descreve.** Vários bugs relatados aqui
  foram encontrados pelos testes (retry após desfecho terminal, fase presa em
  `Processing`, `NoClassDefFoundError: Icons$Filled`). O CI garante que essas
  regressões continuem sendo pegas depois que a sessão com a IA terminar.

Vale registrar a honestidade do escopo: a esteira **não** roda testes
instrumentados nem gate de cobertura. Isso é deliberado — a suíte inteira já roda
na JVM (Compose test via Robolectric), então o CI é rápido e não flaky; e CI não
está entre os critérios de avaliação do case, então não faria sentido investir
além do que efetivamente protege o código.

## Refatoração posterior: Navigation 3 + splash + visual

Segunda rodada de trabalho, com a mesma disciplina (verificar o ambiente, bumpar
o mínimo, compilar/testar/rodar a cada passo):

- **Descoberta de versões antes de codar.** A IA consultou o Google Maven
  (maven-metadata / POMs) para achar as versões reais do Navigation 3 e suas
  dependências transitivas, em vez de chutar.
- **Restrição preservada: não quebrar o composite build.** Ao descobrir que
  lifecycle 2.11 / Compose 1.10 exigem **AGP 9.1 / compileSdk 37** (o que
  quebraria o design system fixado em AGP 8.9.1), a IA recuou para o conjunto
  mínimo viável (Compose 1.9.5, lifecycle 2.10, compileSdk 36) — documentado no
  ADR-011.
- **Trade-off explicitado.** Sem o `lifecycle-viewmodel-navigation3` (que puxaria
  AGP 9.1), o scoping de ViewModel por destino foi feito com `koinViewModel(key)`.
- **Bug de runtime capturado por teste.** O bump do Compose expôs um
  `NoClassDefFoundError: Icons$Filled` (o material3 1.9 não puxa mais
  material-icons). O **Compose UI test sob Robolectric falhou e revelou o crash
  antes do app** — a correção foi declarar `material-icons-core` nos consumidores
  do DS.
- **Verificação visual no emulador.** A IA instalou o app no emulador e capturou
  screenshots confirmando: splash (fundo primary, texto/spinner em branco), header
  colorido na lista de eventos e a navegação type-safe (lista → detalhe com chave
  tipada). Ao notar o título da splash em preto (o `MnsHeading` ignora
  `contentColor`), trocou por `MnsText` com `onPrimary` e reconfirmou por screenshot.

## Terceira rodada: aderência à documentação oficial do deeplink

Pedido: "visite a documentação da integração via deep-link e siga ela à risca".

- **Leitura da fonte primária.** A IA acessou a doc, constatou que a página do
  exemplo de código **não contém o payload** (só aponta para dois repositórios
  de exemplo) e foi buscar o contrato nos samples oficiais — inclusive o da org
  DeveloperCielo. Ou seja: não assumiu o exemplo resumido do enunciado como
  verdade.
- **Correções que a leitura da doc provocou** (detalhadas no ADR-013): Order na
  raiz no sucesso, envelope de erro com `order`, `responsecode` como
  discriminador, `statusCode` como String, `value` numérico, `merchantCode`,
  `<queries>` do Android 11+ e host de callback `payment`.
- **Bug real encontrado por leitura de código-fonte alheio.** O sample oficial
  monta a URI com `Uri.Builder`; a implementação anterior concatenava strings.
  Como o Base64 contém `+`, `/` e `=`, o payload chegaria corrompido ao app da
  Cielo (o `+` vira espaço). Corrigido, com teste de regressão.
- **Melhoria arquitetural derivada de um alerta da doc.** A doc menciona que o
  retorno pode falhar se o app for encerrado durante a transação. Em vez de
  copiar a sugestão (foreground service), a IA propôs conciliar o callback de
  forma **persistente** pelo `reference` — e isso foi validado matando o
  processo do app e entregando o callback (ADR-012).
- **Verificação empírica, não declaratória.** Em vez de afirmar "está
  funcionando", a IA exercitou o fluxo no emulador via `adb`, inspecionando o
  banco Room a cada passo:
  1. Sem o app da Cielo → erro tratado "App de pagamento Cielo não encontrado".
  2. Callback de sucesso simulado com o `reference` real lido do banco →
     `APPROVED` com authCode/NSU/bandeira/máscara e estoque 120 → 119.
  3. **Mesmo callback 3×** → continua 1 compra, estoque continua 119.
  4. **Processo morto + callback** → `PENDING → APPROVED`, estoque 60 → 59.

## Quarta rodada: teste real contra o emulador oficial da Cielo

Ambiente montado: `cmdline-tools` + system image Android 10, AVD `Cielo_API_29`,
emulador oficial da Cielo (`br.com.cielosmart.orderservice` v1.61.8) instalado
ao lado do app. Antes de instalar, a IA **verificou o APK** (package, label,
schemes declarados, sha256) em vez de confiar no download.

Resolução de intents confirmada nos dois sentidos:
`lio://payment` → app da Cielo; `order://payment` → nosso `PaymentResponseActivity`.

**Três bugs reais encontrados — nenhum deles apareceria sem o app de verdade:**

1. **Cancelamento lido como erro** (ADR-014). O sample oficial usa o query param
   `responsecode` para decidir sucesso/falha; o emulador manda `responsecode=0`
   **também no cancelamento**. Diagnóstico feito capturando a URI real via
   logcat e decodificando o Base64 — não por suposição. Corrigido para
   discriminação estrutural, com teste de regressão usando a URI real.
2. **Usuário preso após desfecho terminal** (ADR-015). O retry reusava a compra
   cancelada, e a conciliação idempotente virava no-op. Corrigido para reusar a
   chave só enquanto PENDING.
3. **Botão travado em `Processing`.** Descoberto pelo próprio teste escrito para
   o bug 2: se a tentativa terminasse PENDING sem motivo, a fase nunca saía de
   `Processing`. Separou-se "fim de tentativa" de "observação do banco".

Vale registrar que os bugs 2 e 3 foram encontrados **pelos testes**, e o bug 1
**pelo device** — os dois níveis pegaram coisas diferentes.

**Cenários exercitados de ponta a ponta** (app → Cielo → callback → Room → UI):

| Cenário | Resultado |
|---|---|
| Sucesso (2 ingressos, R$ 240,00) | Cielo exibiu "R$ 240,00 / Crédito / Vista"; comprovante com NSU e autorização reais; estoque 120 → 118 |
| Cancelado | `CANCELED`, "Cancelado pelo usuário", estoque intacto |
| Erro | `Denied(code=2, "Falha no processo de pagamento")` |
| Retry após terminal | Nova chave, `APPROVED`, baixa de estoque só na aprovação |

Confirmou-se também que o **`reference` volta no callback de sucesso** do app
real — a premissa em que a conciliação idempotente se apoia. Nos retornos de
erro/cancelamento ele não vem (o envelope não traz `order`), o que justifica
manter também a conciliação pelo ViewModel.

## Limitações conhecidas

- O fluxo foi validado com o **emulador oficial da Cielo** em AVD Android 10,
  cobrindo Sucesso / Cancelado / Erro. Falta apenas o teste em **hardware real**
  (maquininha Cielo Smart), que não é possível neste ambiente — e onde o
  emulador, segundo a doc, não deve ser instalado.
- Pagamento **parcial** (`pendingAmount != 0`) não é oferecido pelo emulador
  entre os cenários simuláveis; está coberto por teste unitário com o payload do
  contrato, não por teste de device.
- Screenshots ainda não capturadas (placeholder no README).
