# Harness — Vista Unificada de Movimentos

> Cenários verificáveis da [spec](MOVIMENTOS_UNIFICADOS_SPEC.md). Os automatizáveis vivem em
> `MovimentosServiceTest`; os manuais validam a UI desktop.

**Última actualização:** 2026-06-25

---

## Cenários automatizados (MovimentosServiceTest)

| ID    | Cenário                                                                 | Esperado                                                                 |
|-------|------------------------------------------------------------------------|-------------------------------------------------------------------------|
| MU-01 | Empresa com 1 de cada tipo (fatura, encomenda, NC, ND), sem filtros    | Devolve **4** movimentos, um por tipo, com nº/cliente/total correctos.  |
| MU-02 | Ordenação                                                              | Resultado vem por **data desc** (mais recente primeiro).                 |
| MU-03 | Filtro `query` por **nº** (substring, case-insensitive)               | Só os movimentos cujo nº contém o termo.                                 |
| MU-04 | Filtro `query` por **nome de cliente** (substring, case-insensitive)  | Só os movimentos desse cliente (inclui walk-in via `customerName`).      |
| MU-05 | Filtro de período `from`/`to` inclusivo                                | Inclui documentos exactamente em `from` e em `to`; exclui fora.          |
| MU-06 | `from`/`to`/`query` todos nulos/blank                                  | Devolve tudo, sem filtrar.                                               |
| MU-07 | Isolamento multi-tenant                                                | `requireCompany` rejeita empresa ≠ activa; só lista a empresa pedida.    |

## Cenários manuais (UI desktop)

| ID    | Passos                                                                  | Esperado                                                                 |
|-------|------------------------------------------------------------------------|-------------------------------------------------------------------------|
| MU-08 | Abrir Comercial › tab **Movimentos**                                    | Tabela lista os 4 tipos da empresa activa, ordenada por data desc.      |
| MU-09 | Escrever no campo **Pesquisar**                                         | Lista filtra instantaneamente por nº/cliente.                           |
| MU-10 | Preencher **De**/**Até** e **Aplicar**                                  | Lista restringe ao período; rodapé actualiza contagem e soma.           |
| MU-11 | Limpar filtros e **Aplicar**                                            | Volta a mostrar tudo.                                                    |

## Verificação

```
mvn clean test    # inclui MovimentosServiceTest (MU-01..MU-07)
```
