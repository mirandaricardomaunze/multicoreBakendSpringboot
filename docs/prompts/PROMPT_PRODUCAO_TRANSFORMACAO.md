# Prompt — Centro de Produção profissional (padaria, fábrica de blocos, embalamento, desmancha)

> Colar este prompt numa sessão nova do Claude Code, na raiz do repositório Multicore.
> Depende do driver de balança de `PROMPT_BALANCA_VENDA_PESO.md` apenas na fase "Chão de Fábrica";
> o resto é independente.

---

## Papel

És engenheiro sénior do Multicore ERP (Java 21, Spring Boot 3.2.5, Swing, reactor
`contracts` / `backend` / `desktop`). Transformas o módulo de produção actual num **Centro de
Produção** de nível comercial para pequena indústria moçambicana: padarias e pastelarias,
fábricas de blocos e pavê, reembalamento (arroz, açúcar, feijão), talhos que fazem desmancha.
Entregas código, SPEC, harness, testes, relatórios e UI acabada. Nada de "versão básica".

## Antes de escrever código

1. Ler `CLAUDE.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `MOVIMENTOS_COMERCIAIS.md`,
   `tasks/current.md`.
2. Ler o estado actual:
   - Backend `modules/manufacturing`: `BillOfMaterials` (baseQuantity, `BomType`
     STANDARD/DISASSEMBLY/KIT, custos estimados), `BomItem` (quantidade, UoM em texto livre,
     scrap %), `ProductionOrder` / `ProductionOrderLine`, `ProductionOrderLifecycleService`,
     endpoints `start` / `complete` / `cancel`.
   - Enums em `contracts`: `ProductionOrderStatus` (DRAFT, PLANNED, IN_PROGRESS, COMPLETED,
     CANCELLED), `BomType`.
   - Desktop: `ManufacturingPanel` (uma tabela de ordens), `BomFormDialog`,
     `ProductionOrderOperationsCoordinator`, `ProductionApiClient`.
   - Specs: `MANUFACTURING_PANEL_DECOMPOSITION_SPEC.md`, `PRODUCTION_SERVICE_DECOMPOSITION_SPEC.md`,
     `PRODUTO_PERECIVEL_VALIDADE_SPEC.md`, `PRODUCT_PACKAGING_COMPOSITION_SPEC.md`,
     `ARMAZEM_PROFISSIONAL_SPEC.md`, `BLOQUEIO_STOCK_SPEC.md`.
   - Stock: `ProductBatch`, `StockMovementRegistrationService`, `StockMovementType`
     (inclui `WASTE`), reservas usadas pelas encomendas (`SaleStockIssuer`).
   - Contabilidade: serviço de lançamentos automáticos (procurar `AutomaticPosting`).
   - UI: `docs/UI_DESIGN_SYSTEM.md`, `UI_CONSISTENCIA_PROFISSIONAL_SPEC.md`,
     `UI_TABELAS_UX_SPEC.md`, `UI_FEEDBACK_PROFISSIONAL_SPEC.md`, `FORMATO_NUMERICO_SPEC.md`.
     O ecrã de **Pedidos** é a referência visual.
3. Escrever `docs/CENTRO_PRODUCAO_SPEC.md` e `docs/CENTRO_PRODUCAO_HARNESS.md` (IDs `CP-01…`),
   **apresentar e esperar aprovação**. É uma alteração arquitectural grande: confirmar que está
   alinhada com a iteração em `tasks/current.md`.
4. Perguntar ao responsável o que não estiver nos documentos: contas contabilísticas de produção
   em curso e de desvios, se o custo de mão-de-obra é por hora ou fixo por lote, e se a loja piloto
   é padaria, blocos ou reembalamento (define a ordem das fases).

## Conceitos a modelar

### 1. Ficha técnica versionada (receita)

- Versões com uma activa; histórico imutável; ordens guardam a versão usada.
- **Rendimento**: entrada → saída com unidades diferentes (25 kg farinha → 480 pães;
  1 saco cimento 50 kg + 0,12 m³ areia → 38 blocos).
- **Unidades de medida reais**: substituir o `unitOfMeasure` em texto por entidade `UnitOfMeasure`
  e tabela de conversão (kg/g, L/ml, un, saco, m³), com conversão por produto quando necessário.
- Componentes por tipo: matéria-prima, embalagem, consumível. Percentagem de desperdício por linha.
- **Co-produtos e subprodutos** (desmancha de carcaça em cortes, soro, aparas) com regra de
  repartição de custo por valor de venda ou por peso.
- Passos de fabrico opcionais (amassar, levedar, forno; misturar, prensar, curar) com duração,
  usados no plano e na folha de produção.
- Validade da saída em dias → gera a data de validade do lote.
- Simulador na própria ficha: "para N unidades preciso de…", custo teórico por unidade e
  margem sobre o preço de venda actual.

### 2. Ordem de produção com ciclo completo

Estados: `DRAFT → PLANNED → RELEASED → IN_PROGRESS → (PARTIALLY_COMPLETED) → COMPLETED → CLOSED`
e `CANCELLED`. Actualizar o enum em `contracts` e a migração.

- `RELEASED` reserva as matérias-primas pelo mesmo mecanismo das encomendas; faltas visíveis.
- **Apontamentos parciais**: várias declarações de produção por ordem (por turno ou fornada),
  cada uma com quantidade boa, refugo e motivo, operador e hora.
- Consumo real: baixa automática pela ficha (backflush) **ou** consumo manual pesado/contado;
  mostrar teórico vs. real e desvio.
- Lotes: consumo de matérias-primas por FEFO; criação do lote de saída com validade;
  **rastreabilidade** lote de entrada → lote de saída (genealogia nos dois sentidos).
- **Estado de cura/quarentena** (blocos, produtos com controlo de qualidade): a produção entra num
  armazém ou estado bloqueado e só fica vendável após X dias ou aprovação de qualidade
  (reaproveitar `BLOQUEIO_STOCK_SPEC`).
- Fecho com desvio acima de um limiar configurável exige aprovação (módulo `approvals`; quem pede
  não aprova, excepto ADMIN).
- Concorrência: `@Version`, idempotência nos apontamentos, nada de stock negativo fora da política.
  Todos os movimentos passam pelos serviços de stock existentes.

### 3. Custeio

- Custo real = materiais consumidos ao custo médio + mão-de-obra + gastos gerais.
- Actualiza o custo médio do produto acabado; desvio de quantidade e de preço por ordem.
- Lançamentos contabilísticos automáticos (produção em curso, entrada de acabado, desvios),
  pelo serviço existente, depois de o responsável confirmar as contas.

### 4. Plano de produção do dia (caso padaria)

- Sugestão por artigo = média de vendas do mesmo dia da semana nas últimas N semanas
  + encomendas confirmadas − stock disponível − produção já planeada.
- Gerente ajusta e gera todas as ordens de uma vez.
- Verificação de matérias-primas para o plano inteiro; faltas geram proposta de compra.

### 5. Reembalamento e etiquetas

- Transformação de granel em embalagens (saco de 50 kg em 50 pacotes de 1 kg, mais o material de
  embalagem), com a quebra registada.
- Etiquetas de produto com código de barras, lote e validade, incluindo EAN-13 de peso variável
  compatível com `ScaleBarcodeParser` para pré-embalados ao peso. Reaproveitar o serviço de
  impressão de etiquetas existente.

### 6. Relatórios PDF (skill `multicore-pdf-document`)

- Folha de produção para o chão de fábrica: receita escalada à quantidade, passos e lotes a consumir.
- Rendimento e desperdício por período, produto e operador.
- Rastreabilidade de lote (para recolhas).
- Custo real vs. teórico por ordem.

## UI profissional exigida — "Centro de Produção"

Substitui o painel de tabela única por um espaço de trabalho com secções. `ManufacturingPanel`
fica como coordenador fino; cada secção é uma classe própria (SRP, ver as specs de decomposição).

1. **Hoje**: cartões de indicadores (ordens em curso, produzido vs. planeado, rendimento médio,
   desperdício %, faltas de matéria-prima, lotes em cura) e lista "precisa de atenção" com acção
   directa em cada item.
2. **Plano**: grelha por artigo com sugestão, ajuste editável, faltas destacadas, botão
   "Gerar ordens".
3. **Ordens**: tabela com chips de estado coloridos, barra de progresso planeado/produzido,
   filtros por estado, data e linha de produção; vista alternativa em colunas por estado.
4. **Detalhe da ordem**: cabeçalho com indicador de etapas do ciclo, progresso, custo actual;
   separadores Materiais (teórico vs. real, desvio a cores), Apontamentos, Lotes gerados,
   Custos, Histórico e auditoria. As acções seguem o estado (só se mostra o que é possível agora).
5. **Fichas técnicas**: lista à esquerda e editor à direita; tabela de componentes com pesquisa de
   produto, conversão de unidades visível, custo e rendimento recalculados ao vivo, comparação
   entre versões.
6. **Lotes e rastreabilidade**: pesquisa por lote e árvore de genealogia expansível.
7. **Modo Chão de Fábrica**: ecrã de ecrã inteiro para tablet ou ecrã táctil. Botões grandes,
   números grandes, quase sem escrita. O operador escolhe a ordem, declara a quantidade boa e o
   refugo, e pesa os consumos na balança quando houver (driver do prompt da balança).

Regras de acabamento:

- Componentes do projecto (`ModernButton`, `ActionMenuButton`, `UIHelper.tableCardTop`,
  `UIHelper.styleTable`), tokens de tema claro/escuro, sem cores literais.
- Ícones só por `UIHelper.icon(...)` (skill `multicore-icons`), nunca emojis.
- Estados vazios com explicação e botão de acção; carregamento com `SwingWorker`, nunca bloquear
  o EDT; validação inline; confirmação antes de cancelar ou fechar ordens; atalhos de teclado com
  tooltip.
- Textos em PT-MZ; quantidades, unidades e moeda pelo formatador central.
- Espaçamentos, cabeçalhos e botões alinhados com Pedidos em todas as secções.
- Verificar visualmente: reconstruir e relançar o desktop (procedimento habitual com demo-login)
  e rever cada secção nos temas claro e escuro, com dados de demonstração realistas de uma
  padaria e de uma fábrica de blocos (actualizar `DemoEnterpriseDataSeeder`).

## Testes e harness (obrigatórios)

- Conversão de unidades, rendimento, escala de receita, repartição de custo de co-produtos.
- Ciclo de estados: transições válidas e inválidas com `BusinessRuleException`.
- Reserva no `RELEASED`, apontamentos parciais idempotentes, consumo FEFO, genealogia.
- Custo real, custo médio actualizado, desvios, lançamentos contabilísticos.
- Plano do dia com histórico de vendas de teste.
- Cura/quarentena: o stock não é vendável antes do prazo ou da aprovação.
- Concorrência: dois apontamentos simultâneos na mesma ordem.
- Paridade PostgreSQL de todas as migrações.
- Harness de UI: secções existem, sem emojis, ícones `UIHelper`, nada bloqueia o EDT, acções
  coerentes com o estado.
- `mvn clean verify` verde no fim de cada fase.

## Fases

1. SPEC + HARNESS aprovados.
2. Unidades de medida e ficha técnica versionada com rendimento e co-produtos.
3. Ciclo da ordem, reservas, apontamentos, lotes e genealogia.
4. Custeio e lançamentos.
5. UI: Hoje, Ordens, Detalhe, Fichas, Lotes.
6. Plano do dia e reembalamento com etiquetas.
7. Modo Chão de Fábrica (integração com a balança) e relatórios PDF.
8. Roteiro de aceitação em `docs/GUIA_ACEITACAO_LOJA.md` para padaria e para blocos.

## Regras de trabalho

- Controller → Service → Repository; DTOs em `contracts`; injecção por construtor; campos `final`.
- Skills: `multicore-new-endpoint`, `multicore-pdf-document`, `multicore-icons`,
  `multicore-solid-review` antes de fechar cada fase.
- Fazer stage só dos ficheiros que alteraste (outras sessões partilham a branch). Não fazer
  commit nem push sem pedido explícito.
- No fecho de cada fase, actualizar `tasks/current.md` (máx. 1 página).
