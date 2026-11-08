# Prompt — Balança real e venda ao peso profissional (talhos, frutarias, charcutaria)

> Colar este prompt numa sessão nova do Claude Code, na raiz do repositório Multicore.
> Uma sessão para este prompt; a produção tem prompt próprio (`PROMPT_PRODUCAO_TRANSFORMACAO.md`).

---

## Papel

És engenheiro sénior do Multicore ERP (Java 21, Spring Boot 3.2.5, Swing, reactor
`contracts` / `backend` / `desktop`). Entregas funcionalidade de nível comercial, pronta para uma
loja piloto: código, SPEC, harness, testes e UI acabada. Não entregas protótipos nem "versão básica".

## Antes de escrever código

1. Ler `CLAUDE.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `tasks/current.md`.
2. Ler o que já existe sobre balança e não o reescrever sem motivo:
   - `docs/BALANCA_PESO_VARIAVEL_SPEC.md` e `docs/BALANCA_PESO_VARIAVEL_HARNESS.md`
     (etiqueta EAN-13 de medida variável, `ScaleBarcodeParser`, `retail.scale.*`, IDs BV-xx).
   - `docs/POS_DESIGN_ECRA_SPEC.md` §Balança e `docs/POS_PRE_PILOTO_SPEC.md`.
   - `desktop/.../modules/pos/scale/SerialScaleReader.java` (hoje **só simulação**),
     `desktop/.../gui/pos/scale/PosScaleLiveWidget.java`, `POSPanel` (`handleScaleScan`,
     `addWeighedProductToCart`), `DesktopLocalSettings.posScaleEnabled()`.
   - `ProductSaleType.WEIGHT`, `Product.netUnitWeightKg`, `StockMovementType.WASTE`.
   - UI: `docs/UI_DESIGN_SYSTEM.md`, `UI_CONSISTENCIA_PROFISSIONAL_SPEC.md`,
     `UI_FEEDBACK_PROFISSIONAL_SPEC.md`, `UI_TABELAS_UX_SPEC.md`, `FORMATO_NUMERICO_SPEC.md`,
     `SISTEMA_ICONES_SPEC.md`. O ecrã de **Pedidos** é a referência visual.
3. Escrever `docs/BALANCA_REAL_SPEC.md` e `docs/BALANCA_REAL_HARNESS.md` (IDs `BR-01…`) com o
   desenho abaixo, **apresentar a SPEC e esperar aprovação** antes de implementar.
4. Perguntar ao responsável, se não estiver nos documentos: marca e modelo das balanças da loja
   piloto (Toledo/Prix, CAS, Dibal, Elgin, Bizerba…), ligação (RS-232, USB-série), e se a
   balança etiquetadora da secção imprime etiquetas com peso ou com preço.

## Estado actual (não repetir, completar)

| Já existe | Falta |
|---|---|
| Leitura da etiqueta EAN-13 com peso/preço embutido | Driver real de porta série; hoje é simulação |
| Tipo de venda `WEIGHT` e quantidades decimais até ao stock | Leitura do peso **ao vivo** no balcão do POS |
| PLU reaproveita o campo `barcode` | Campo `plu` próprio, único por empresa |
| Formato de etiqueta global em `application.properties` | Formato por empresa, configurável na UI |
| Dígito de controlo ignorado | Validação EAN-13 opcional |
| `WASTE` como tipo de movimento | Quebras com motivo, relatório de perdas por secção |

## O que entregar

### 1. Driver de balança (desktop, hardware é local)

- Dependência `com.fazecast:jSerialComm` (versão estável fixa) só no módulo `desktop`.
- Interface `ScaleProtocol` (Strategy) com implementações testáveis sem hardware:
  `ToledoPrixProtocol`, `CasContinuousProtocol` (`ST,GS,+001.234kg`), `DibalProtocol`,
  `GenericEnqProtocol` (pedido ENQ/resposta). Cada protocolo é **puro**: recebe bytes, devolve
  `ScaleReading(weightKg, stable, overload, negative, unit, rawFrame)`.
- `ScaleConnection` gere abertura, timeouts, reconexão automática com backoff e fecho limpo ao
  sair do POS. Corre numa thread própria; a UI recebe leituras por `SwingUtilities.invokeLater`.
  **Nunca** ler a porta no EDT.
- Detecção de estabilidade: flag do protocolo **e** N leituras consecutivas dentro de tolerância
  configurável (ex.: ±2 g durante 400 ms). Peso mínimo de venda configurável (ex.: 20 g).
- `SerialScaleReader` passa a delegar no driver real; a simulação fica como `SimulatedScaleProtocol`
  e só é seleccionável por `ADMIN`/`MANAGER`, com aviso visível de "modo simulação".

### 2. Configuração do posto — diálogo "Configurar Balança"

- Acesso: `ADMIN`/`MANAGER`, a partir do POS e das definições do posto.
- Campos: porta (lista detectada + actualizar), protocolo, baud, bits, paridade, stop bits,
  tolerância de estabilidade, peso mínimo, unidade.
- Painel "Testar ligação" com leitura ao vivo, estado da ligação e últimas tramas recebidas
  (hex + texto) para diagnóstico de campo.
- Persistência em `%LOCALAPPDATA%\Multicore\desktop.properties` via `DesktopLocalSettings`
  (chaves `desktop.pos.scale.*`). Nunca guardar nada disto no backend.

### 3. Backend — PLU e formato por empresa

- Migração Flyway nova (seguinte a `V111`): coluna `plu` em `product`, única por empresa,
  com paridade PostgreSQL (`docs/POSTGRES_SCHEMA_PARITY_SPEC.md`, check `postgres-parity`).
- Resolução da etiqueta: `plu` primeiro, `barcode` como fallback (compatibilidade com BV-xx).
- Entidade `ScaleLabelFormat` por empresa (prefixos aceites, dígitos de PLU e medida, peso ou
  preço, divisores, validar dígito de controlo). `retail.scale.*` passa a valor por omissão.
- Endpoints via skill `multicore-new-endpoint`, DTOs em `contracts`, `BusinessRuleException`
  para regras, sem expor entidades.
- Linha de venda regista a **origem do peso**: `SCALE_LIVE`, `SCALE_LABEL`, `MANUAL`.
  Peso manual exige permissão, motivo e fica no trilho de auditoria (`AUDIT_TRAIL_CRITICAL_EVENTS_SPEC`).

### 4. POS — experiência de pesagem ao balcão

Painel de pesagem inline (não um `JOptionPane`) que abre ao escolher um artigo `WEIGHT` pela
pesquisa ou pelas teclas rápidas:

- Peso em dígitos grandes (fonte monoespaçada, ≥ 48 pt), três casas decimais, unidade "kg".
- Chip de estado com cor e ícone `UIHelper.icon`: Estável · A estabilizar · Sobrecarga ·
  Negativo · Desligada · Simulação.
- Preço/kg, tara aplicada e total calculado ao vivo pela mesma engine (`LineCalculator`).
- Taras pré-definidas por artigo (bandeja, saco) e tara manual.
- "Confirmar peso" só activo com leitura estável e acima do peso mínimo.
- Teclado: `Enter` confirma, `Esc` cancela, `T` tara, `Z` zero, `M` peso manual (se permitido).
- **Teclas rápidas de frescos**: grelha configurável de artigos ao peso com imagem do produto
  (`Product.imageData`), nome e preço/kg; pesquisa por PLU numérico; ordenação por mais vendidos.
- Linha no carrinho: `1,235 kg × 450,00 MT/kg`, com ícone da origem do peso.
- Talão e fatura mostram kg com 3 casas e preço/kg.
- Se a balança cair a meio da venda: aviso não bloqueante, venda continua por etiqueta ou manual.

### 5. Quebras e perdas (talho e frutaria)

- Registo de quebra com motivo (desidratação, apodrecimento, aparas, devolução de cliente,
  validade), peso lido da balança e custo à média; usa `StockMovementType.WASTE` e os serviços de
  movimento existentes (`StockMovementRegistrationService`), sem caminhos paralelos.
- Relatório "Perdas por secção e motivo" (kg, MT, % das vendas) no ecrã e em PDF
  (skill `multicore-pdf-document`).

## Padrão de UI exigido

- Componentes do projecto (`ModernButton`, `ActionMenuButton`, `UIHelper.tableCardTop`,
  `UIHelper.styleTable`), tokens de tema claro/escuro, sem cores literais soltas.
- Ícones só por `UIHelper.icon(...)` (skill `multicore-icons`). Nunca emojis.
- Estados vazios com explicação e acção, carregamento com `SwingWorker`, validação inline junto
  ao campo, confirmação antes de acções irreversíveis, tooltips com o atalho de teclado.
- Textos em PT-MZ; números e moeda pelo formatador central.
- Ecrãs alinhados com Pedidos em espaçamentos, cabeçalhos e botões.
- Verificar visualmente: reconstruir e relançar o desktop (procedimento habitual com demo-login)
  e confirmar o painel de pesagem e o diálogo de configuração nos temas claro e escuro.

## Testes e harness (obrigatórios)

- Testes unitários por protocolo com tramas reais gravadas como fixtures, incluindo tramas
  partidas, lixo na linha, sobrecarga e negativo.
- Testes do detector de estabilidade (tolerância, janela temporal, ruído).
- `ScaleConnection` com porta falsa: reconexão, timeout, fecho.
- Resolução PLU → barcode, formato por empresa, dígito de controlo.
- Harness de UI: o painel não lê a porta no EDT, botões com ícone `UIHelper`, sem emojis,
  "Confirmar" desactivado sem estabilidade.
- Paridade PostgreSQL da migração.
- `mvn clean verify` verde no fim de cada fase. Compilar é a verdade, não o IDE.

## Fases

1. SPEC + HARNESS aprovados.
2. Protocolos puros e detector de estabilidade, com testes.
3. Driver real, `ScaleConnection`, diálogo de configuração.
4. Backend: `plu`, `ScaleLabelFormat`, origem do peso, auditoria.
5. Painel de pesagem e teclas rápidas no POS.
6. Quebras e relatório.
7. Validação em loja: actualizar `docs/GUIA_ACEITACAO_LOJA.md` com o roteiro de teste da balança.

## Regras de trabalho

- Não misturar camadas; injecção por construtor; DTO na fronteira.
- Fazer stage só dos ficheiros que alteraste (outras sessões partilham a branch). Não fazer
  commit nem push sem pedido explícito.
- No fecho, actualizar `tasks/current.md` (máx. 1 página) e indicar o que ficou por validar
  em hardware real.
