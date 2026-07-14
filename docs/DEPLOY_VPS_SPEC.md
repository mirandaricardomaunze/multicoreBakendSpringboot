# Deploy — Backend separado (VPS + Docker + PostgreSQL privado)

**Objetivo:** correr o backend Spring Boot (`com.phcpro.MulticoreApplication`) num VPS, com PostgreSQL
**privado** (nunca exposto à internet) e HTTPS automático, rumo ao desktop **cliente-fino** (só HTTPS).

**Modelo escolhido:** VPS + Docker; desktop **HTTPS-only primeiro** (a BD só abre aos balcões depois de
a migração da UI para HTTP estar completa — ver [§ Track B](#track-b--migração-para-cliente-fino)).

---

## Arquitetura do deploy

```
Internet ── 443 ──▶ [ Caddy (TLS) ] ──▶ [ backend:8080 ] ──▶ [ db:5432 (PostgreSQL) ]
                        publica            rede interna            rede interna
                     80/443 no host      não publicado          não publicado
```

Só o Caddy publica portas. `backend` e `db` vivem na rede interna do compose — o PostgreSQL **não**
tem `ports:`, logo é inacessível de fora do host. É o objetivo "BD 100% privada".

Ficheiros: [Dockerfile](../Dockerfile) · [docker-compose.yml](../docker-compose.yml) ·
[Caddyfile](../Caddyfile) · [.env.example](../.env.example)

---

## Pré-requisitos

1. **VPS** Linux (≥ 2 GB RAM recomendado para o build; 1 GB chega se fizeres `docker compose build`
   noutra máquina e usares uma imagem já publicada) com **Docker** + **Docker Compose v2**.
2. **Domínio** (ex.: `erp.exemplo.co.mz`) com registo **A** a apontar para o IP do VPS.
3. Portas **80** e **443** abertas no firewall do VPS. **5432 fechada** (não é preciso de fora).

---

## Passos

```bash
git clone <repo> multicore && cd multicore
cp .env.example .env
nano .env          # DOMAIN, DB_PASSWORD forte, e PG_MAJOR/POSTGRES_IMAGE = versão da tua BD

docker compose up -d --build
docker compose logs -f backend     # acompanhar o arranque + Flyway
```

No primeiro arranque o **Flyway** aplica `V1..V30` numa BD vazia (perfil `prod`: `validate` +
`baseline-on-migrate`). Confirmar no log `Successfully applied N migrations`.

Verificar (deve responder, mesmo que 401/404 — a app está viva):
```bash
curl -sS -o /dev/null -w "%{http_code}\n" https://$DOMAIN/api/auth/login
```

### Migrar dados de uma BD existente (opcional)

Se já tens dados (o ambiente atual usa **PostgreSQL 18** — mete `PG_MAJOR=18` e
`POSTGRES_IMAGE=postgres:18` no `.env`):
```bash
# no host antigo:
pg_dump -Fc -d multicore -f multicore.dump
# copiar para o VPS e restaurar no container db:
docker compose cp multicore.dump db:/tmp/multicore.dump
docker compose exec db pg_restore --clean --if-exists --no-owner -U multicore -d multicore /tmp/multicore.dump
```

---

## Backups

O backend corre `pg_dump` **dentro do container** (por isso a imagem inclui `postgresql-client`).
Config já ligada no compose:
- Agendado diário às **23:00**, retenção **30 dias** (perfil `prod`).
- Ficheiros em `./data/backups` no host (volume montado).
- **Copiar `./data/backups` para fora do VPS** (off-site) — um backup no mesmo disco não protege de
  perda do VPS.

---

## Checklist de HARDENING (obrigatório antes de abrir os 443 ao público)

| # | Item | Estado |
|---|------|--------|
| 1 | **Fechar o filtro Spring Security** — hoje é `permitAll()`; quem protege é o `SecurityInterceptor`. Para go-live: escrever um filtro que valide o token opaco (`AuthSessionService`) e popule o `SecurityContext`, e restringir `/api/**` a autenticado (deixando `/api/auth/login` público). **Não** fechar às cegas — parte-se a API. | ⬜ pendente |
| 2 | **Firewall**: só 80/443 abertas; 5432 fechada. O compose já mantém o PostgreSQL interno. | ⬜ |
| 3 | **Sessões em memória** (`AuthSessionService` usa um `Map`): uma só instância de backend. Escalar horizontalmente exige um store partilhado (Redis) — não agora. | ⬜ (aceite p/ 1 instância) |
| 4 | **DB_PASSWORD** forte e único; rotação documentada. | ⬜ |
| 5 | **Off-site** dos backups (`./data/backups`). | ⬜ |
| 6 | **Swagger / H2 console**: já desativados em `prod`. Confirmar que continuam off. | ✅ (perfil prod) |
| 7 | **Rate-limiting do login**: já existe (`LoginRateLimiter`, 5 tentativas / 15 min). | ✅ |
| 8 | **Desktop → `DESKTOP_API_BASE_URL=https://DOMAIN`** em cada balcão, **e** Track B concluída (desktop sem ligação direta à BD). | ⬜ depende de Track B |

---

## Track B — Migração para cliente-fino

O deploy acima corre já, mas o desktop **ainda liga diretamente ao PostgreSQL** para a maioria dos
ecrãs (só auth + parte do comercial passam por HTTP). Enquanto isso, a BD não pode fechar-se por
completo. A migração restante:

**Base já existente (reutilizar):** [DesktopApiClient](../src/main/java/com/phcpro/desktop/client/DesktopApiClient.java)
(get/post/put/delete com Bearer + `X-Company-Id`, tratamento de erro) e o padrão de
[ComercialApiClient](../src/main/java/com/phcpro/desktop/client/ComercialApiClient.java) /
[AuthApiClient](../src/main/java/com/phcpro/desktop/client/AuthApiClient.java).

**Falta migrar ~24 domínios** (um `@RestController` cada): inventory, inventory/counts,
inventory/transfers, pos, purchases, hr, finance, fiscal, crm, approvals, credit-notes, debit-notes,
documents, movimentos, product-categories, promotions, reports, print, support, subscription,
platform/{companies,users,subscriptions,support}.

**Receita por domínio (iteração fechável):**
1. **Gap de endpoints:** confirmar que o controller expõe tudo o que o painel chama no Service
   (muitos métodos de Service ainda não têm endpoint) → skill `phc-new-endpoint`.
2. **Cliente typed** `XxxApiClient` sobre o `DesktopApiClient` (espelha o `ComercialApiClient`).
3. **Painel** deixa de receber `XxxService` e passa a receber `XxxApiClient`; cada `service.m(...)`
   vira `apiClient.m(...)`. Os DTOs já são os mesmos (a API devolve os mesmos records).
4. **Teste** de contrato do cliente + validação viva do painel.
5. Quando **todos** os painéis estiverem migrados: remover a `DataSource`/JPA do contexto `desktop`,
   o desktop passa a `SPRING_PROFILES_ACTIVE` sem BD, e fecha-se o 5432.

**Sequência sugerida** (validar o padrão no mais simples primeiro, depois o fluxo de vendas que é o
que mais importa operacionalmente):
`product-categories` → `reports` → `inventory` → `pos` → `purchases` → `hr`/`fiscal` → restantes.

Cada domínio deve fechar com spec+harness curtos, como as outras iterações do projeto.
