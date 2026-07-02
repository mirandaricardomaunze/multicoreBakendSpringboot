# Spec — Entrada de stock por caixas (cadastro/reposição)

> Permitir dar entrada de mercadoria **por nº de caixas** (mais unidades soltas), convertendo para
> unidades, sem alterar a unidade interna de stock nem as camadas de faturação/POS/guia/reserva.
> Toca apenas o [StockPanel](../src/main/java/com/phcpro/gui/StockPanel.java) (apresentação/entrada).

**Última actualização:** 2026-07-01

## Problema

A loja compra e arruma mercadoria **às caixas** (ex.: 1 caixa de refresco = 24 unidades), mas o
sistema só aceitava a entrada de stock **em unidades** — o operador tinha de calcular à mão
`caixas × unidades`. Já existia a coluna **"Qtd Caixas"** no inventário (visualização) e o campo
**"Unidades por Caixa"** no cadastro do produto, mas faltava o caminho de **entrada por caixas**.

## Decisão

- **A unidade interna de stock é a UNIDADE.** Movimentos, reservas, faturação, guia de remessa e POS
  continuam **em unidades** — nada disso muda. A "caixa" é apenas uma **camada de entrada e de
  visualização** (conversão), nunca a unidade persistida.
- `Product.unitsPerBox` (já existente) é o factor de conversão. `Qtd Caixas = stock ÷ unitsPerBox`
  (visualização, já existente).
- Nos diálogos de **entrada de stock** (stock inicial após cadastro **e** "Adicionar Lote/Validade",
  que são o mesmo método `createBatchEntryDialog`), o operador introduz:
  - **Nº de Caixas** (inteiro ≥ 0),
  - **Unidades soltas** (≥ 0),
  - vendo as **unidades por caixa** do produto seleccionado e o **Total (unidades)** calculado em
    tempo real: `total = caixas × unitsPerBox + soltas`.
- A quantidade gravada no movimento de stock é o **total em unidades** — o resto do sistema não nota
  diferença. Validação: `total > 0`.
- O **Total (unidades)** recalcula quando muda o produto (e portanto `unitsPerBox`), o nº de caixas
  ou as unidades soltas.

## Venda ao grosso (faturação)

A loja também vende **caixas fechadas** a revendedores. Na **faturação** (não no POS, que é retalho
rápido à unidade) a linha ganhou um campo opcional **"Caixas"**: ao indicar o nº de caixas com um
produto seleccionado, a **Qtd (unidades)** preenche-se automaticamente = `caixas × unitsPerBox`. A
entrada directa em unidades continua a funcionar (campo "Caixas" vazio). **O cálculo de dinheiro é
sempre por unidade** — a caixa é só um multiplicador de conveniência; nada muda no `LineCalculator`
nem no IVA. O POS mantém-se por unidade (não é tocado).

## Não-objetivos

- Não alterar Services (faturação/POS/guia/reserva/inventário) nem DTOs — só a UI de entrada.
- Não converter o stock interno para caixas nem guardar caixas como unidade. A caixa é derivada.
- Não tocar na recepção de compras (`ComprasPanel`) nesta iteração — fica em unidades por agora.
- Não permitir caixas fracionadas (nº de caixas é inteiro; fracções entram como unidades soltas).
