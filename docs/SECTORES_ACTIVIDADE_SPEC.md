# Spec — Sectores de actividade sem limitações (talho, padaria, estaleiro e outros)

**Data:** 2026-11-08
**Estado:** aprovada em 2026-11-08 ("desde que não cause erros no projecto já feito").
F1 feita: ver `UNIDADES_MEDIDA_SPEC.md`, `TABELAS_PRECO_SPEC.md` e `SECTORES_ACTIVIDADE_HARNESS.md`.
**Prompts de execução:** `docs/prompts/PROMPT_SECTORES_ACTIVIDADE.md` (por fase),
`docs/prompts/PROMPT_BALANCA_VENDA_PESO.md`, `docs/prompts/PROMPT_PRODUCAO_TRANSFORMACAO.md`

## 1. Princípio

O Multicore **não** ganha uma versão por sector. Ganha **capacidades genéricas** que qualquer
negócio pode ligar, e um **Perfil de Actividade** por empresa que liga as capacidades certas,
simplifica os menus e semeia valores iniciais (categorias, unidades, fichas técnicas modelo).

Consequências:

- Um talho que também faz enchidos, uma padaria que revende refrigerantes, um estaleiro que fabrica
  blocos: tudo funciona, porque as capacidades combinam-se.
- Nenhuma regra fiscal, de stock ou de caixa é duplicada por sector. Tudo passa pelos serviços de
  documentos, stock e tesouraria existentes (`MOVIMENTOS_COMERCIAIS.md`).
- Desligar uma capacidade esconde a UI, **nunca apaga dados**.

## 2. Catálogo de capacidades

Estado: **Existe** · **Parcial** · **Falta**.

| ID | Capacidade | Estado | O que já há / o que falta |
|---|---|---|---|
| SA-C01 | Perfil de Actividade da empresa | **Feito (F1)** | Um ou mais perfis por empresa em Configurações › Perfil de Actividade. Falta (F2+): esconder menus e semear dados por perfil |
| SA-C02 | Unidades de medida e conversões por produto | **Feito (F1)** | Catálogo, unidade base e unidades alternativas por produto (`UNIDADES_MEDIDA_SPEC`). Na fatura e na encomenda: Ctrl+U "Quantidade noutra unidade" (o documento grava a unidade base). Falta: compras e POS |
| SA-C03 | Tabelas de preço | **Feito (F1)** | Tabelas com escalões e validade, por cliente, aplicadas na fatura, encomenda e cotação (`TABELAS_PRECO_SPEC`). Falta (F2): preço por unidade alternativa e preço da tabela no carrinho do POS |
| SA-C04 | Variantes de artigo | Falta | Tamanho/cor; espessura de varão; gramagem; sabor. Um artigo-pai, stock e preço por variante |
| SA-C05 | Venda ao peso e balança real | **Driver feito (F3)** · `BALANCA_SERIE_SPEC` (porta série, configuração por posto) | Etiqueta EAN-13 lida. Falta driver da balança e o resto: ver `PROMPT_BALANCA_VENDA_PESO.md` |
| SA-C06 | Produção e transformação | Parcial | BOM e ordem simples. Falta receita versionada, rendimento, desmancha, cura, plano do dia: ver `PROMPT_PRODUCAO_TRANSFORMACAO.md` |
| SA-C07 | Material pago por levantar | **Feito (F2)** · `MATERIAL_POR_LEVANTAR_SPEC` | Cliente paga 200 sacos e levanta aos poucos. Saldo por linha de fatura, guias parciais contra a fatura, stock reservado, extracto do cliente |
| SA-C08 | Entregas e frete | Parcial · **frete por zona feito** (Ctrl+Alt+F, zona = serviço `FRT-`) | Frota tem manifestos e paragens. Falta frete por zona/viagem/peso como linha de serviço, agenda de entregas, prova de entrega (assinatura, foto, nome de quem recebeu) e devolução na obra |
| SA-C09 | Obras / projectos do cliente | Falta | Um empreiteiro, várias obras. Entregas, faturas, crédito, preços e extracto por obra |
| SA-C10 | Adiantamentos e sinais | **Feito** · `ADIANTAMENTOS_SPEC` (fatura de adiantamento com IVA, NC + compensação, saldo em 2101; V123) | Sinal de encomenda (bolo, carne para festa, blocos por medida), adiantamento de empreiteiro; recibo de adiantamento e abate na fatura final |
| SA-C11 | Consignação e venda em rota | Falta | Padaria entrega pão a revendedores, recolhe sobras e fatura o líquido; distribuidor de bebidas faz rota diária com carga e acerto |
| SA-C12 | Vasilhame e embalagens retornáveis | Falta | Grades e garrafas, botijas de gás, paletes de blocos. Conta-corrente de vasilhame por cliente, caução, devolução |
| SA-C13 | Encomendas com data e personalização | Parcial | Encomendas existem. Falta data/hora de entrega, ligação à produção, notas de personalização e imagem de referência |
| SA-C14 | Sobras e remarcação | **Remarcação feita (F3)** · `VALIDADES_REMARCACAO_SPEC` (regras por dias, promoção automática) | Promoções existem. Falta remarcação automática por idade do lote (pão de ontem, carne a 1 dia da validade) e fecho de dia com sobras |
| SA-C15 | Perecíveis, FEFO e alertas | **Feito (F3)** · `VALIDADES_REMARCACAO_SPEC` (quadro, abate por validade, perdas; V125) | `tracksExpiry` e lotes. Falta FEFO obrigatório na venda e na produção, alertas por dias e relatório de perdas por validade |
| SA-C16 | Compra com rendimento | **Desmancha feita (F3)** · `DESMANCHA_SPEC` (custo por kg, rendimento, perda; V124) | Compra de animal ou carcaça por peso, com rendimento esperado e real; custo por kg vendável |
| SA-C17 | Stock a granel e inventário por medição | **Feito (F2)** · `STOCK_GRANEL_SPEC` | Areia, brita, pedra em monte: inventário por medição em m³, tolerância de perda natural, ajuste justificado |
| SA-C18 | Báscula de camião | Falta | Pesagem de entrada e saída, tara do camião, talão de pesagem ligado à guia. Reaproveita o driver da SA-C05 |
| SA-C19 | Pagamentos e crédito | Parcial | Pagamentos mistos, crédito com limite, vales. Falta operador M-Pesa/e-Mola real (já registado em `tasks/current.md`) |
| SA-C20 | Contingência sem internet | Parcial | POS tem fila local (`POS_CONTINGENCIA_RESILIENCIA_SPEC`). Falta para guias de levantamento no estaleiro e apontamentos de produção |
| SA-C21 | Controlos anti-fraude por sector | Parcial | Auditoria e aprovações existem. Faltam relatórios de excepção específicos (ver §4) |
| SA-C22 | Restauração (mesas, comandas, cozinha) | Falta | Vertical própria, fase final |
| SA-C23 | Agenda e ordens de serviço (oficina, salão) | Falta | Vertical própria, fase final |
| SA-C24 | Farmácia | Falta | Regulado; só depois de parecer jurídico |

## 3. Problemas por sector e como se resolvem

### Talho e charcutaria

| Problema do dia-a-dia | Capacidade |
|---|---|
| Compra um boi ou uma carcaça por peso e não sabe quanto custa cada kg de bife | SA-C16, desmancha em SA-C06 |
| Os cortes saem de uma só peça e o custo tem de ser repartido | SA-C06 (co-produtos, repartição por valor) |
| A carne perde peso na câmara (desidratação) e o stock nunca bate | SA-C05 (quebras com motivo), SA-C15 |
| Operador escreve o peso à mão e engana | SA-C05 (peso estável obrigatório, peso manual com permissão e auditoria), SA-C21 |
| Preço por kg muda muitas vezes | SA-C03 (tabela com validade, histórico de preço) |
| Carne pré-embalada para a montra | SA-C06 reembalamento + etiqueta EAN-13 de peso variável |
| Encomenda para festa com sinal | SA-C10, SA-C13 |
| Carne a perder validade | SA-C14 remarcação, SA-C15 alertas |
| Restaurantes compram a crédito todas as semanas | SA-C03 preço de revendedor, SA-C19 crédito |

### Padaria e pastelaria

| Problema | Capacidade |
|---|---|
| Não sabe quanto pão fazer amanhã | SA-C06 plano do dia por histórico + encomendas |
| O preço da farinha sobe e a margem desaparece sem ninguém notar | SA-C06 custo teórico vs. real, alerta de margem |
| Fornada à noite, ninguém declara o que saiu do forno | SA-C06 modo chão de fábrica, apontamentos por fornada |
| Entrega pão a barracas e cantinas, recolhe sobras e cobra o resto | SA-C11 consignação e rota |
| Sobras do dia anterior | SA-C14 remarcação, quebra com motivo |
| Bolos de aniversário por encomenda, com sinal e foto | SA-C10, SA-C13 |
| Ingredientes com validade curta (fermento, natas) | SA-C15 FEFO na produção |
| Vende pão à unidade e ao saco de 10 | SA-C02, SA-C03 |

### Estaleiro e materiais de construção

| Problema | Capacidade |
|---|---|
| Cliente paga 200 sacos de cimento e levanta 20 por semana; ninguém sabe o saldo | SA-C07 |
| Vende cimento ao saco e à palete, varão à barra e ao kg, areia ao m³ e ao camião | SA-C02 |
| Empreiteiro tem várias obras e quer extracto por obra | SA-C09 |
| Preço diferente para empreiteiro, revendedor e particular | SA-C03 |
| Frete cobrado à parte conforme a zona | SA-C08 |
| Camião sai com mais material do que está na guia | SA-C08 conferência de carga, SA-C18 báscula, SA-C21 |
| Areia e brita em monte: inventário impossível de contar | SA-C17 |
| Fábrica de blocos no próprio estaleiro, com tempo de cura | SA-C06 cura/quarentena |
| Varão e chapa cortados à medida | SA-C06 transformação com sobra reaproveitável |
| Paletes de blocos têm de voltar | SA-C12 |
| Adiantamento do empreiteiro antes de começar a obra | SA-C10 |
| Internet cai e o camião tem de sair | SA-C20 guia em contingência |

### Outros sectores cobertos pelas mesmas capacidades

| Sector | Capacidades principais |
|---|---|
| Mercearia, minimercado, grossista | SA-C02, SA-C03, SA-C15, SA-C19 |
| Frutaria e hortícolas | SA-C05, SA-C14, SA-C15 |
| Ferragem e electricidade | SA-C02 (metro, rolo), SA-C04, SA-C09 |
| Distribuidor de bebidas, bottle store | SA-C11, SA-C12, SA-C08 |
| Revenda de gás | SA-C12 (botija com caução), SA-C08 |
| Roupa, calçado, acessórios | SA-C04 variantes |
| Loja agrícola e veterinária | SA-C02, SA-C15, SA-C04 |
| Reembalamento (arroz, açúcar, feijão) | SA-C06, SA-C05 etiquetas |
| Restauração e take-away | SA-C22 (fase final) |
| Oficina, salão, serviços | SA-C23 (fase final) |
| Farmácia | SA-C24 (depois de parecer jurídico) |

## 4. Controlos anti-fraude por sector (SA-C21)

Relatórios de excepção no ecrã e em PDF, com alerta no Painel Executivo:

- **Talho:** vendas com peso manual por operador; quebras acima da média; rendimento de desmancha
  abaixo do esperado.
- **Padaria:** produção declarada vs. farinha consumida; sobras acima do histórico; consignações sem acerto.
- **Estaleiro:** guias sem fatura; levantamentos acima do saldo pago (bloqueado); diferença entre
  báscula e guia; ajustes de inventário de granel acima da tolerância.
- **Geral:** descontos manuais, anulações e reaberturas de caixa por operador.

## 5. Regras invioláveis

1. Todos os documentos fiscais continuam a sair pelos serviços existentes (fatura, NC, ND, guia,
   recibo). Nenhuma capacidade cria documento fiscal paralelo.
2. Todo o movimento de stock passa por `StockMovementRegistrationService`; todo o dinheiro pelos
   ledgers de caixa e tesouraria.
3. Saldo por levantar (SA-C07), vasilhame (SA-C12) e consignação (SA-C11) são **contas-correntes
   auditáveis**: só mudam por documento, nunca por edição directa.
4. Quantidades em unidade base no domínio; a conversão é só na entrada e na apresentação.
5. A isenção ou taxa de IVA de cada artigo (por ex. bens de primeira necessidade) é decidida pelo
   responsável com o jurista, nunca assumida no código.
6. Cada capacidade tem SPEC, HARNESS, testes, paridade PostgreSQL e UI ao nível de Pedidos.

## 6. Fases

| Fase | Conteúdo | Porquê nesta ordem |
|---|---|---|
| F1 Fundação **(feita)** | SA-C01, SA-C02, SA-C03 | Tudo o resto depende de unidades e preços |
| F2 Estaleiro | SA-C07, SA-C08, SA-C09, SA-C10, SA-C17, SA-C20 (guias) | Maior volume de dinheiro e maior risco de perda |
| F3 Talho | Balança (SA-C05), SA-C16, desmancha, SA-C14, SA-C15 | Usa a balança e a desmancha |
| F4 Padaria | Centro de Produção (SA-C06), SA-C11, SA-C13 | Usa o Centro de Produção |
| F5 Retornáveis e variantes | SA-C12, SA-C04, SA-C18 | Abre bebidas, gás, roupa e báscula |
| F6 Anti-fraude | SA-C21 completo | Precisa dos dados das fases anteriores |
| F7 Verticais próprias | SA-C22, SA-C23, SA-C24 | Domínios novos, cada um com SPEC própria |

## 7. Decisões em aberto

1. Qual é o sector da loja piloto? Define se F2, F3 ou F4 vem logo a seguir a F1.
2. Fatura de adiantamento (SA-C10): que documento fiscal a AT-MZ exige. Jurista.
3. Material pago por levantar (SA-C07): o preço fica congelado no pagamento? Prazo máximo de levantamento?
4. Vasilhame (SA-C12): caução cobrada em dinheiro ou só em conta-corrente?
5. Consignação (SA-C11): a mercadoria entregue sai do stock na entrega ou só no acerto?
