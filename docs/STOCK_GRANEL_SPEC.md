# Spec — Stock a granel por medição (SA-C17)

**Data:** 2026-11-08 · **Estado:** feito (F2) · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Harness:** `docs/SECTORES_ACTIVIDADE_HARNESS.md`

## Problema

Areia, brita e pedra estão em monte: não se contam. O stock do sistema nunca bate, ninguém sabe se a
diferença é perda natural (vento, chuva, compactação) ou desvio, e o material vendido por levantar ainda
está no monte.

## Decisões

1. **Produto a granel** (`bulk_stock_settings`): o gestor marca o produto e define a tolerância de perda
   natural (0 a 50%).
2. **Medição** (`bulk_measurements`): quantidade directa, ou volume do monte em metros: rectangular
   (C×L×A), prisma (C×L×A÷2) ou cone (π·r²·h÷3). O volume em m³ passa para a unidade base pelas
   unidades do produto (SA-C02). Exemplo: brita em toneladas com 1 m³ = 1,6 t. Sem esse factor, o servidor
   pede para o definir ou para usar a quantidade directa.
3. **Comparação:** diferença = medido − material vendido por levantar (SA-C07) − stock do sistema.
   A percentagem é sobre o stock do sistema (100% se o sistema tiver zero).
4. **Decisão:**
   - sem diferença: nada muda;
   - dentro da tolerância: o acerto é feito logo;
   - acima da tolerância: fica à espera de um gestor, que aprova ou rejeita com motivo.
5. **O acerto** passa por `StockMovementRegistrationService` como `ADJUSTMENT`, com a descrição da medição.
   Ao aprovar mais tarde, regista-se a diferença medida, não um novo cálculo: as vendas entretanto feitas
   são movimentos à parte.
6. **Segurança:** se o monte tiver menos do que o material já vendido por levantar, a medição é recusada,
   porque o stock ficaria negativo e há clientes com saldo. A aprovação bloqueia a linha, por isso não há
   duplo acerto.
7. **Pré-visualização:** o cálculo completo sem gravar, para o fiel ver o resultado antes de confirmar.

## API (`/api/bulk-stock`)

| Método | Caminho | Quem |
|---|---|---|
| GET/PUT | `/settings` | ler: vendedor · alterar: gestão |
| GET | `/measurements?companyId=` | vendedor |
| POST | `/measurements/preview` · `/measurements` | vendedor |
| POST | `/measurements/{id}/approve` · `/reject` | gestão |

Auditoria: `BULK_STOCK_SETTING_*`, `BULK_MEASUREMENT_RECORD`, `BULK_MEASUREMENT_APPROVE`, `BULK_MEASUREMENT_REJECT`.

## Interface

Stock › separador **Granel**: formulário de medição com os campos que mudam conforme o método,
pré-visualização com a conta completa e a cor da tolerância, histórico com aprovação e rejeição, e o botão
"Produtos a Granel" para a gestão. O separador "Granel" fica no fim da barra do Stock. Não entra dentro
de "Inventário Físico", porque o painel selecciona esse separador pelo componente.
