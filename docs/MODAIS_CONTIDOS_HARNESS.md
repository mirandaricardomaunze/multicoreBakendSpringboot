# Harness — Modais contidos na janela principal

> Validação da [spec](MODAIS_CONTIDOS_SPEC.md). É UX/Swing — verificação manual. Critério técnico:
> `mvn clean compile` + app arranca; `mvn test` sem regressões.

**Última actualização:** 2026-06-27

| ID    | Passos                                                                       | Esperado                                                                       |
|-------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| MD-01 | Abrir um modal grande (Stock › Cadastrar Produto)                            | Abre **centrado** e totalmente **dentro** da janela principal; com scroll se alto. |
| MD-02 | Arrastar o modal para a direita/baixo, para lá da margem da janela           | O modal **encosta na margem** e não passa para fora da janela principal.        |
| MD-03 | Arrastar o modal para cima/esquerda, para lá da barra de topo                | Idem — fica preso dentro da janela principal.                                   |
| MD-04 | Reduzir a janela principal e reabrir o modal                                 | Modal nunca excede ~94% da janela; cabe e centra-se nela.                       |
| MD-05 | Abrir um `JOptionPane` (ex.: confirmação de impressão de recibo) e arrastar  | Também fica contido (a contenção é global, não por diálogo).                    |
| MD-06 | Diálogo de progresso ("A finalizar venda…")                                  | Centrado na janela; sem barra de título (não arrastável).                       |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # 157 testes, 0 falhas (sem regressões)
```
</content>
