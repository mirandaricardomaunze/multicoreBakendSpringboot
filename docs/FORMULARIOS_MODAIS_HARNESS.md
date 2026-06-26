# Harness — Formulários em Modal Responsivo

> Cenários de validação manual da [spec](FORMULARIOS_MODAIS_SPEC.md). É UX/Swing — verificação
> manual (não automatizável em teste unitário). Critério técnico: `mvn clean compile` + app arranca.

**Última actualização:** 2026-06-26

| ID    | Passos                                                                   | Esperado                                                                 |
|-------|--------------------------------------------------------------------------|-------------------------------------------------------------------------|
| FM-01 | Abrir Comercial › Faturação                                              | Tabela **Faturas Recentes** ocupa o separador inteiro; topo com «Nova Fatura…». |
| FM-02 | Clicar «Nova Fatura…»                                                    | Abre modal centrado; formulário + linhas de rascunho com scroll se necessário. |
| FM-03 | Gravar sem itens / sem cliente                                           | Mensagem de erro; **modal permanece aberto**.                            |
| FM-04 | Adicionar linhas e Gravar                                                | Fatura emitida; modal fecha; lista recarrega.                            |
| FM-05 | Reduzir a janela / ecrã pequeno e reabrir o modal                       | Modal nunca excede ~90% do ecrã; aparece scroll vertical, sem cortar botões. |
| FM-06 | Compras › Faturas de Compra → «Registar Compra…»                        | Histórico a ecrã inteiro; form em modal; "— A crédito —" disponível.    |
| FM-07 | Compras › Encomendas a Fornecedor → «Nova Encomenda…»                   | Lista a ecrã inteiro; form em modal; criar encomenda fecha e recarrega. |
| FM-08 | Botão Gravar dos modais                                                  | Ícone vectorial (`fas-save`), **sem emoji**.                            |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # 155 testes, 0 falhas (sem regressões)
```
