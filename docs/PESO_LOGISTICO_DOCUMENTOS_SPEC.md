# Peso logístico nos documentos — especificação

**Criado em:** 2026-08-17
**Estado:** implementado, automatizado em PS-01..PS-06
**Origem:** pergunta do utilizador — *"o peso é incluído no cadastro de produtos e nos
documentos?"*

---

## 1. Onde o peso existe

**No cadastro do produto** (V41): `netUnitWeightKg` (líquido) e `grossUnitWeightKg` (bruto, com
embalagem). Ambos opcionais; `LogisticsLoadCalculator.validateWeights` recusa negativos e recusa
bruto menor que o líquido.

**Nos documentos que movimentam mercadoria:**

| Documento | Carga | Porquê |
|---|---|---|
| Guia de Remessa ao cliente | ✅ | sai mercadoria para a rua |
| Lista de Separação (picking) | ✅ | alguém vai carregar aquilo |
| Encomenda | ✅ | prepara-se o transporte |
| **Guia de Transferência** | ✅ **(novo)** | viaja numa carrinha como qualquer expedição |
| Factura, recibo, notas, mapas fiscais | ❌ | ninguém carrega uma factura |

A ausência na transferência era a única que não era uma escolha — era um esquecimento.

---

## 2. A regra: uma conta só

`LoadSummaryRenderer` (novo) é o **único** sítio onde a carga se calcula e se escreve. A conta
estava dentro do `DeliveryGuidePrintService`; copiá-la para a transferência era pôr a mesma
regra em duas portas — o padrão de erro que este projecto já fechou três vezes (IVA, saldo em
dívida, "isto conta como venda").

Usa o **peso bruto**: o que conta para transportar é o que se levanta, embalagem incluída.

Formato:

```
Carga total: 28.300 kg. Volumes: Arroz 5kg: 2 cx + 2 un, 20.800 kg (16.67% qtd; 73.50% peso)
 | Açúcar 1kg: 1 cx + 8 un, 7.500 kg (83.33% qtd; 26.50% peso)
```

A repartição em **quantidade e peso** é o que serve a quem carrega: um artigo pode ser 83% das
unidades e só 26% do peso.

---

## 3. Sem pesos, não imprime nada

`build(...)` devolve `null` quando o peso total é zero, e o documento omite a linha.

É deliberado: numa empresa que ainda não preencheu pesos no cadastro, um **"Carga total: 0,000
kg"** no documento diz que a carga não pesa nada — pior do que não dizer nada. Artigos sem peso
no meio de artigos com peso não impedem os outros de contar (PS-04).

---

## 4. Limites conhecidos

- O **peso líquido** é guardado mas **não é usado em lado nenhum** — só o bruto entra na carga.
  Serve para quando houver declarações que o exijam.
- Não há **capacidade do veículo**: o documento diz quanto pesa, não diz se cabe.
- Não há peso na **recepção de compras** (o que chega do fornecedor).
- A guia de remessa antiga (`GuideRemittancePrintService`) continua sem peso — se ainda estiver
  em uso, o mesmo tipo de documento comporta-se de duas maneiras.
