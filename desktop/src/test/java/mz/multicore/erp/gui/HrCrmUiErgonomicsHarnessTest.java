package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HrCrmUiErgonomicsHarnessTest {

    private static String source(String relative) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui").resolve(relative));
    }

    @Test
    @DisplayName("CRM: Folhas de obra respeitam o limite de 3 botões com agrupamento em ActionMenuButton")
    void crmWorkSheetsToolbarAdheresToMaxThreeButtons() throws IOException {
        String crm = source("CRMPanel.java");
        assertThat(crm).contains("UIHelper.tableCardTop(\"Folhas de Obra Registadas\", wsBar,\n                moreBtn, billBtn, newWsBtn)");
        assertThat(crm).contains("Ações da Folha");
        assertThat(crm).contains("Imprimir PDF", "Corrigir", "Anular", "Tarifa/hora");
    }

    @Test
    @DisplayName("RH: Aba de Colaboradores, Recibos e Faltas têm no máximo 3 botões visíveis no cabeçalho")
    void hrPanelToolbarsAdhereToMaxThreeButtons() throws IOException {
        String hr = source("HRPanel.java");
        // Colaboradores
        assertThat(hr).contains("UIHelper.tableCardTop(\"Quadro de Colaboradores\", empBar,\n                consultationBtn, moreBtn, newBtn)");
        assertThat(hr).contains("Actualizar", "Alternar Detalhes", "Ver Perfil");
        assertThat(hr).contains("Editar Colaborador", "Evolução Salarial", "Documentos", "Saúde Ocupacional");
        // Recibos de Salário
        assertThat(hr).contains("UIHelper.tableCardTop(\"Recibos de Salário\", psBar,\n                documentsBtn, actionsBtn, newBtn)");
        assertThat(hr).contains("Documentos", "Operações");
        assertThat(hr).contains("Aprovar Recibo", "Marcar Pago", "Imprimir PDF", "Ficheiro de Pagamento");
        // Faltas
        assertThat(hr).contains("UIHelper.tableCardTop(\"Registo de Faltas\", absBar,\n                exportBtn, actionsBtn, newBtn)");
        assertThat(hr).contains("Ações da Falta");
        assertThat(hr).contains("Justificar", "Eliminar");
    }

    @Test
    @DisplayName("RH Férias e Ponto: Cabeçalhos têm no máximo 3 botões")
    void hrVacationsAndTimeSheetAdhereToMaxThreeButtons() throws IOException {
        String vac = source("HRVacationsPanel.java");
        assertThat(vac).contains("UIHelper.tableCardTop(\"Pedidos de Férias\", vacBar,\n                exportBtn, decisionBtn, newBtn)");
        assertThat(vac).contains("Decisão & Subsídio");
        assertThat(vac).contains("Aprovar", "Rejeitar", "Subsídio de Férias");

        String time = source("HRTimeSheetPanel.java");
        assertThat(time).contains("UIHelper.tableCardTop(\"Folha de Ponto\", filters,\n                moreBtn, closeBtn, entryBtn)");
        assertThat(time).contains("Configurar Acréscimos", "Exportar PDF");
    }

    @Test
    @DisplayName("RH Descontos, Passivos e Cessações: Cabeçalhos agrupados sem sobreposição")
    void hrSubPanelsAdhereToMaxThreeButtons() throws IOException {
        String ded = source("HRDeductionsPanel.java");
        assertThat(ded).contains("UIHelper.tableCardTop(\"Descontos, Adiantamentos e Empréstimos\", bar,");
        assertThat(ded).contains("refreshBtn, stopBtn, newDeductionBtn)");
        assertThat(ded).contains("Novo Desconto");
        assertThat(ded).contains("Adiantamento Salarial", "Empréstimo", "Desconto Recorrente");

        String liab = source("HRLiabilitiesPanel.java");
        assertThat(liab).contains("UIHelper.tableCardTop(\"Retenções por Entregar\", bar,");
        assertThat(liab).contains("refreshBtn, actionsBtn, deliverBtn)");
        assertThat(liab).contains("Apurar Período", "Valores Legais");

        String term = source("HRTerminationsPanel.java");
        assertThat(term).contains("UIHelper.tableCardTop(\"Cessações e Acertos Finais\", bar,");
        assertThat(term).contains("docsBtn, payBtn, newBtn)");
        assertThat(term).contains("Acerto PDF", "Certificado de Trabalho");
    }
}
