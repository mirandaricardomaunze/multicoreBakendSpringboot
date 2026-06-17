# UI Design System - Swing

Este documento define padroes visuais e de interaccao para o cliente desktop Swing.

## Principios

- UI deve ser operacional, densa e clara.
- O utilizador deve conseguir trabalhar rapido com teclado, tabelas e dialogos previsiveis.
- Nao duplicar estilos; usar helpers e componentes existentes.
- Operacoes longas nunca bloqueiam o EDT.

## Componentes base

Usar:

- `UIHelper` para cores, icones, tabelas, dialogs e estilos.
- `ModernButton` para botoes.
- `ModernPanel` para paineis.
- `ModernFormDialog` quando o formulario encaixa no padrao existente.

Evitar:

- `new Color(...)` ad-hoc em paineis.
- Emojis em botoes, tabs ou labels funcionais.
- Layouts que dependem de tamanhos magicos sem responsividade.
- Regras de negocio dentro de listeners Swing.

## Icones

Padrao:

```java
UIHelper.icon("fas-save", 14)
```

Regras:

- Usar Ikonli FontAwesome 5 Solid.
- Icone deve reforcar a accao: guardar, imprimir, procurar, apagar, aprovar.
- Botoes destrutivos devem ser visualmente distintos e confirmar quando houver risco.

## Formularios

- Labels em portugues de Mocambique.
- Campos seguem ordem do fluxo real de trabalho.
- Campos obrigatorios devem ser claros.
- Datas de input usam `yyyy-MM-dd` enquanto nao houver date picker oficial.
- Datas apresentadas usam `dd/MM/yyyy`.
- Validacao de regra fica no Service; UI apenas mostra mensagem clara.

## Tabelas

- Usar `DefaultTableModel` com `isCellEditable` definido.
- Aplicar `UIHelper.styleTable(table)`.
- Colunas monetarias alinhadas e formatadas.
- Quantidades com precisao consistente.
- Evitar carregar listas enormes sem filtro/paginacao quando o volume crescer.

## Dialogos

- Usar `UIHelper.createDialogForm(...)` quando aplicavel.
- Dialogos altos devem usar `UIHelper.makeDialogScrollable(...)`.
- Titulos: "Sucesso", "Erro", "Aviso", "Confirmar".
- Mensagens devem orientar a correccao, nao culpar o utilizador.

## Threading

- Chamadas demoradas, relatorios, PDFs, imports e operacoes remotas usam `SwingWorker`.
- UI deve mostrar estado de carregamento ou desactivar a accao durante execucao.
- Nunca actualizar componentes Swing fora do EDT.

## Separacao de responsabilidades

Painel Swing pode:

- Montar UI.
- Ler valores dos campos.
- Chamar Service ou, no futuro, Client HTTP.
- Apresentar DTOs.
- Mostrar mensagens.

Painel Swing nao pode:

- Calcular imposto, stock, salario ou totais oficiais.
- Persistir directamente.
- Chamar Repository.
- Decidir permissao sensivel sem Service.
- Conter regra que precise sobreviver a migracao para backend.
