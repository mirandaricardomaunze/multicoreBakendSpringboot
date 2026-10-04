# Especificação Canónica — Estabilização Funcional e Compatibilidade de Harnesses

**Código:** `SPEC-SFS-001`  
**Módulos:** `backend`, `desktop`  
**Data:** 2026-09-24

## 1. Objectivo

Fechar as regressões detectadas pela suite completa sem desfazer funcionalidades posteriores já
especificadas. Esta estabilização protege segurança multiempresa, autorização de áreas sensíveis,
filtros temporais, tabelas Swing, impressão e feedback visual.

## 2. Regras obrigatórias

### SFS-01 — Último administrador activo por empresa

- Uma empresa deve manter pelo menos um utilizador activo com papel `ADMIN` no acesso dessa empresa.
- Alterar o papel, desactivar o utilizador ou revogar o acesso não pode remover o último administrador
  activo.
- A decisão usa o papel por empresa (`app_user_companies.role`), não o papel global legado.
- O teste deve criar uma empresa isolada; não pode depender da quantidade de administradores dos
  dados de demonstração.

### SFS-02 — Áreas sensíveis permanecem fail-closed

- Auditoria forense e previsão de tesouraria exigem `MANAGER` ou `ADMIN`.
- Testes funcionais dessas áreas devem estabelecer explicitamente utilizador, papel e empresa.
- Utilizadores comuns e ausência de contexto continuam bloqueados.

### SFS-03 — Vocabulário temporal mais recente prevalece

`TableFilter.periodCombo()` conserva as oito opções de `FILTRO_PERIODO_UNIVERSAL_SPEC`: Todo o
período, Hoje, Ontem, Esta semana, Últimos 7 dias, Este mês, Últimos 30 dias e Este ano. Datas futuras
não pertencem a intervalos retrospectivos.

### SFS-04 — Matrizes executivas pequenas não são paginadas

Matrizes fixas de previsão de tesouraria e listas executivas limitadas pelo backend podem desactivar
a paginação automática. Depois de `displayForecast`, as linhas recebidas devem ficar imediatamente
visíveis.

### SFS-05 — Testes Swing respeitam modelo, vista e EDT

- Testes que seleccionam linhas filtradas devem concluir primeiro as actualizações pendentes do EDT.
- Índices da vista são convertidos para o modelo sempre que houver sorter/paginação.
- O estado vazio rico é um painel composto; o harness não assume que seja um `JLabel` directo.

### SFS-06 — Todo PDF passa pela pré-visualização

- Nenhum ecrã grava ou abre PDF directamente com `PdfFileSaver`.
- Ecrãs decompostos podem delegar a impressão a diálogos especializados, desde que estes chamem
  `PrintPreviewDialog`.

### SFS-07 — Feedback profissional nas funcionalidades novas

- Ficheiros introduzidos depois de `UI_FEEDBACK_PROFISSIONAL_SPEC` não podem acrescentar chamadas
  directas a `JOptionPane.showMessageDialog`, `showConfirmDialog` ou `showInputDialog`.
- Mensagens usam `ModernMessageDialog`, `ToastManager`, feedback inline ou `ModernFormDialog`.
- O inventário legado global não pode exceder 49 chamadas e continua a diminuir.

### SFS-08 — Vocabulário visual actual

- O POS conserva os rótulos operacionais actuais, incluindo `Fechar Caixa (Z)`.
- Mensagens e acções usam português de Moçambique; `Actualizar` é a grafia canónica da aplicação.

## 3. Critério de pronto

1. Harnesses focados de segurança, filtros, tabelas, impressão, POS e feedback passam.
2. `MultiModuleArchitectureHarnessTest` passa.
3. `mvn clean compile` passa.
4. `mvn test` termina sem falhas nem erros.

