# Harness — Gestão de categorias de produto

> Validação da [spec](CATEGORIAS_GESTAO_SPEC.md). UX/Swing — verificação manual. Critério técnico:
> `mvn clean compile` + app arranca; `mvn test` sem regressões.

**Última actualização:** 2026-06-28

| ID    | Passos                                                                  | Esperado                                                                                  |
|-------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| CT-01 | Stock › tab **Categorias**                                              | Tabela com colunas Código · Nome · **Cor** · **Produtos** · Estado.                       |
| CT-02 | Observar a coluna Cor                                                   | Mostra uma **amostra** da cor (quadrado) + o hex; sem cor → contorno cinza e "—".         |
| CT-03 | Observar a coluna Produtos                                              | Número de produtos que usam cada categoria (0 quando nenhuma).                            |
| CT-04 | **Nova Categoria** → preencher código/nome, **Escolher…** cor          | `JColorChooser` abre; ao escolher, a **amostra** no diálogo actualiza.                     |
| CT-05 | Gravar                                                                  | Categoria criada; aparece na tabela com a cor escolhida; modal premium (ícone `fas-tags`). |
| CT-06 | Gravar sem código ou sem nome                                          | Erro "Código e nome são obrigatórios."; o modal **mantém-se aberto**.                      |
| CT-07 | Selecionar uma categoria → **Editar** → **Limpar** cor → Gravar         | Cor removida; coluna Cor passa a "—".                                                      |
| CT-08 | Selecionar → **Activar/Desactivar**                                     | Estado alterna entre Activa/Inactiva; categoria inactiva não aparece no cadastro de produto.|
| CT-09 | Escrever na **pesquisa** (código ou nome)                              | A tabela filtra as categorias correspondentes.                                            |
| CT-10 | Criar produto e escolher a categoria                                    | A categoria activa aparece no combo; a contagem de Produtos sobe ao reabrir a tab.        |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
