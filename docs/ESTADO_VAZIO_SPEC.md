# Tabela vazia que diz o que falta e o passo seguinte

Estado: **feito** · 2026-11-17 · Harness: `desktop/.../components/EmptyStateGuidanceHarnessTest.java`

## Problema

A galeria visual mostrou "Sem registos." em quase todas as tabelas: de 172, só 5 tinham frase própria
e 1 oferecia uma acção. A ligação ao botão principal já existia (`TableCardTop.linkToTable`), mas
acontecia depois de a tabela desenhar o estado vazio, e o `TableEmptyState` não a escutava. Uma tabela
que nasce vazia — o primeiro uso de qualquer ecrã — ficava para sempre sem frase e sem botão.

## Regras

| ID    | Regra |
|-------|-------|
| EV-01 | O título do cartão dá a frase: "Cotações" → "Sem cotações."; corta-se em " — ", " (" e " / "; "&" passa a "e"; títulos no singular ficam com "Sem registos.". |
| EV-02 | O estado vazio actualiza-se quando o cartão liga frase, botão principal ou filtros, mesmo depois do primeiro desenho. |
| EV-03 | Com linhas que os filtros escondem todas, a frase é "Nenhum resultado com estes filtros." e o botão é "Limpar filtros". |
| EV-04 | O `tableCardTop` liga título, botão verde e filtros à tabela do mesmo cartão; nenhum ecrã precisa de código próprio. Um `emptyText` posto pelo ecrã prevalece. |
| EV-05 | Ligações centrais: `TableEmptyState` escuta as propriedades, `TableCardTop` deriva a frase (`EmptyWording`). |
