# Spec — Portal web do gestor

Status: **aprovada**
Effective date: 2026-11-19
Harness: [PORTAL_GESTOR_HARNESS.md](PORTAL_GESTOR_HARNESS.md)
Goal: [GOAL-012](../tasks/goals/GOAL-012-portal-web-gestor.md)

## 1. Problema

O dono ou gestor não está sempre ao computador da loja. Quer ver no telemóvel como correm as vendas,
o que falta em stock e quem deve, e aprovar um desconto ou uma compra sem ir ao desktop.

## 2. Decisões do responsável (2026-11-19)

| Tema | Decisão |
|---|---|
| Tecnologia | **Portal web servido pelo backend** em `/portal`, HTML/CSS/JS sem framework nem build novo, adaptado ao telemóvel, instalável no ecrã inicial (PWA). |
| Funções | **Painel + aprovações**: ver indicadores e aprovar/rejeitar pedidos pendentes. Nada mais se altera. |
| Acesso | **Só ADMIN e MANAGER**, nas empresas a que já têm acesso. |
| Segurança | **2FA obrigatório**, sessão de **12 h** ou **30 min sem uso**, **só HTTPS** em produção, mesmo limite de tentativas do login actual. |

## 3. Regras

### 3.1 Entrada e sessão

- Mesmo endpoint de login (`/api/auth/login`) com o indicador de canal `PORTAL`.
- Perfil que não seja ADMIN nem MANAGER → recusado com mensagem PT-MZ, sem token.
- Conta sem 2FA activo → recusada: "Active a verificação em dois passos no programa da loja para
  usar o portal." O código 2FA é pedido como no desktop.
- O token de portal expira **12 h** depois do login ou **30 min** depois do último pedido, o que vier
  primeiro. Logout invalida-o.
- Tentativas falhadas contam para o mesmo limitador do login actual (`LoginRateLimiter`).

### 3.2 O token de portal só abre o portal

- Um token de portal só é aceite em: `/api/auth/**`, `/api/reports/store-dashboard`,
  `/api/approvals/**` e `/api/portal/**`. Qualquer outra rota responde **403**.
- Tokens do desktop continuam a funcionar como hoje (o portal não muda o desktop).
- Empresa escolhida no portal entre as do utilizador; os pedidos levam `X-Company-Id` e respeitam o
  isolamento de empresa existente.

### 3.3 Painel

- Para a empresa escolhida, com dados de `store-dashboard` e, se faltar algum indicador, de um
  endpoint `/api/portal/summary` só de leitura:
  - vendas de hoje e do mês, por loja/armazém;
  - caixas POS abertas (operador, loja, desde quando);
  - artigos com stock abaixo do mínimo (os 20 mais urgentes);
  - facturas vencidas: total e os 10 clientes com maior valor em atraso;
  - número de aprovações pendentes.
- Valores com `DisplayNumbers` (`1.460,00 MT`) e datas `dd/mm/aaaa`.
- Botão "Actualizar"; actualiza sozinho a cada 5 minutos com o ecrã aberto.

### 3.4 Aprovações

- Lista as pendentes de `/api/approvals/pending` com tipo, pedido por, valor e data.
- Aprovar ou rejeitar usa os endpoints existentes; rejeitar exige motivo (como no desktop).
- As mesmas regras de quem pode aprovar o quê do módulo de aprovações; o portal não as contorna.

### 3.5 Página e segurança web

- Ficheiros em `backend/src/main/resources/static/portal/`: `index.html`, CSS e JS próprios, sem CDN
  nem bibliotecas externas; `manifest.webmanifest` e ícone para instalar.
- Funciona a partir de 360 px de largura, sem scroll horizontal.
- Cabeçalhos em `/portal/**`: `Content-Security-Policy` sem `unsafe-inline` nem origens externas,
  `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`.
- O token fica em `sessionStorage` (nunca em `localStorage` nem em cookie legível); fechar o separador
  termina a sessão no browser.
- Em produção (perfil `prod`), pedido ao portal sem HTTPS (directo ou `X-Forwarded-Proto: https`)
  é recusado.
- Texto em PT-MZ; nenhum texto vindo da API é inserido como HTML (só `textContent`).

## 4. Fora de âmbito

- Criar ou alterar documentos, clientes, artigos, preços ou stock pelo portal.
- App nativa, notificações push, modo offline.
- Acesso de vendedores, caixas ou clientes finais.
