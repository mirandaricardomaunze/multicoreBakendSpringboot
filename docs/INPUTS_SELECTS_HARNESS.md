# Harness — Inputs e selects profissionais

> Cenários de validação manual da [spec](INPUTS_SELECTS_SPEC.md). É UI/Swing — verificação manual.
> Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-29

| ID    | Passos                                                                       | Esperado                                                                                   |
|-------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| IS-01 | Abrir um formulário com campos (ex.: Novo Colaborador, Submeter Despesa)      | Campos com borda arredondada, padding uniforme e altura igual aos botões (38px).           |
| IS-02 | Clicar/Tab para um text field                                                | A borda **acende na cor de acento** (foco); ao sair, volta ao normal.                      |
| IS-03 | Focar um combo (select) e abrir o popup                                       | Combo com borda de foco a acento; **seta sem bevel 3D**, fundo a condizer com o campo.     |
| IS-04 | Abrir o popup de um combo e percorrer opções                                 | Opção destacada legível (fundo de selecção), restantes com fundo do campo.                 |
| IS-05 | Trocar para **tema claro** (Configurações) e reabrir um formulário           | Fundo/borda dos campos seguem o tema (sem borda escura fixa); foco continua a acento.      |
| IS-06 | Password field (ecrã de login) e text area (descrições/notas)                | Mesmo estilo e realce de foco dos restantes campos.                                        |
| IS-07 | Campos de pesquisa com ícone dentro (POS: cliente, produto, código de barras)| Mantêm o aspecto de caixa única; sem regressão visual.                                     |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
