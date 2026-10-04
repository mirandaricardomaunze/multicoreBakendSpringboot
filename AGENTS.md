# AI Instructions - Multicore ERP

Este ficheiro e a porta de entrada para qualquer agente de codigo neste projecto. Antes de alterar codigo, ler os documentos canonicos abaixo e seguir a ordem indicada.

## Ordem de leitura

1. [README.md](README.md) - stack, entrypoints, comandos e estrutura base.
2. [ARCHITECTURE.md](ARCHITECTURE.md) - camadas obrigatorias, SOLID e separacao backend/desktop.
3. [CONVENTIONS.md](CONVENTIONS.md) - naming, Lombok, DTOs, transaccoes, UI Swing e idioma.
4. [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) - ownership dos dominios e dependencias permitidas.
5. [docs/BUSINESS_FLOWS.md](docs/BUSINESS_FLOWS.md) - fluxos de negocio que nao podem ser quebrados.
6. [docs/DATABASE.md](docs/DATABASE.md) - regras de persistencia, tenant, migrations e integridade.
7. [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) - contratos REST, DTOs, erros e versionamento.
8. [docs/UI_DESIGN_SYSTEM.md](docs/UI_DESIGN_SYSTEM.md) - padroes de UI Swing.
9. [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) - expectativas de testes.
10. [tasks/current.md](tasks/current.md) - contexto operacional actual.

Se uma instrucao do utilizador colidir com estes documentos, perguntar antes de divergir.

## Regras nao negociaveis

- A arquitectura física obrigatória é o reactor Maven `contracts`, `backend`, `desktop`, conforme `docs/MULTI_MODULE_ARCHITECTURE_SPEC.md`.
- `desktop` depende apenas de `contracts` e comunica com o backend por HTTPS; nunca recebe JPA, Flyway, driver de BD, Repository ou Service.
- `backend` depende de `contracts`, é headless e nunca importa Swing, `gui` ou `desktop`.
- `contracts` é independente de Spring, JPA e Swing.
- Toda IA deve executar `MultiModuleArchitectureHarnessTest` quando alterar POMs, fronteiras, DTOs ou mover classes entre módulos.

- Controller nunca chama Repository directamente.
- Controller nunca recebe ou devolve Entity JPA.
- Service contem regras de negocio, validacoes semanticas e transaccoes.
- Repository contem apenas persistencia e queries.
- DTOs records em todas as fronteiras HTTP.
- Erros de negocio usam `BusinessRuleException` com mensagem clara para o utilizador.
- Injecao por construtor, campos `final`; nunca `@Autowired` em campo.
- Mensagens visiveis ao utilizador em portugues de Mocambique.
- Identificadores Java, pacotes, tabelas e colunas em ingles.
- UI Swing usa `UIHelper`, `ModernButton`, `ModernPanel` e `UIHelper.icon(...)` / `UIHelper.semanticIcon(...)`; abas e menus de acção usam cores semânticas vibrantes (nunca ícones cinzentos/`TEXT_LIGHT`); proibido usar emojis ou caracteres especiais Unicode (como ⭐, ✅, ❌) em qualquer elemento Swing (botões, abas, labels, cabeçalhos ou menus), pois o Java 2D no Windows renderiza retângulos/quadrinhos (`▯`); usar sempre ícones vetoriais FontAwesome; manter botão de logout visível e acessível no chip de utilizador e rodapé da barra lateral; tabelas e todos os seus filtros/pesquisa/seletores devem estar estritamente contidos dentro do card `ModernPanel(16)` (`BorderLayout.NORTH` para filtros e `CENTER` para tabela), proibido deixar filtros soltos ou em `topStack` externo; diálogos modais de registo/emissão devem validar campos em `ModernFormDialog.setOnSave` com foco automático sem fechar a janela, prevenindo a perda de linhas digitadas; no cabeçalho, evitar filas longas de botões que colidem com títulos (máx. 2-3 botões abertos, agrupando acções de linha seleccionada em `ActionMenuButton` com limite estrito de no máximo 5 opções por menu para não disparar `IllegalStateException`); ao alterar UI ou construtores de painéis, executar obrigatoriamente `DesktopThinContextTest` para garantir que o `MainFrame` instancia sem falhas no login; ao alterar DTOs/records em `contracts`, preservar construtores sobrecarregados retrocompatíveis; explicar diagnóstico e spec antes de alterar código de layout; gestão de embalagens segue o padrão internacional multinível (`Caixa → Embalagem → Unidade`) via `PackagingComposition` e `PackagingQuantity`; os cartões de KPI devem manter rigorosamente a mesma altura e proporção uniforme em todo o sistema (`KpiCard.STANDARD_CARD_HEIGHT = 96px`, `KpiCard.createGrid` e `KpiCard.createCard`), sendo expressamente proibido encolher cards de KPI para compensar espaço de tabelas (a recuperação de altura vertical deve ser feita consolidando filtros numa linha horizontal única e reduzindo espaçamentos internos); botões de navegação lateral (Page Up / Page Down via `TableNavigator`) e paginação (`ClientTablePagination`) nunca devem ter ícones brancos fixos ou fundos indistinguíveis sobre cartões claros ("tudo white"), devendo possuir contraste adaptativo ao tema (`Slate-700` em light mode), superfície/borda visíveis e hover destacado em `UIHelper.ACCENT_BLUE`.
- Dinheiro e quantidades usam `BigDecimal`, nunca `double` ou `float`.
- Antes de terminar uma alteracao de codigo, correr `mvn clean compile` quando possivel.

## Como trabalhar

1. Identificar o dominio dono da regra antes de editar.
2. Ler classes vizinhas no mesmo modulo e seguir o padrao existente.
3. Fazer a menor alteracao que preserve arquitectura.
4. Adicionar ou ajustar testes quando a regra de negocio muda.
5. Actualizar documentacao apenas quando uma decisao, fluxo ou contrato muda.

### Arranque e Reinício da Aplicação (Backend & Desktop no Windows)

Quando o utilizador pedir para abrir ou reiniciar a aplicação ("abre app", "reinicia app") ou antes de empacotar com `mvn package`:
1. **Verificar Processos Bloqueadores (`javaw`):** Garantir que nenhuma instância anterior de `javaw` está a bloquear o JAR:
   ```powershell
   Get-Process javaw -ErrorAction SilentlyContinue | Stop-Process -Force
   ```
2. **Verificar Backend:** Confirmar que `http://localhost:8080/actuator/health` responde `{"status":"UP"}` antes de abrir o desktop.
3. **Evitar GUI no Terminal de Background:** Não iniciar `java -jar multicore-desktop-1.0.0.jar` directamente dentro do shell de background do agente, pois não renderiza na sessão interativa do utilizador (`WinSta0\Default`).
4. **Lançamento Interativo Canónico:** Executar a tarefa interativa do Windows ou `scripts/run_gui.bat` via:
   ```cmd
   schtasks /create /tn "MulticoreERP" /tr "C:\Users\miran\Desktop\manager\scripts\run_gui.bat" /sc ONCE /st 23:59 /it /f
   schtasks /run /tn "MulticoreERP"
   ```
5. **Propriedades Obrigatórias do Desktop:** Sempre incluir `-Djava.awt.headless=false -Dspring.profiles.active=desktop`.

## Quando parar e perguntar

- A mudanca exige quebrar uma regra em [ARCHITECTURE.md](ARCHITECTURE.md).
- Ha duvida sobre regra fiscal, contabilistica, stock, pagamento, permissao ou auditoria.
- Um dado parece pertencer a mais de um modulo e o ownership nao esta claro.
- A solucao exige migracao de base de dados destrutiva.
- A alteracao muda comportamento de faturacao, POS, stock, caixa ou salarios.

## Definicao de pronto

- Codigo compila.
- Camadas continuam separadas.
- DTOs e mensagens seguem as convencoes.
- Regras criticas tem teste ou justificacao clara para nao ter.
- UI nao bloqueia o EDT em operacoes longas.
- `tasks/current.md` foi actualizado se uma fase de trabalho fechou.
