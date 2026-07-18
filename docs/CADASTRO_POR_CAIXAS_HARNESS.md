# Harness — Entrada de stock por caixas

> Cenários de validação para [CADASTRO_POR_CAIXAS_SPEC.md](CADASTRO_POR_CAIXAS_SPEC.md).
> CX-01..CX-06 manuais (UI Swing); a invariância de unidades a jusante é coberta pelos testes
> existentes de faturação/POS/inventário (continuam em unidades, sem alterações).

**Última actualização:** 2026-07-01

## Cenários manuais (UI)

| ID    | Passos                                                                                          | Esperado |
|-------|------------------------------------------------------------------------------------------------|----------|
| CX-01 | Cadastrar produto com **Unidades por Caixa = 24**. No stock inicial, escrever **10 caixas**, 0 soltas. | Total (unidades) mostra **240**; movimento de entrada regista **240 unidades**. |
| CX-02 | Mesmo produto: **10 caixas + 3 unidades soltas**.                                               | Total = **243**; entrada de 243 unidades. |
| CX-03 | "Adicionar Lote/Validade", escolher produto de 24 und/caixa, mudar para outro produto de **6 und/caixa**. | O rótulo "unidades por caixa" e o Total recalculam para o factor do novo produto. |
| CX-04 | Nº de Caixas = 0 e Unidades soltas = 0.                                                         | Erro "quantidade deve ser maior que zero"; não regista. |
| CX-05 | Após a entrada CX-01, abrir o inventário.                                                       | Coluna **Qtd Unidades = 240**, **Qtd Caixas = 10.00**. |
| CX-06 | Faturar/POS/guia 50 unidades desse produto.                                                     | Documento usa **unidades** (50), stock baixa 50 → 190 un (≈7.92 caixas). Sem alteração de comportamento. |
| CX-07 | Stock › **Editar Produto**, escolher o artigo, mudar **Unidades por Caixa** de 24 → 12, gravar.   | Inventário recalcula **Qtd Caixas** com o novo factor (240 un → 20.00 caixas). SKU imutável. |
| CX-08 | Editar Produto: mudar preço/nome/IVA/categoria/imagem.                                            | Alterações reflectidas no catálogo POS e nos documentos futuros; stock intacto; auditoria `PRODUCT_UPDATE`. |
| CX-09 | Faturação: produto de 24 und/caixa, escrever **2** em "Caixas".                                   | "Qtd (unidades)" preenche **48** automaticamente. Adicionar linha → total = preço_un × 48 (dinheiro por unidade). |
| CX-10 | Faturação: deixar "Caixas" vazio e escrever 5 em "Qtd (unidades)".                                | Vende 5 unidades (entrada directa em unidades continua a funcionar). |

## Invariância automática (já coberta)

- Faturação/POS/guia/reserva continuam a operar sobre `quantity` em unidades — sem mudança de
  Service nem de DTO, logo `ComercialServiceTest`, `POSServiceTest` e os testes de inventário
  garantem que nada regrediu (`mvn clean test`).
