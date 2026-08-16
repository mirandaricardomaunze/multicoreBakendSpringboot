# SPEC — Consistência profissional da UI Swing

**Criado em:** 2026-08-09  
**Camada:** cliente desktop Swing (`com.phcpro.gui`, `com.phcpro.gui.components`)  
**Backend:** contratos e regras de negócio permanecem inalterados.

## 1. Objectivo

Uniformizar inputs, selects, botões, tabelas, formulários, estados assíncronos e acessibilidade do
cliente-fino. A interface deve continuar densa e rápida para operação de ERP, funcionar a 100%, 125%
e 150% de escala no Windows e nunca bloquear o EDT durante chamadas HTTP, impressão ou relatórios.

## 2. Princípios obrigatórios

- Uma única fonte visual em `UIHelper`/componentes; painéis não criam paletas próprias.
- Regras de negócio continuam no backend; a UI apenas valida formato e apresenta erros.
- Um ecrã tem uma acção primária; destrutivas são distintas e confirmadas.
- Campos mostram obrigatoriedade, ajuda e erro junto do próprio campo.
- Valores técnicos (`ACTIVE`, `MANAGER`, nomes de enums) não são apresentados ao utilizador.
- Listagens usam pesquisa, estado vazio, loading, formatação por tipo e acções previsíveis.
- Formulários com linhas/documentos usam painel completo; modal fica para acções curtas.
- Chamadas remotas/PDF/importação nunca correm no EDT.
- Navegação por teclado, tooltip e nome acessível são obrigatórios em controlos apenas com ícone.

## 3. Prioridade 1 — componentes canónicos de formulário

Criar e adoptar:

- `FormField`: label, marcador obrigatório, conteúdo, ajuda e erro inline.
- `MoneyField`: aceita ponto ou vírgula, devolve `BigDecimal`, escala monetária e erro accionável.
- `QuantityField`: `BigDecimal`, até 3 casas decimais e regra configurável para zero/negativos.
- `DateField`: entrada `yyyy-MM-dd`, apresentação/ajuda clara e parsing seguro.
- estado read-only visualmente distinto; altura e foco vêm de `UIHelper`.

Não duplicar cálculo fiscal, comercial ou de stock nestes componentes.

## 4. Prioridade 2 — selects, botões e tabelas

### Selects

- Renderer é preservado ao aplicar tema.
- Fábrica para labels humanas de enums.
- Selects de entidades volumosas devem ser pesquisáveis.
- Estados: `A carregar…`, `Sem opções` e falha com tentativa novamente.

### Botões

- Variantes: primary, success, secondary, danger e icon-only.
- `icon-only` exige tooltip e `AccessibleContext.accessibleName`.
- Botões de barras não dependem de largura fixa; texto pode crescer sem corte.
- Durante submissão remota, botão fica desactivado e não permite duplo envio.

### Tabelas

- Dinheiro/quantidade/percentagem à direita; datas e estados com renderers canónicos.
- Estado usa badge semântico (`success`, `warning`, `danger`, `neutral`).
- Loading não deixa dados antigos aparentarem estar actualizados.
- Preferências de ordem/largura de colunas podem ser guardadas por tabela estável.
- Listagens grandes usam paginação de API quando o contrato existir.

## 5. Prioridade 3 — assíncrono e erros

- Toda chamada de `desktop.client` iniciada por painel executa via `UIHelper.loadAsync` ou
  `runWithProgress`.
- Capturar empresa/utilizador no EDT e repor apenas na thread de trabalho.
- Respostas de empresa anterior são ignoradas após troca de tenant.
- Erro de rede, autenticação, permissão e regra de negócio têm mensagens distintas.
- Loading termina sempre, inclusive em erro; a acção pode ser tentada novamente.

## 6. Prioridade 4 — formulários e responsividade

- Fatura, encomenda, compra e outros documentos multi-linha usam `DocumentEditorHost`.
- Modal é limitado a 94% da área principal e possui scroll interno.
- Remover `setPreferredSize` de painéis quando GridBag/BorderLayout/scroll puder resolver.
- Cores de dashboards passam para tokens semânticos do tema.
- Validar em 1366×768 e em escala Windows 100%, 125% e 150%.

## 7. Prioridade 5 — decomposição

- Painéis acima de 1.000 linhas são divididos por caso de uso, sem criar novos domínios:
  toolbars, listagens, editores, diálogos e formatadores em classes reutilizáveis.
- Ordem: `ComercialPanel`, `StockPanel`, `POSPanel`, `ComprasPanel`, `HRPanel`, `ConfigPanel`.
- Extracção não altera contratos HTTP nem regras; testes de atalhos e fluxos permanecem verdes.

## 8. Acessibilidade e idioma

- Texto visível em português de Moçambique.
- Ordem de foco acompanha o fluxo do formulário.
- `Esc` cancela/volta; `Ctrl+S` grava quando aplicável; atalhos POS são preservados.
- Cor nunca é o único indicador de estado: badge inclui texto/ícone.
- Contraste deve permanecer legível nos temas claro e escuro.

## 9. Fora de âmbito

- Reescrever Swing noutra framework.
- Alterar regras fiscais, stock, caixa, faturação ou salários.
- Introduzir acesso directo do desktop à base de dados.
- Paginação sem endpoint backend adequado; nesses casos fica registada como contrato necessário.

## 10. Definition of done

- Harness automático totalmente verde.
- Harness manual executado em Windows nas três escalas.
- Nenhuma chamada HTTP detectada no EDT nos fluxos auditados.
- Nenhum enum técnico visível nos fluxos cobertos.
- Sem novas cores ad-hoc ou tamanhos mágicos em painéis.
- `mvn clean compile` e `mvn test` verdes.
- `tasks/current.md` actualizado a cada fase encerrada.

