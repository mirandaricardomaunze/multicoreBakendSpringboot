# Harness — Validação SAF-T

Ver [FISCAL_SAFT_VALIDACAO_SPEC.md](FISCAL_SAFT_VALIDACAO_SPEC.md).

## Automáticos — `SaftXsdValidatorTest`

| ID    | Cenário                                              | Esperado                          |
|-------|------------------------------------------------------|-----------------------------------|
| SV-01 | XML conforme a XSD de exemplo                        | sem erros (válido)                |
| SV-02 | XML que viola a XSD (elemento em falta/errado)       | ≥1 erro reportado                 |

## Manuais — UI + XSD oficial

| ID     | Cenário                                                            | Esperado                                                        |
|--------|-------------------------------------------------------------------|----------------------------------------------------------------|
| SV-50  | `fiscal.saft.xsd-path` vazio → "Validar SAF-T"                    | Mensagem "XSD não configurada…" (falha segura).                |
| SV-51  | Apontar `fiscal.saft.xsd-path` a uma XSD válida e validar um mês  | "SAF-T válido face à XSD" ou lista de erros concreta.          |
| SV-52  | XSD oficial da AT-MZ + export real                                | Ajustar o export até validar sem erros (certificação). *(pendente da XSD oficial)* |
