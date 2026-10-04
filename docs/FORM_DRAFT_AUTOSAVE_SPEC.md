# Especificação Técnica: Auto-Salvamento & Recuperação de Rascunhos de Formulários (SPEC-DFRT-001)

## 1. Visão Geral & Objetivos
Esta especificação define o subsistema de **Auto-Salvamento & Recuperação de Rascunhos de Formulários** (`FormDraftManager` e `FormDraftBanner`).
O objetivo é blindar a operação diária no Multicore ERP contra perdas acidentais de dados decorrentes de:
1. **Cortes de energia elétrica / blackouts** frequentes em operações de retalho e armazéns em Moçambique.
2. **Fecho involuntário** ou clique acidental em "Cancelar" / `Esc` durante o preenchimento de propostas, cotações, faturas ou fichas de clientes complexas.
3. **Quedas temporárias de conectividade** com o backend que impeçam a submissão imediata.

## 2. Modelo Canónico `FormDraft`
Cada rascunho de formulário é representado por um registo imutável:
- `formKey`: Identificador único do tipo de formulário (ex.: `"CUSTOMER_CREATE"`, `"QUOTATION_NEW"`, `"PURCHASE_ORDER"`, `"PRODUCT_NEW"`).
- `title`: Título legível para identificação no diálogo de recuperação (ex.: `"Nova Cotação — Cimento Moçambique"`).
- `payload`: Mapa de chave/valor (`Map<String, String>`) contendo o estado serializado dos campos do formulário.
- `timestampMillis`: Carimbo da última gravação em milissegundos.
- `author`: Utilizador que iniciou o preenchimento (obtido de `CurrentUserContext.getUsername()`).

## 3. Arquitetura do Gestor de Rascunhos (`FormDraftManager`)
1. **Singleton & Thread-Safety**:
   - `FormDraftManager.getInstance()` gere o ciclo de vida dos rascunhos com sincronização concorrente segura.
2. **Armazenamento em Disco Local**:
   - Diretório canónico: `${user.home}/.multicore/drafts/`.
   - Ficheiro individual por formulário: `${user.home}/.multicore/drafts/<formKey>.json`.
   - Gravação atómica utilizando Jackson `ObjectMapper` com escrita defensiva.
3. **Ciclo de Vida Operacional**:
   - `saveDraft(formKey, title, payload)`: grava ou atualiza o rascunho.
   - `getDraft(formKey)`: recupera o rascunho existente, se houver.
   - `hasDraft(formKey)`: verificação rápida booleana de existência.
   - `clearDraft(formKey)`: purga o rascunho de memória e disco (acionado após gravação final com sucesso).
   - `listAllDrafts()`: inventário de todos os rascunhos pendentes.
   - `clearAll()`: purga global de rascunhos.

## 4. Componente Visual de Restauração (`FormDraftBanner`)
1. **Comportamento na Abertura**:
   - Ao inicializar um formulário, verifica se existe rascunho não submetido para a `formKey`.
   - Se existir, exibe um banner horizontal discreto no topo do diálogo com aviso:
     *"Existe um rascunho não submetido deste formulário guardado [há X min]. Deseja restaurar?"*
2. **Ações Disponíveis**:
   - `[ Restaurar Rascunho ]`: invoca o callback `onRestore` repovoando os campos e oculta o banner.
   - `[ Descartar ]`: purga o ficheiro de rascunho e remove o banner da visualização.

## 5. Critérios de Aceitação & Conformidade
- Todos os testes da suíte `FormDraftAutoSaveHarnessTest` passam a 100%.
- A gravação e leitura de rascunhos não bloqueia o Event Dispatch Thread (EDT) nem lança exceções visíveis ao utilizador.
- Todas as classes criadas e modificadas respeitam rigorosamente o limite de $\le 1000$ linhas.
