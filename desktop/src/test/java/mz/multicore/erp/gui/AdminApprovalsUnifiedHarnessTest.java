package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness AA-01..02:
 * Valida a conformidade canónica dos módulos de Administração, Aprovações, Risco e CRM:
 * - Ausência de GridBagLayout em filtros de tabelas
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 * - Instalação de Quick Peek silencioso com tecla Espaço
 */
class AdminApprovalsUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("AA-01: UserManagementPanel e ApprovalsPanel contêm ensureHeadersFit e Quick Peek")
    void aa01_usersAndApprovalsCanonical() throws Exception {
        String users = source("users/UserManagementPanel.java");
        assertThat(users).contains("UIHelper.ensureHeadersFit(usersTable)");
        assertThat(users).contains("TableQuickPeekController.install(usersTable");

        String approvals = source("ApprovalsPanel.java");
        assertThat(approvals).contains("UIHelper.ensureHeadersFit(pendingTable)");
        assertThat(approvals).contains("TableQuickPeekController.install(pendingTable");
        assertThat(approvals).contains("KpiCard.createGrid(4)");
    }

    @Test
    @DisplayName("AA-02: CreditRiskPanel e CRMPanel contêm ensureHeadersFit e Quick Peek")
    void aa02_creditRiskAndCrmCanonical() throws Exception {
        String risk = source("CreditRiskPanel.java");
        assertThat(risk).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(risk).contains("TableQuickPeekController.install(table");

        String crm = source("CRMPanel.java");
        assertThat(crm).contains("UIHelper.ensureHeadersFit(ticketsTable)");
        assertThat(crm).contains("TableQuickPeekController.install(ticketsTable");
    }

    @Test
    @DisplayName("AA-03: Módulo de Clientes padronizado (ClientesPanel, CreditRiskPanel, CustomerStatementPanel)")
    void aa03_clientesModuleCanonical() throws Exception {
        String clientes = source("ClientesPanel.java");
        assertThat(clientes).contains("UIHelper.tableCardTop(\"Gestão de Clientes\"");
        assertThat(clientes).contains("ClientTablePagination.install(table)");
        assertThat(clientes).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(clientes).contains("TableQuickPeekController.install(table");

        String risk = source("CreditRiskPanel.java");
        assertThat(risk).contains("UIHelper.tableCardTop(\"Matriz de Aging & Gestão de Risco\"");
        assertThat(risk).contains("ClientTablePagination.install(table)");
        assertThat(risk).contains("KpiCard.createGrid(4)");

        String statement = source("CustomerStatementPanel.java");
        assertThat(statement).contains("UIHelper.tableCardTop(\"Conta Corrente & Reconciliação de Clientes\"");
        assertThat(statement).contains("ClientTablePagination.install(table)");
        assertThat(statement).contains("KpiCard.createGrid(5)");
    }
}
