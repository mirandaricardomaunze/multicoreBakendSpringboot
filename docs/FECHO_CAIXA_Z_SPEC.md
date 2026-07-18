# Fecho de Caixa (Z) — documento imprimível + reconciliação

**Última actualização:** 2026-07-12
**Estado:** feito (serviço + PDF + impressão no fecho + testes). Reimpressão do histórico = passo futuro.

## Objectivo

Dar ao fecho de caixa um **documento Z imprimível** com a **reconciliação da gaveta**: abertura +
vendas em numerário + suprimentos − sangrias − devoluções = **esperado**, versus o **contado**, e a
**diferença**. Fecha o item pendente da Fase 5 do retalho (conferir totais de caixa).

## Reconciliação (gaveta)

```
  Saldo de abertura
  (+) Vendas em numerário          (nº de vendas)
  (+) Suprimentos
  (−) Sangrias
  (−) Devoluções (reembolsos)
  ─────────────────────────────
  = Esperado na gaveta
    Contado (saldo físico no fecho)
  ─────────────────────────────
  = Diferença   (contado − esperado)
```

Coincide com o cálculo do `POSService.closeSession` / `computeExpectedCash` (mesma fórmula) — o Z
não inventa números, lê os movimentos de caixa (`TillMovement`) da sessão. Vendas não-numerário
(cartão, M-Pesa/e-Mola) não passam pela gaveta (vão à tesouraria) — por isso **não** entram no
esperado; o Z é da **gaveta**, não do total de vendas (esse é o relatório diário).

## Peças

- **`PosZReportDTO`** (`modules/pos/dto`) — abertura, vendas/suprimentos/sangrias/devoluções,
  esperado, contado, diferença, operador, datas, nº de vendas.
- **`POSService.buildZReport(sessionId)`** — leitura pura (tenant-scoped); funciona **antes e depois**
  do fecho (permite pré-visualizar). Soma os `TillMovement` por tipo.
- **`POSZReportPrintService.render(sessionId)`** (`modules/printing`) — PDF A4 com cabeçalho da
  empresa (`CompanyHeaderRenderer` + `PdfDocumentBuilder`), bloco de meta (operador/datas) e a tabela
  de reconciliação; bloco de assinaturas (Operador / Conferido por).
- **UI (`POSPanel`)** — ao **Fechar Caixa** com sucesso, oferece "Imprimir Fecho (Z)".

## Regras / limites

- **Permissão:** ler/imprimir o Z é tenant-scoped (não exige gerente); fechar caixa com diferença
  continua a exigir MANAGER/ADMIN (regra já existente no `closeSession`).
- **Reimpressão a partir do histórico de sessões** não está nesta iteração (não há lista de sessões
  no desktop) — `buildZReport` já suporta qualquer sessão, falta só o ecrã. Passo futuro.
- Totais de vendas **por método de pagamento** (todos os métodos) são do **relatório diário**
  (`ReportService`), não do Z de gaveta.
