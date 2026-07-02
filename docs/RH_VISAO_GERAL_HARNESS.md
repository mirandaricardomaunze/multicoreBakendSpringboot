# Harness — Visão Geral do RH

> Cenários de validação manual da [spec](RH_VISAO_GERAL_SPEC.md). É UI/Swing — verificação manual.
> Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-29

| ID    | Passos                                                                        | Esperado                                                                                       |
|-------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| RV-01 | Abrir **Recursos Humanos**                                                    | Primeira aba é **Visão Geral** (ícone `fas-chart-pie`), com 6 cards e 2 gráficos.              |
| RV-02 | Observar os cards                                                             | Cartões com gradiente + ícone, valor em destaque e linha de detalhe (estilo Dashboard).        |
| RV-03 | Card **Colaboradores Ativos**                                                 | Mostra o nº de colaboradores `ACTIVE` e "de N no quadro".                                       |
| RV-04 | Emitir/processar recibos do mês corrente e voltar à Visão Geral               | **Massa salarial (mês)** = Σ líquido; sub mostra bruto e nº de recibos.                         |
| RV-05 | Card **INSS Patronal (mês)**                                                  | Σ `employerInss` do mês; sub com IRPS+INSS retido.                                              |
| RV-06 | Submeter um pedido de férias (fica `PENDING`)                                 | Card **Férias Pendentes** incrementa; sub mostra dias por decidir.                             |
| RV-07 | Registar uma falta no mês corrente (tipo `UNJUSTIFIED`)                       | Card **Faltas (mês)** conta a falta; sub mostra dias não justificados.                          |
| RV-08 | Submeter uma nota de despesa (fica `PENDING_APPROVAL`)                        | Card **Despesas Pendentes** incrementa; sub mostra valor por aprovar.                           |
| RV-09 | Gráfico **Massa salarial líquida (6 meses)**                                  | Barras por mês (até 6), rótulo `MM/yy`, valor compacto (k/M).                                   |
| RV-10 | Gráfico **Colaboradores por departamento**                                    | Uma barra por departamento (top 5), com o headcount de activos.                                 |
| RV-11 | Sem dados (empresa nova)                                                       | Cards a 0 / `0,00 MT`; gráficos mostram "Sem dados" sem erros.                                  |
| RV-12 | Confirmar que o **Dashboard** principal continua igual                        | Cards e gráficos do Dashboard inalterados (após extração de `KpiCard`/`SimpleBarChart`).        |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
