# Harness — Polish profissional do ecrã POS

> Cenários de validação manual da [spec](POS_UI_POLISH_SPEC.md). É UX/Swing — verificação manual
> (não automatizável em teste unitário). Critério técnico: `mvn clean compile` + app arranca.

**Última actualização:** 2026-06-27

| ID    | Passos                                                                  | Esperado                                                                            |
|-------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| PU-01 | Abrir POS › separador **Venda POS** (caixa fechada)                     | Secção **DOCUMENTO** (Cliente, Armazém, Conta) visível **sem fazer scroll**.        |
| PU-02 | Olhar para os campos de pesquisa de cliente e de produto               | Cada um tem **ícone de lupa** vectorial à esquerda; sem emoji.                       |
| PU-03 | Inspeccionar placeholders/labels do separador                          | Nenhum emoji (🔍); ícones via `UIHelper.icon`.                                       |
| PU-04 | Carrinho vazio                                                          | Mostra *empty state*: ícone de carrinho + "Carrinho vazio" + dica.                  |
| PU-05 | Abrir caixa, ler código de barras / adicionar artigo                   | Tabela substitui o *empty state*; **TOTAL A PAGAR** em destaque (≥24px) actualiza.  |
| PU-06 | Remover todas as linhas do carrinho                                     | Volta ao *empty state*; total a `0,00 MT`.                                           |
| PU-07 | Caixa fechada vs aberta                                                 | Linha de estado mostra cadeado **fechado/amarelo** vs **aberto/verde**.             |
| PU-08 | Fluxo completo: abrir caixa → adicionar → finalizar venda → recibo      | Sem regressão; venda conclui e carrinho volta ao *empty state*.                     |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões (155 testes)
```
</content>
