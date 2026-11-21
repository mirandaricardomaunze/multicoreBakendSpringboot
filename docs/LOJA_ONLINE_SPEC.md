# Spec — Loja online

Status: **aprovada**
Effective date: 2026-11-19
Harness: [LOJA_ONLINE_HARNESS.md](LOJA_ONLINE_HARNESS.md)
Goal: [GOAL-016](../tasks/goals/GOAL-016-loja-online.md)

## 1. Problema

Os clientes da loja querem ver o que há e encomendar pelo telemóvel, e levantar ou receber em casa.
Hoje não há montra pública: só o desktop dentro da loja.

## 2. Decisões do responsável (2026-11-19)

| Tema | Decisão |
|---|---|
| Pagamento | **Na entrega ou no levantamento** (dinheiro, M-Pesa ou cartão no POS). Sem pagamento online. |
| Conta do cliente | **Sem conta**: nome, telemóvel e NUIT opcional; consulta por número da encomenda + telemóvel. |
| Entrega | **Levantamento** numa loja à escolha (grátis) ou **entrega ao domicílio** com taxa fixa por zona. |
| Stock | Mostra só **Disponível / Esgotado**; esgotado não se encomenda; a encomenda reserva stock. |

## 3. Regras

### 3.1 Activar a loja

- Por empresa, o ADMIN activa a loja online e define: **código público** da loja (minúsculas,
  números e hífen, único no sistema, ex.: `mercearia-baixa`), lojas/armazéns de levantamento,
  zonas de entrega (nome + taxa ≥ 0), telefone de contacto e prazo de cancelamento automático
  (dias, omissão **7**).
- Artigos aparecem só se activos e com **"Publicar na loja online"** ligado (novo campo, desligado
  por omissão). Imagem, nome, categoria e preço com IVA vêm do artigo.
- Endereço público: `/loja/{código}`; loja desactivada responde "Loja indisponível".

### 3.2 Montra

- Página pública servida pelo backend (mesmas regras técnicas do portal: sem CDN, sem script
  inline, CSP, `textContent`, a partir de 360 px), com catálogo por categoria, pesquisa e carrinho
  guardado no `localStorage` do browser (só SKUs e quantidades).
- Por artigo: **Disponível** se há stock disponível (stock − reservas) em pelo menos uma loja de
  levantamento da empresa; senão **Esgotado**. Nunca mostra quantidades, custos nem fornecedores.
- Endpoints públicos `/api/shop/{código}/**` (sem login), só de leitura do catálogo e criação/consulta
  de encomendas, com limite por IP (**60 pedidos/min**; **5 encomendas/hora**) e campo-armadilha
  (honeypot) contra robôs.

### 3.3 Encomendar

- Checkout: nome\*, telemóvel moçambicano\* (validado como no pagamento móvel), NUIT opcional,
  modo (`LEVANTAMENTO` numa loja escolhida, ou `ENTREGA` com zona e morada\*), notas.
- A encomenda é criada pelo **mesmo serviço da API pública** (`docs/API_PUBLICA_WEBHOOKS_SPEC.md`
  §3.4): preços do sistema, stock disponível, reserva, cliente por NUIT ou Consumidor Final com o
  contacto guardado na encomenda. Origem `LOJA_ONLINE`.
- O armazém da encomenda é a loja de levantamento; na entrega, a primeira loja de levantamento com
  todo o stock disponível, senão é recusada com "Não há stock suficiente para entrega".
- Entrega acrescenta uma linha de serviço **"Taxa de entrega — {zona}"** com a taxa da zona (artigo
  de serviço criado automaticamente na activação, IVA da taxa normal da empresa).
- Artigo esgotado entretanto → recusa com a lista dos artigos sem stock; nada é criado.
- Resposta: número da encomenda e resumo; o cliente consulta o estado com número + telemóvel
  (telemóvel errado → "Encomenda não encontrada", sem revelar se existe).

### 3.4 Na loja

- A encomenda aparece no desktop como qualquer outra, com a marca "Loja online", o contacto, o modo
  e a zona, e um alerta no sino dos perfis de vendas.
- Factura-se e cobra-se no levantamento/entrega pelos meios normais (POS ou factura-recibo).
- Encomenda online não facturada ao fim do prazo de cancelamento é **cancelada automaticamente**,
  libertando a reserva, com auditoria.

## 4. Fora de âmbito

- Pagamento online (M-Pesa, cartão), contas de cliente, cupões e promoções na loja online.
- Domínio próprio por empresa (o endereço é `/loja/{código}` no servidor).
- Envio de SMS/WhatsApp ao cliente.
