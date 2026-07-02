# Spec — Resiliência das ligações à base de dados (Hikari)

> Garantir que o cliente desktop (PC de balcão que fica ligado o dia todo e pode hibernar/suspender)
> **recupera automaticamente** ligações mortas ao PostgreSQL, sem precisar de reiniciar a app.

**Última actualização:** 2026-07-02

## Problema

Observado em uso real: com a app aberta desde a manhã e a máquina em **suspensão/hibernação ~18h**,
o pool HikariCP mantinha ligações **mortas** (o servidor PostgreSQL já as tinha fechado). Ao retomar,
as operações de gravação **falhavam ou não persistiam até reiniciar a aplicação** (visto nos logs:
`Thread starvation or clock leap detected`, avisos do `HikariPool`). Num balcão de loja isto é
inaceitável — ninguém quer reiniciar o sistema a meio do dia.

## Decisão

Configurar o HikariCP para **detectar e substituir ligações mortas proactivamente**, em vez de
confiar só na validação no momento do empréstimo:

- **`keepalive-time`** — o Hikari sonda periodicamente as ligações **ociosas** (`Connection.isValid`)
  e descarta as que morreram, antes de serem entregues a uma operação. É o mecanismo desenhado
  exactamente para o cenário "máquina dorme / firewall corta ligações ociosas".
- **`max-lifetime`** — reforma ligações ao fim de um tempo (rotação), para nunca acumularem estado
  velho. Tem de ser **> `keepalive-time`**.
- **`connection-timeout`** curto — em vez de bloquear indefinidamente à espera de uma ligação morta,
  falha depressa e o pool abre uma nova.
- **`validation-timeout`** — tecto para a validação `isValid`.

Aplica-se ao perfil **`desktop`** (o caso real) e espelha-se no **`prod`** (mesmo motor PostgreSQL,
mesmo benefício). O driver PostgreSQL é JDBC4, por isso a validação usa `isValid()` — **não** é
preciso `connection-test-query`.

### Valores

| Definição | Desktop | Prod | Porquê |
|-----------|---------|------|--------|
| `keepalive-time` | 120000 (2 min) | 120000 | Sonda ligações ociosas; < max-lifetime. |
| `max-lifetime` | 600000 (10 min) | 600000 | Rotação de ligações. |
| `connection-timeout` | 10000 (10 s) | 10000 | Falha rápida em vez de pendurar a UI. |
| `validation-timeout` | 5000 (5 s) | 5000 | Tecto da validação. |
| `maximum-pool-size` | 5 | 20 (mantém) | Desktop é mono-utilizador. |
| `minimum-idle` | 1 | 5 (mantém) | Desktop não precisa de pool quente grande. |

## Não-objetivos

- Não trocar de pool nem de driver (continua HikariCP + PostgreSQL JDBC).
- Não mexer na lógica de negócio, Services, DTOs nem no schema.
- Não resolver perda de rede prolongada durante uma transacção em curso (essa falha, por design,
  devolve erro ao operador) — o âmbito é **ligações ociosas mortas após inactividade/suspensão**.
