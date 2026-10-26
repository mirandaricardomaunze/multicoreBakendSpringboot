# Especificação Técnica de Decomposição: MainFrame (Fase 91)

## 1. Contexto e Diagnóstico

A janela principal da aplicação Swing desktop, `MainFrame.java`, constitui o contentor raiz do ERP para a sessão do utilizador autenticado.
Antes da Fase 91, atingia **991 linhas de código**, agregando:
1. Gestão e renderização do sino de notificações na barra superior, popups de pré-visualização assíncrona e sincronização bidirecional de badges (sino e sidebar).
2. Construção da barra superior (`TopNavBar`) com alternância de tema, botão de histórico de itens recentes, seletor de empresa e chip do operador ativo com logout.
3. Construção do índice de pesquisa global (Omnibar / Ctrl+K) para superadmin, operadores e vendedores (SELLER).
4. Inicialização de painéis e registo no `CardLayout`.

## 2. Objectivos da Refacção (Fase 91)

- Reduzir `MainFrame.java` para $\le 750$ linhas (redução de mais de 240 linhas).
- Extrair `MainFrameNotificationCoordinator.java` ($\le 150$ linhas) para orquestração isolada do sino de notificações, pré-visualização em menu popup, e recálculo assíncrono de itens não lidos.
- Extrair `MainFrameTopBarBuilder.java` ($\le 130$ linhas) para montagem limpa dos componentes da barra superior (`TopNavBar`, chip de utilizador, botões de ação de tema, histórico e sobre).
- Preservar rigorosamente 100% das asserções de `GlobalOmnibarHarnessTest`, `SellerExperienceHarnessTest`, `SellerLeanStartupHarnessTest`, `RadarAlertasProactivoHarnessTest`, `DesktopThinContextTest` e `MultiModuleArchitectureHarnessTest`.
- Garantir zero `JOptionPane` em todos os ficheiros.

## 3. Arquitectura dos Componentes Decompostos

```
mz.multicore.erp.gui
 ├── MainFrame.java                          (Janela principal e orquestrador de cartões <= 750 linhas)
 ├── MainFrameNotificationCoordinator.java   (Sino de notificações, popup assíncrono e badges <= 150 linhas)
 ├── MainFrameTopBarBuilder.java             (Construção de TopNavBar, chip de utilizador e utilitários <= 130 linhas)
 ├── MainFrameSidebarBuilder.java            (Construção da barra lateral retrátil executiva)
 ├── MainFrameCompanySelector.java           (Seletor corporativo de empresa multitenant)
 └── MainFrameSubscriptionCoordinator.java   (Vigia e chip de subscrição)
```

## 4. Garantia de Conformidade

- **Limites Físicos:** Todos os novos colaboradores $\le 150$ linhas, orquestrador $\le 750$ linhas.
- **Zero Regressões:** Testes unitários e de contexto passam a 100%.
