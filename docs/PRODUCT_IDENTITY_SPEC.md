# Spec — Identidade técnica própria do Multicore

**Data:** 2026-08-16

## Objectivo

Garantir que o produto, o instalador e as actualizações usam exclusivamente a identidade Multicore,
sem nomes herdados de produtos externos em pacotes, caminhos, ficheiros, configurações ou textos.

## Namespace canónico

- Java: `mz.multicore.erp`
- Preferências desktop: `mz/multicore/erp`
- Maven: `mz.multicore.erp`
- Entrypoint backend: `mz.multicore.erp.MulticoreApplication`
- Entrypoint desktop: `mz.multicore.erp.desktop.DesktopApplication`

## Regras

1. Código principal e testes devem residir sob `src/*/java/mz/multicore/erp`.
2. Scripts, Docker, Maven e documentação devem usar os entrypoints canónicos.
3. Preferências novas do Windows devem ser gravadas apenas sob a identidade Multicore.
4. Nenhum nome de ficheiro, pasta ou conteúdo versionável pode conter a marca externa anterior.
5. O Harness de identidade deve impedir regressões antes da criação do instalador.

