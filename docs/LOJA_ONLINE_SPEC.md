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

### 3.5 No desktop (detalhe de §3.1 e §3.4; escrito na iteração 44, 2026-11-28)

**Entrega guardada na encomenda (LO-12).** Hoje a morada é validada no checkout mas não fica em lado
nenhum; o modo e a zona só se adivinham pela linha da taxa. A encomenda `LOJA_ONLINE` passa a guardar:

- `fulfilmentLabel` (≤ 120): `Levantamento — {nome da loja}` ou `Entrega — {nome da zona}`;
- `deliveryAddress` (≤ 255): a morada do checkout, só na entrega (nulo no levantamento).

Ficam na própria encomenda (como já ficam `origin`, `originName` e `contactPhone`), gravados pelo
serviço da API pública através de `ExternalOrderSource`; as encomendas do desktop e da API ficam nulas.
O `OrderDTO` passa a trazer `origin`, `originLabel` (`Desktop`, `API · {chave}`, `Loja online`),
`contactPhone`, `fulfilmentLabel` e `deliveryAddress`. Migração Flyway com o próximo número livre.

**Definições (LO-UI-01).** Configurações › Integrações ganha, depois de "Webhooks", a secção
**"Loja online"** (card `ModernPanel`, ícone pelo `UIHelper.icon`):

- campos: "Loja activa" (caixa), "Código público" com a pré-visualização do endereço `/loja/{código}`,
  "Lojas de levantamento" (uma caixa por armazém de venda da empresa), tabela "Zonas de entrega"
  (Nome, Taxa em MT) com "Adicionar zona" e "Remover zona", "Telefone de contacto" e
  "Prazo de cancelamento (dias)" com 7 por omissão; botão "Guardar";
- lê e grava por endpoints internos `GET`/`PUT /api/online-shop/settings` (só ADMIN, área classificada
  no `ApiRolePolicy`); as mensagens de erro são as do backend (LO-01), mostradas sem alteração;
- outro perfil não pede as definições e vê "Só o administrador gere a loja online.".

**Publicar artigo (LO-UI-02).** A ficha do artigo (Stock › artigo) ganha a caixa
**"Publicar na loja online"**, desligada por omissão e sempre desligada no artigo novo até ser gravado.
O estado actual vem do servidor (`GET /api/online-shop/products/{id}/published`); ao gravar, só quando a
caixa mudou, o desktop chama `PUT /api/online-shop/products/{id}/published` (auditado, LO-02). Quem pode
editar o artigo pode publicá-lo; a recusa do backend (ex.: Adiantamento) aparece sem alteração.

**Encomendas online (LO-UI-03).** Na lista de encomendas, a encomenda `LOJA_ONLINE` aparece com a marca
**"Loja online"** (coluna "Origem"; as do desktop não têm a marca e mantêm "Directa"/"Cotação {número}",
que a coluna já mostrava); o detalhe mostra "Contacto: {telemóvel}",
"Entrega: {fulfilmentLabel}" e, na entrega, "Morada: {deliveryAddress}". O sino dos perfis de vendas
(`SignedInUser.isSellerOrAbove()`) mostra uma linha por encomenda `LOJA_ONLINE` ainda por facturar e não
cancelada: tipo "Loja online", título "Encomenda da loja online por facturar", detalhe
"{número} — {cliente} · {fulfilmentLabel}", prioridade 2, abre o card das encomendas (`comercial`). Os outros
perfis não pedem a lista. A lista vem de `GET /api/online-shop/orders/unbilled` (empresa activa, da mais recente;
só leitura, SELLER e acima no `ApiRolePolicy`).

## 4. Fora de âmbito

- Pagamento online (M-Pesa, cartão), contas de cliente, cupões e promoções na loja online.
- Domínio próprio por empresa (o endereço é `/loja/{código}` no servidor).
- Envio de SMS/WhatsApp ao cliente.
