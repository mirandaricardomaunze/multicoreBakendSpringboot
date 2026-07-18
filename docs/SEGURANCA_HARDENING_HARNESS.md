# Harness — Endurecimento de segurança

Ver [SEGURANCA_HARDENING_SPEC.md](SEGURANCA_HARDENING_SPEC.md).

## Ao vivo (backend a correr contra PostgreSQL real)

| ID    | Cenário                                                        | Esperado                                              |
|-------|----------------------------------------------------------------|-------------------------------------------------------|
| SH-01 | `GET /api/platform/companies` **sem** token                    | **401** (Spring Security recusa, antes do interceptor) |
| SH-02 | `GET /api/platform/companies` com token **inválido**           | **401**                                               |
| SH-03 | Login `superadmin` → `GET /api/platform/companies` com o token | **200** + lista de empresas                           |
| SH-04 | `GET /actuator/health` (público)                               | **200** `{"status":"UP"}`, sem detalhes sensíveis     |
| SH-05 | `GET /actuator/env` (não exposto)                              | **não-2xx, sem fuga de dados** (só `health` exposto; a app devolve 500 genérico para caminhos não mapeados) |
| SH-06 | `POST /api/auth/login` (público) com credencial errada         | **400** com erro estruturado (não 401 do filtro)      |
| SH-07 | Painéis já migrados no desktop continuam a funcionar           | Sem regressão (enviam token → autenticam)             |

## Regressão

- `mvn -o compile` limpo.
- O desktop arranca e os 11 domínios migrados funcionam (ver DESKTOP_THIN_CLIENT_HARNESS TC-50..63).
