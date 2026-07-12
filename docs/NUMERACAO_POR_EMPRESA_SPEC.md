# Numeração de documentos por empresa (gapless por NUIT)

**Última actualização:** 2026-07-12
**Estado:** feito (código + migração V30 aplicada e validada em PostgreSQL real).

## Problema

A `DocumentSequence` numerava por **(série, ano)** de forma **global** — partilhada entre todas as
empresas. Como cada empresa é um **contribuinte distinto (NUIT próprio)**, isto criava **saltos** na
sequência de cada uma: se a Empresa A emitia e a B emitia no meio, A ficava com FT‑2026/1 e /3 (faltava
a /2, que foi da B). A lei/SAF-T exige numeração **contínua e sem saltos por contribuinte**.

## Solução

Numeração **por empresa**: a chave passa a **(empresa, série, ano)**.

- **Entidade** `DocumentSequence`: novo `companyId`; unique `uk_document_sequence_company_series_year`
  em `(company_id, series, doc_year)`.
- **Repositório**: `lockByCompanyAndSeriesAndYear(companyId, series, year)` (bloqueio pessimista).
- **`DocumentNumberService.next(series)`**: lê a **empresa activa** do `CurrentUserContext` (lança se
  em falta) e emite na sequência dessa empresa. **Os 11 chamadores não mudam** — continuam a chamar
  `next(SÉRIE)`; o serviço faz o scoping por empresa.
- **Migração `V30`**: adiciona `company_id`, **larga a unique global primeiro** (senão as cópias por
  empresa violam-na), semeia cada empresa no **máximo global** de cada série/ano (cutover seguro — nunca
  repete um número já emitido, cada empresa continua para cima), remove as linhas globais, cria a unique
  composta e põe `company_id NOT NULL`.

## Cutover (o que acontece aos números existentes)

- Documentos já emitidos **não mudam** (o número está gravado em cada documento).
- No arranque, cada empresa é semeada no **máximo global** de cada série/ano → o **próximo** número de
  cada empresa continua a partir daí. Empresas sem documentos começam a partir desse máximo (começam
  "alto", mas sem salto — não há números em falta antes do primeiro).
- Só corre em **PostgreSQL** (Flyway está desligado nos testes; validada pelo arranque real da app).

## Verificação

- `DocumentNumberServiceTest` (7): gapless por empresa, formato `SÉRIE-ANO/N`, séries independentes,
  falha sem empresa activa, corrida na criação concorrente.
- V30 aplicada em PostgreSQL 18 e app arranca com Hibernate `validate` OK.
