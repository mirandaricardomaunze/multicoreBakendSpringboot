# Harness — Fecho de Caixa (Z)

Ver [FECHO_CAIXA_Z_SPEC.md](FECHO_CAIXA_Z_SPEC.md).

## Automáticos — `POSServiceTest` (buildZReport)

| ID    | Cenário                                                                 | Esperado                                             |
|-------|-------------------------------------------------------------------------|-----------------------------------------------------|
| Z-01  | Abertura 100; vendas 500; suprimento 50; sangria 30; devolução 20       | esperado = 100+500+50−30−20 = **600**               |
| Z-02  | Sessão fechada com contado 590 e esperado 600                           | diferença = **−10**                                 |
| Z-03  | Sessão ainda aberta (sem contado)                                       | contado = null, diferença = **null** (pré-visualiza)|
| Z-04  | Sessão de outra empresa                                                 | lança (tenant guard)                                |

## Manuais — PDF + UI

| ID     | Cenário                                                              | Esperado                                                       |
|--------|---------------------------------------------------------------------|----------------------------------------------------------------|
| Z-50   | Abrir caixa, vender (numerário + cartão), suprimento/sangria, fechar | Diálogo de fecho → "Imprimir Fecho (Z)" gera o PDF.           |
| Z-51   | Ler o PDF Z                                                          | Cabeçalho da empresa; reconciliação bate certo; assinaturas.  |
| Z-52   | Vendas em cartão/M-Pesa                                              | **Não** entram no esperado da gaveta (só numerário).          |
| Z-53   | Fecho com diferença ≠ 0                                              | Exige MANAGER/ADMIN (regra existente); Z mostra a diferença.  |
