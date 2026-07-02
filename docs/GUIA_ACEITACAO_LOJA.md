# Guião de Aceitação — Loja / Mercearia

> Checklist de **validação de terreno** para declarar o Multicore pronto para uso profissional numa
> loja real. O software está implementado e testado (`mvn test` verde); o que falta são as provas que
> só se fazem no local, com hardware e dados reais. Imprimir, executar por ordem, assinalar e assinar.

**Loja:** _______________________  **Data:** ____/____/______  **Responsável:** _______________________

Legenda: **☐ Por fazer · ✅ Passou · ❌ Falhou (anotar)**. Fonte técnica:
[RETAIL_STORE_HARNESS.md](RETAIL_STORE_HARNESS.md), [RESILIENCIA_LIGACOES_HARNESS.md](RESILIENCIA_LIGACOES_HARNESS.md),
[CADASTRO_POR_CAIXAS_HARNESS.md](CADASTRO_POR_CAIXAS_HARNESS.md).

---

## 0. Pré-requisitos (uma vez)

| ☐ | Item | Como confirmar |
|---|------|----------------|
| ☐ | PostgreSQL instalado e a arrancar com o Windows | Serviço `postgresql-x64-*` em *Running* |
| ☐ | Variável `DB_PASSWORD` definida na máquina | App liga sem erro de autenticação |
| ☐ | App arranca e faz login | Janela "MULTICORE" abre; login OK |
| ☐ | Empresa e armazém criados | Aparecem nos combos |

---

## 1. Hardware de balcão

| ☐ | Teste | Passos | Esperado |
|---|-------|--------|----------|
| ☐ | **Leitor de código de barras** | No POS, disparar o leitor sobre um produto | Produto entra no carrinho automaticamente |
| ☐ | **Impressora térmica — recibo** | Finalizar uma venda de teste | Recibo imprime alinhado, com totais, IVA, troco |
| ☐ | **Impressora A4 — fatura/guia/inventário** | Emitir fatura e imprimir; imprimir Inventário | PDFs saem legíveis e completos |
| ☐ | **Gaveta de dinheiro** | Fechar venda a numerário | Gaveta abre (se ligada à impressora) |

**Evidência / anotações:** ______________________________________________

---

## 2. Backup e restauro (recuperação de desastre)

| ☐ | Teste | Passos | Esperado |
|---|-------|--------|----------|
| ☐ | **Backup físico** | Config → Cópias de Segurança → "Backup Físico (BD)" | Ficheiro `.dump` criado em `backups/` |
| ☐ | **Restauro em ambiente separado** | Levar o `.dump` a um PostgreSQL **limpo** e restaurar | BD restaura sem erro |
| ☐ | **Conferência pós-restauro** | Comparar contagens (produtos, faturas, stock) origem vs restauro | Números **idênticos** |

> ⚠️ Enquanto o restauro não for provado numa máquina separada, **não confiar só no backup**.

**Evidência / anotações:** ______________________________________________

---

## 3. Conferência de totais (um dia real)

Fazer ao fim de um dia normal de vendas:

| ☐ | Confere | Onde |
|---|---------|------|
| ☐ | Total de vendas do dia = soma das faturas emitidas | Relatório diário vs lista de Faturas |
| ☐ | Numerário na gaveta (fecho de caixa) = vendas a dinheiro − sangrias + suprimentos | Fecho de Caixa no POS |
| ☐ | Pagamentos por método (dinheiro/cartão/transferência) batem com a tesouraria | Relatório vs Tesouraria |
| ☐ | Fiado em aberto = faturas por liquidar | Contas a Receber |
| ☐ | Stock desceu de acordo com o vendido | Movimentos de stock |

**Diferença encontrada (se houver):** ______________________________________________

---

## 4. Fiscal (SAF-T / AT-MZ)

| ☐ | Teste | Passos | Esperado |
|---|-------|--------|----------|
| ☐ | **Exportar SAF-T** | Fiscal → aba IVA → "Exportar SAF-T (Vendas)" | Gera `.xml` do período |
| ☐ | **Validar contra XSD oficial** | Submeter/validar o XML contra a XSD da AT-MZ | Sem erros de estrutura |
| ☐ | **Apuramento de IVA** | Conferir IVA do período no mapa fiscal | Bate com as faturas |

> ⚠️ A exportação segue a **estrutura** SAF-T mas **não é certificada** — validar com a AT-MZ / contabilista antes de submeter.

**Evidência / anotações:** ______________________________________________

---

## 5. Stock, caixas e FEFO (dados reais)

| ☐ | Teste | Passos | Esperado |
|---|-------|--------|----------|
| ☐ | **Entrada por caixas** | Dar entrada de N caixas de um produto (und/caixa definidas) | Qtd em unidades = N × und/caixa; Inventário mostra "Qtd Caixas" |
| ☐ | **Venda ao grosso** | Fatura/Encomenda: campo "Caixas" preenche a Qtd em unidades | Total = preço unitário × unidades |
| ☐ | **FEFO** | 2 lotes do mesmo produto, validades diferentes; vender | Sai primeiro o de validade mais próxima |
| ☐ | **Alerta de validade** | Produto a vencer ≤30 dias | Aparece no Dashboard/Stock |

**Evidência / anotações:** ______________________________________________

---

## 6. Estabilidade (PC que fica ligado)

| ☐ | Teste | Passos | Esperado |
|---|-------|--------|----------|
| ☐ | **Ociosidade longa** | App aberta > 15 min sem uso; depois gravar um produto | Grava à primeira, sem erro |
| ☐ | **Após suspensão** | Suspender a máquina horas; retomar **sem reiniciar**; faturar | Opera normal (pool recupera) |

**Evidência / anotações:** ______________________________________________

---

## 7. Permissões (perfis)

| ☐ | Teste | Esperado |
|---|-------|----------|
| ☐ | EMPLOYEE vende e consulta, mas **não** anula fatura nem faz sangria | Bloqueado com aviso |
| ☐ | MANAGER aprova operações sensíveis (desconto >10%, anulações) | Permitido |
| ☐ | ADMIN gere backup e utilizadores | Permitido |

---

## Veredito final

- ☐ **Todos os pontos ✅** → sistema **aceite para uso profissional** nesta loja.
- ☐ **Há ❌** → listar bloqueios e reavaliar após correção.

**Bloqueios em aberto:** ______________________________________________

**Assinatura (Responsável):** _______________________   **Assinatura (Conferido por):** _______________________
