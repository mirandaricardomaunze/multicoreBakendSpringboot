# Harness — Catálogo POS em cards com imagem

> Validação da [spec](POS_CATALOGO_CARDS_SPEC.md). UX/Swing — verificação manual. Critério técnico:
> `mvn clean compile` + app arranca; `mvn test` sem regressões; migração `V18` aplica em PostgreSQL.

**Última actualização:** 2026-06-28

| ID    | Passos                                                                     | Esperado                                                                                  |
|-------|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| PC-01 | Stock › Cadastrar Produto → escolher imagem                                 | Selector de ficheiro abre; pré-visualização mostra a imagem escolhida.                     |
| PC-02 | Gravar o produto com imagem                                                 | Produto criado; imagem persiste (`products.image_data` preenchido).                       |
| PC-03 | POS › separador Venda POS                                                   | Catálogo aparece como **grid de cards** (imagem/marcador + nome + preço).                  |
| PC-04 | Produto com imagem vs sem imagem                                            | Com imagem mostra o thumbnail; sem imagem mostra ícone de marcador (placeholder).         |
| PC-05 | Abrir caixa e **clicar** num card                                           | Artigo adicionado ao carrinho (qtd 1); total actualiza.                                    |
| PC-06 | Clicar **de novo** no mesmo card                                            | A quantidade da linha **incrementa** (merge), não cria 2ª linha.                           |
| PC-07 | Clicar num card **sem caixa aberta**                                        | Aviso "abra caixa"; nada é adicionado.                                                     |
| PC-08 | Escrever no campo de pesquisa de produto                                    | O grid filtra os cards por SKU/nome/código de barras/referência.                          |
| PC-09 | Selects Cliente/Armazém/Conta                                              | Aparecem numa **barra superior compacta** (em linha), não na coluna esquerda.             |
| PC-10 | Finalizar venda                                                            | Checkout funciona como antes (sem regressão de cálculo/IVA/promoções).                     |
| PC-11 | Leitor de código de barras                                                 | Continua a adicionar via o mesmo caminho (merge de quantidade).                            |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
# V18 aplica em PostgreSQL: coluna image_data presente em products
```
