# Harness — Numeração por empresa

Ver [NUMERACAO_POR_EMPRESA_SPEC.md](NUMERACAO_POR_EMPRESA_SPEC.md).

## Automáticos — `DocumentNumberServiceTest`

| ID    | Cenário                                                      | Esperado                              |
|-------|--------------------------------------------------------------|---------------------------------------|
| NE-01 | 1.ª emissão de uma série numa empresa                        | cria sequência → `SÉRIE-ANO/1`        |
| NE-02 | Sequência existente incrementa                               | sem saltos (`/8` após `/7`)           |
| NE-03 | Séries diferentes na mesma empresa                           | contadores independentes              |
| NE-04 | `next()` sem empresa activa no contexto                      | lança                                 |
| NE-05 | Criação concorrente (unique violation → relê com bloqueio)   | continua sem duplicar                 |

## Manuais — multi-empresa + migração

| ID     | Cenário                                                                          | Esperado                                                            |
|--------|----------------------------------------------------------------------------------|--------------------------------------------------------------------|
| NE-50  | Empresa A emite fatura, depois Empresa B, depois A                                | A: FT/1, FT/2 (contínuo **dela**); B: FT/1 — sem saltos por empresa.|
| NE-51  | Arranque após V30 numa BD com dados                                              | Migração aplica; app arranca; validate OK; nenhum número repetido. |
| NE-52  | Cada empresa continua a sua numeração no ano seguinte                            | Nova sequência por (empresa, série, ano).                          |
