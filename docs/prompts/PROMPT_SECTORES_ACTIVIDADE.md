# Prompt — Implementar uma fase dos Sectores de Actividade

> Colar numa sessão nova do Claude Code, na raiz do repositório, trocando `<FASE>` pela fase
> (F1 a F7) de `docs/SECTORES_ACTIVIDADE_SPEC.md`. Uma fase por sessão.
> F3 usa também `PROMPT_BALANCA_VENDA_PESO.md`; F4 usa `PROMPT_PRODUCAO_TRANSFORMACAO.md`.

---

## Papel

És engenheiro sénior do Multicore ERP (Java 21, Spring Boot 3.2.5, Swing, reactor
`contracts` / `backend` / `desktop`). O objectivo do produto é servir talhos, padarias, estaleiros
e outros negócios moçambicanos **sem limitações**, com capacidades genéricas que se combinam.
Entregas nível comercial: SPEC, harness, código, testes, PDFs e UI acabada. Nada de "básico".

## Tarefa

Implementar a fase **`<FASE>`** de `docs/SECTORES_ACTIVIDADE_SPEC.md`.

## Antes de escrever código

1. Ler `CLAUDE.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `MOVIMENTOS_COMERCIAIS.md`,
   `tasks/current.md` e `docs/SECTORES_ACTIVIDADE_SPEC.md` inteiro (sobretudo §3 problemas e
   §5 regras invioláveis).
2. Para cada capacidade da fase, encontrar no código o que já existe (a coluna "Estado" da SPEC
   diz por onde começar) e **estender, não duplicar**. Ler as SPECs ligadas (ex.:
   `PRECO_GROSSO_SPEC`, `CADASTRO_POR_CAIXAS_SPEC`, `PRODUCT_PACKAGING_COMPOSITION_SPEC`,
   `LIMITE_CREDITO_SPEC`, `POS_CONTINGENCIA_RESILIENCIA_SPEC`, `COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC`).
3. Pôr as decisões em aberto da §7 que tocam esta fase ao responsável e **esperar resposta**.
4. Escrever uma SPEC e um HARNESS por capacidade (`docs/<CAPACIDADE>_SPEC.md`,
   `docs/<CAPACIDADE>_HARNESS.md`, IDs próprios), com:
   - os problemas reais da §3 que resolve, escritos como cenários de aceitação;
   - modelo de dados, migrações Flyway (numeração seguinte à última), paridade PostgreSQL;
   - endpoints, DTOs em `contracts`, regras com `BusinessRuleException`;
   - impacto em fatura, guia, NC, recibo, stock, caixa, contabilidade e SAF-T;
   - ecrãs e fluxos de UI;
   - controlos anti-fraude e eventos de auditoria.
   Apresentar e **esperar aprovação** antes de implementar.

## Cenários que a solução tem de aguentar

Além dos cenários da §3, testar sempre:

- Uma empresa com dois perfis ao mesmo tempo (ex.: estaleiro + fábrica de blocos).
- Duas vendas ou levantamentos em simultâneo sobre o mesmo saldo ou stock.
- Anulação e nota de crédito depois da operação (o saldo, vasilhame ou stock tem de voltar certo).
- Internet a cair a meio da operação.
- Utilizador sem permissão a tentar a acção (backend recusa, não só a UI).
- Dados antigos sem os campos novos (migração com valores por omissão seguros).
- Volume de uma loja real: listas paginadas e sem consultas N+1.

## Padrão de UI exigido

- O ecrã de **Pedidos** é a referência visual. Ler `docs/UI_DESIGN_SYSTEM.md`,
  `UI_CONSISTENCIA_PROFISSIONAL_SPEC.md`, `UI_TABELAS_UX_SPEC.md`, `UI_FEEDBACK_PROFISSIONAL_SPEC.md`,
  `FORMATO_NUMERICO_SPEC.md`.
- Componentes do projecto (`ModernButton`, `ActionMenuButton`, `UIHelper.tableCardTop`,
  `UIHelper.styleTable`), tokens de tema claro/escuro, sem cores literais.
- Ícones só por `UIHelper.icon(...)` (skill `multicore-icons`). Nunca emojis.
- Cada conta-corrente nova (saldo por levantar, vasilhame, consignação, obra) tem: cartão de
  resumo no detalhe do cliente, extracto com filtros e PDF, e acção directa ("Levantar",
  "Devolver vasilhame", "Fazer acerto").
- Estados vazios com explicação e acção; `SwingWorker` para carregar; validação inline;
  confirmação antes de acções irreversíveis; atalhos com tooltip; textos PT-MZ.
- O Perfil de Actividade simplifica menus, mas nada fica inacessível a quem tem permissão.
- Verificar visualmente: reconstruir e relançar o desktop e rever cada ecrã nos temas claro e
  escuro, com dados de demonstração realistas do sector (actualizar `DemoEnterpriseDataSeeder`).

## Testes

- Unitários para cada regra pura (conversões, preço aplicado, saldos, repartições).
- Integração para cada fluxo de documento, incluindo anulação.
- Concorrência nos saldos.
- Harness de UI: sem emojis, ícones `UIHelper`, nada bloqueia o EDT.
- `mvn clean verify` verde no fim da fase.

## Regras de trabalho

- Controller → Service → Repository; DTO na fronteira; injecção por construtor; campos `final`.
- Skills: `multicore-new-endpoint`, `multicore-new-module`, `multicore-pdf-document`,
  `multicore-icons`, `multicore-solid-review` antes de fechar.
- Fazer stage só dos ficheiros que alteraste; não fazer commit nem push sem pedido explícito.
- No fecho, actualizar o estado da fase em `docs/SECTORES_ACTIVIDADE_SPEC.md` e
  `tasks/current.md` (máx. 1 página).
