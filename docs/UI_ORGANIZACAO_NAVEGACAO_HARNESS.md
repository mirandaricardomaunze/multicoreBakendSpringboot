# HARNESS — Modernização da UI e Organização da Navegação

Complementa [UI_ORGANIZACAO_NAVEGACAO_SPEC.md](UI_ORGANIZACAO_NAVEGACAO_SPEC.md).

---

## 1. Cenários Automatizados

| ID | Cenário Automático | Comportamento Esperado |
|---|---|---|
| NAV-01 | `LoginDialog` inicializado | Campos utilizador e senha presentes com rótulos e acessibilidade definidos |
| NAV-02 | Clique no botão de alternância de senha no `LoginDialog` | Alterna `echoChar` entre `(char)0` e bullet; actualiza tooltip e nome acessível |
| NAV-03 | Clique no chip de conta demo `maria` | Preenche `maria` no utilizador e `password` na senha |
| NAV-04 | Deteção de Caps Lock no `LoginDialog` | Estado de aviso contextual refletido quando o Caps Lock é ativado |
| NAV-05 | `CollapsibleSidebar` com 4 categorias | Possui seções "OPERAÇÕES", "GESTÃO & CRM", "FISCAL & AUDITORIA" e "SISTEMA" |
| NAV-06 | `SidebarNavItem` em modo expandido vs recolhido | Largura adapta-se (220/240px vs 56/64px) e preserva tooltip acessível |
| NAV-07 | `SidebarNavItem` com badge de notificações | Exibe badge numérico quando valor > 0 |
| NAV-08 | Ativação de `SidebarNavItem` por teclado | Foco e tecla Enter/Espaço acionam o `onClick` do item |
| NAV-09 | Alternância de tema no `CollapsibleSidebar` e `SidebarNavItem` | Cores adaptam-se aos valores claros e escuros de `UIHelper` |
| NAV-10 | Limite de linhas dos painéis prioritários | `POSPanel.java` <= 1000 linhas e zero `new Color(...)` |

---

## 2. Comandos de Verificação Automatizada

```powershell
# Executar harness de navegação e organização da UI
mvn test -pl desktop "-Dtest=UiOrganizationNavigationHarnessTest"

# Executar suíte de decomposição e uniformidade visual da UI
mvn test -pl desktop "-Dtest=UiPanelDecompositionTest,FinalUiUniformityHarnessTest"

# Executar harness arquitetural multi-módulo
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"

# Compilação limpa do reactor completo
mvn clean compile
```

---

## 3. Critérios de Validação Manual (Windows)

| ID | Cenário Manual | Evidência Esperada |
|---|---|---|
| M-NAV-01 | Abertura do diálogo de login em resolução 1366×768 (escala 125% e 150%) | Janela e cartão íntegros, sem corte de botões ou scroll indesejado |
| M-NAV-02 | Digitação de senha com Caps Lock | Alerta amarelo/visual "Caps Lock ativado" aparece imediatamente |
| M-NAV-03 | Utilização de contas demo | Clique em "Maria" preenche credenciais e foco vai para o botão "Entrar" |
| M-NAV-04 | Alternância de visibilidade da senha | Senha revela texto em claro ao clicar no olho e volta a asteriscos no segundo clique |
| M-NAV-05 | Navegação pela barra lateral | Módulos abrem suavemente e item activo fica visualmente destacado |
| M-NAV-06 | Recolha da barra lateral (`Ctrl+B` ou botão) | Barra recolhe para 64px, liberando largura total para tabelas e POS; tooltips funcionam no hover |
| M-NAV-07 | Alternância de tema claro/escuro | Fundo da barra lateral, textos, ícones e linhas de separação mudam suavemente de contraste |
