# Harness — Resiliência das ligações à base de dados

> Cenários para [RESILIENCIA_LIGACOES_SPEC.md](RESILIENCIA_LIGACOES_SPEC.md). São **manuais** — a
> falha só se reproduz com tempo/suspensão real, fora do alcance do CI.

**Última actualização:** 2026-07-02

## Cenários manuais

| ID    | Passos                                                                                             | Esperado |
|-------|----------------------------------------------------------------------------------------------------|----------|
| RL-01 | Arrancar o desktop; confirmar no log que o Hikari arranca sem erro e a app liga ao PostgreSQL.      | Arranque normal. |
| RL-02 | Deixar a app aberta e **ociosa > 15 min**; depois cadastrar/editar um produto.                     | Grava à primeira, sem erro nem atraso perceptível. |
| RL-03 | **Suspender/hibernar** a máquina algumas horas; retomar; **sem reiniciar a app**, editar um produto e faturar. | Opera normalmente; ligações mortas foram substituídas em silêncio. |
| RL-04 | Parar o serviço PostgreSQL 30 s e reiniciá-lo com a app aberta; tentar uma operação a seguir.       | A operação falha limpa se cair a meio, mas a **seguinte** já liga (pool recupera). |
| RL-05 | Observar o log durante um dia de uso.                                                               | Sem acumular `connection is closed` / falhas de persistência silenciosas. |

## Nota de verificação

- `mvn clean compile` e `mvn clean test` continuam verdes (mudança é só de configuração; o perfil
  `desktop`/`prod` não é activado nos testes, que correm em H2).
- A validação real (RL-02..RL-05) faz-se no PC de balcão — como os itens de hardware/restore do
  [RETAIL_STORE_HARNESS.md](RETAIL_STORE_HARNESS.md), não em CI.
