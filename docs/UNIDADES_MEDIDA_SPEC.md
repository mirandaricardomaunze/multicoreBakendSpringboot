# Spec — Unidades de medida e conversões por produto (SA-C02)

**Data:** 2026-11-08 · **Estado:** feito (F1, F2 em 2026-11-19) · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Harness:** `docs/SECTORES_ACTIVIDADE_HARNESS.md`

## Problema

Um estaleiro vende cimento ao saco e à palete, varão à barra e ao kg, areia ao m³ e à carga de camião.
O sistema só conhecia a unidade e a composição caixa/embalagem do produto.

## Decisões

1. **Unidade base.** Cada produto tem uma unidade base: a do stock, dos preços e dos documentos.
   `products.base_unit_code` é anulável; nulo significa `KG` para venda ao peso e `UN` nos restantes,
   que é o que o stock desses produtos sempre significou. **Nenhum dado existente muda.**
2. **Catálogo.** `units_of_measure` tem as unidades do sistema (semeadas no arranque por
   `SystemUnitCatalogSeeder`, idempotente) e as embalagens próprias de cada empresa.
   Grandezas: contagem, massa, volume, comprimento, área e embalagem.
3. **Unidades alternativas.** `product_units`: `1 unidade = factor × base`, com código de barras próprio
   opcional e marcação de uso na venda e na compra.
   - Na mesma grandeza física o factor é fixo (g = 0,001 kg) e o servidor calcula-o.
   - Em embalagens ou entre grandezas (1 m³ de areia = 1,6 t) o factor é indicado e tem de ser > 0.
   - Base sem casas decimais (UN) só aceita factores inteiros e recusa conversões com fracção.
4. **Base fixa depois de usada.** A base só muda se o produto não tiver movimentos de stock, linhas de
   fatura, encomenda, cotação ou compra, nem unidades alternativas.
5. **Caixa e embalagem do produto continuam a mandar.** Aparecem como linhas derivadas, só de leitura.
   Não se cria `CX` nem `EMB` explícitos num produto que já tem essa composição.
6. **Só entrada e apresentação.** Nenhum documento grava a unidade alternativa: as linhas continuam na
   base, e a unidade alternativa só serve para indicar a quantidade.
7. **Onde se indica a quantidade noutra unidade (F2, 2026-11-19):**
   - Fatura e encomenda de venda: Ctrl+U, com as unidades marcadas para venda.
   - Encomenda a fornecedor: Ctrl+U, com as unidades marcadas para compra. O preço continua por unidade
     base; o lote, a validade e o IVA da linha não mudam.
   - POS: ler o código de barras de uma unidade marcada para venda acrescenta a quantidade dessa unidade
     (código da palete = 40 sacos). `GET /api/units/barcode` devolve o artigo e o factor; um código
     desconhecido ou de uma unidade fora da venda não devolve nada, e o POS mostra "Produto não encontrado".
8. **Preço da palete (decisão do responsável, 2026-11-19).** Não há preço por unidade alternativa: a palete
   custa 40 × o preço do saco. Para a palete sair mais barata, cria-se um escalão na tabela de preço na
   unidade base (por exemplo, a partir de 40 un, 850 MT). O stock, o IVA e os documentos continuam em sacos.

## API (`/api/units`, leitura para vendedores, alteração para gestão)

| Método | Caminho | Uso |
|---|---|---|
| GET | `/api/units?companyId=` | Catálogo visível à empresa |
| POST | `/api/units` | Embalagem própria da empresa |
| GET | `/api/units/products/{id}?companyId=` | Base, bloqueio e unidades do produto |
| PUT | `/api/units/products/base` | Mudar a base (só sem uso) |
| PUT | `/api/units/products` | Criar ou actualizar uma unidade do produto |
| DELETE | `/api/units/products/units/{id}?companyId=` | Remover uma unidade |
| GET | `/api/units/products/{id}/convert?companyId=&unitCode=&quantity=` | Converter para a base |
| GET | `/api/units/barcode?companyId=&barcode=&warehouseId=` | POS: artigo e factor da unidade com este código |

Todas as alterações ficam no trilho de auditoria (`PRODUCT_UNIT_*`, `PRODUCT_BASE_UNIT_SET`,
`UNIT_OF_MEASURE_CREATE`).

## Interface

Stock › detalhe do produto › separador **Unidades**: cartão da unidade base (com cadeado e motivo),
tabela das unidades, diálogo com pré-visualização "1 Palete = 40 un" e conversor rápido.
