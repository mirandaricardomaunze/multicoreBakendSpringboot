# Especificação de Decomposição: AboutDialog (Fase 202)

## Objectivo
Decompor o diálogo executivo "Sobre o Multicore ERP" `AboutDialog.java` (378 linhas) numa fachada enxuta que delega a construção das 4 abas temáticas (Autoria, Módulos, Arquitetura, Licenciamento) a `AboutDialogTabBuilder`.

## Colaborador Extraído
- `AboutDialogTabBuilder.java`: Construção das abas `buildAuthorTab`, `buildModulesTab`, `buildArchitectureTab` e `buildLicenseTab`.

## Invariantes Preservadas
- O método `AboutDialog.show(Component parent)` e todos os KPIs e informações de autoria permanecem 100% preservados.
