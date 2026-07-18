# Harness — Cabeçalho compacto do POS

> Cenários de validação manual da [spec](POS_CABECALHO_COMPACTO_SPEC.md). É layout/Swing —
> verificação manual. Critério técnico: `mvn clean compile` + app arranca.

**Última actualização:** 2026-06-29

| ID    | Passos                                                                       | Esperado                                                                                          |
|-------|------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| CH-01 | Abrir POS › **Venda POS**                                                    | Cabeçalho numa **única linha**: Cliente · Armazém Expedição · Conta Tesouraria · Código de barras. |
| CH-02 | Olhar para o campo de código de barras                                       | Está na **mesma linha** que os selects, com ícone `fas-barcode` **dentro** do input (38px).        |
| CH-03 | Comparar com a versão anterior (altura do catálogo)                          | O grid de cards de produtos está **mais alto/visível** (ganhou a linha do antigo scanner).         |
| CH-04 | Ler/escrever um código de barras válido e premir **Enter**                   | Artigo adicionado ao carrinho (`handleBarcodeScan` intacto); campo limpa e mantém foco.            |
| CH-05 | Escrever na **pesquisa de cliente** (nome/NUIT)                              | O combo de Cliente filtra como antes; **+ Novo** continua a abrir o cadastro rápido.               |
| CH-06 | Redimensionar a janela / arrastar o divisor do workspace                     | Cliente é mais estreito que antes; nenhum campo do cabeçalho parte para 2.ª linha.                 |
| CH-07 | Selecção de Armazém Expedição e Conta Tesouraria                             | Combos funcionam; selecção lida no checkout sem regressão.                                          |
| CH-08 | Fluxo completo: abrir caixa → adicionar por card e por código → finalizar    | Sem regressão; venda conclui e carrinho volta ao *empty state*.                                    |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
