# Spec — Variantes de artigo (SA-C04, F5)

**Data:** 2026-11-09 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

A loja de roupa tem a mesma camisa em 4 tamanhos e 3 cores; o estaleiro tem varão de 8, 10 e 12 mm; a padaria tem
o bolo em 3 sabores. Criar cada combinação à mão é lento, os códigos ficam inconsistentes e ninguém vê de relance
que tamanho e cor acabaram.

## Decisões

1. **Cada variante é um artigo normal** (código, preço, custo e stock próprios). POS, faturas, encomendas, stock e
   tabelas de preço não mudam.
2. **Família** (`product_families`): nome, prefixo do código (único na empresa), um ou dois atributos e o
   **artigo modelo**, de onde cada variante copia preço, custo, grosso, IVA, categoria, linha de negócio, unidade
   base e regras de stock e validade.
3. **Geração:** uma variante por combinação (`product_variants`). Código `PREFIXO-VALOR1-VALOR2` (só letras e
   números, sem acentos, até 12 caracteres por valor); nome `Família - Valor1 / Valor2`. Valores repetidos (sem
   contar maiúsculas) são ignorados. Máximo 200 variantes por família. Recusa se um código já existir.
4. **Acrescentar valores** cria só as combinações que faltam.
5. **Preço único** aplica o mesmo preço a todas as variantes; preços diferentes editam-se no artigo.

## API (`/api/variants`): leitura para todos, escrita só gestão

`GET /families?companyId=` · `POST /families` · `POST /families/{id}/values` · `PUT /families/{id}/price`.
Migração `V130__variantes_artigo.sql`: só tabelas novas.

## Interface

Módulo **Variantes** (capacidade `VARIANTS`, gestão): famílias à esquerda; à direita a **grelha** (linhas =
primeiro atributo, colunas = segundo) com o stock de cada combinação, a vermelho quando está a zero; o código e o
preço aparecem ao passar o rato.

## Testes

`VariantsTest`: limpeza de valores, combinações, códigos sem acentos e nomes; família com 6 variantes ao preço do
modelo, prefixo repetido recusado, stock na grelha, valores novos sem duplicar, preço único.
