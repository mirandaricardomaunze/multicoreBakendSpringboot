package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.currency.dto.BaseCurrencyDTO;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Harness de UI da moeda estrangeira (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.5).
 * FX-UI-03: Configurações têm a tabela de câmbios (data, moeda, câmbio) e a moeda base da empresa,
 * só de leitura depois de haver movimentos.
 */
class ForeignCurrencyUiHarnessTest {

    @AfterEach
    void clearSession() throws Exception {
        // The session is thread-local and the tests set it on the EDT.
        onEdt(CurrentUserContext::clear);
    }

    @Test
    @DisplayName("FX-UI-03: Configurações ganham o separador de câmbios e moeda base, sem mudar a ordem das outras abas")
    void fxUi03_configuracoesTemSeparadorDeCambios() throws Exception {
        onEdt(() -> {
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null, null,
                    mock(ExchangeRateApiClient.class));

            JTabbedPane tabs = firstOf(config, JTabbedPane.class);
            int index = tabs.indexOfTab("Câmbios e Moeda Base");
            assertThat(index).as("separador \"Câmbios e Moeda Base\"").isGreaterThanOrEqualTo(0);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(ExchangeRatesPanel.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.getTitleAt(0)).isEqualTo("Log de Auditoria Geral");
            assertThat(tabs.getTitleAt(5)).isEqualTo("A Minha Assinatura");
        });
    }

    @Test
    @DisplayName("FX-UI-03: A tabela de câmbios mostra data, moeda e câmbio com 4 casas, dentro de um card")
    void fxUi03_tabelaDeCambiosMostraDataMoedaECambio() throws Exception {
        onEdt(() -> {
            ExchangeRatesPanel panel = new ExchangeRatesPanel(mock(ExchangeRateApiClient.class));
            panel.applyRates(List.of(
                    new ExchangeRateDTO(2L, "USD", LocalDate.of(2026, 11, 20), new BigDecimal("63.9055")),
                    new ExchangeRateDTO(1L, "ZAR", LocalDate.of(2026, 11, 19), new BigDecimal("3.5"))));

            JTable table = panel.ratesTable();
            assertThat(table.getColumnCount()).isEqualTo(3);
            assertThat(table.getColumnName(0)).isEqualTo("Data");
            assertThat(table.getColumnName(1)).isEqualTo("Moeda");
            assertThat(table.getColumnName(2)).isEqualTo("Câmbio");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).isEqualTo(2);
            assertThat(model.getValueAt(0, 0)).isEqualTo("20/11/2026");
            assertThat(model.getValueAt(0, 1)).isEqualTo("USD");
            assertThat(model.getValueAt(0, 2)).isEqualTo("63,9055");
            assertThat(model.getValueAt(1, 2)).isEqualTo("3,5000");
            assertThat(model.isCellEditable(0, 2)).as("corrigir só pelo diálogo").isFalse();
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("FX-UI-03: Moeda base fica só de leitura depois de haver movimentos")
    void fxUi03_moedaBaseSoLeituraComMovimentos() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            ExchangeRatesPanel panel = new ExchangeRatesPanel(mock(ExchangeRateApiClient.class));

            panel.applyBaseCurrency(new BaseCurrencyDTO("MZN", false));
            assertThat(panel.baseCurrencyCombo().getSelectedItem()).isEqualTo("MZN");
            assertThat(panel.baseCurrencyCombo().isEnabled()).isFalse();
            assertThat(panel.saveBaseCurrencyButton().isEnabled()).isFalse();
            assertThat(panel.baseCurrencyStatus().getText()).contains("Só leitura");

            panel.applyBaseCurrency(new BaseCurrencyDTO("MZN", true));
            assertThat(panel.baseCurrencyCombo().isEnabled()).isTrue();
            assertThat(panel.saveBaseCurrencyButton().isEnabled()).isTrue();
            assertThat(panel.baseCurrencyCombo().getItemCount()).as("lista fechada MZN, USD, ZAR, EUR").isEqualTo(4);
        });
    }

    @Test
    @DisplayName("FX-UI-03: Criar e corrigir câmbios e mudar a moeda base só para ADMIN (SPEC §3.2)")
    void fxUi03_soAdminAlteraCambiosEMoedaBase() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            ExchangeRatesPanel panel = new ExchangeRatesPanel(mock(ExchangeRateApiClient.class));
            panel.applyBaseCurrency(new BaseCurrencyDTO("MZN", true));

            assertThat(panel.newRateButton().isEnabled()).isFalse();
            assertThat(panel.correctRateButton().isEnabled()).isFalse();
            assertThat(panel.baseCurrencyCombo().isEnabled()).isFalse();
            assertThat(panel.saveBaseCurrencyButton().isEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("FX-UI-03: Ícones pelo UIHelper.icon e diálogo canónico para criar/corrigir câmbio")
    void fxUi03_iconesEDialogoCanonico() throws Exception {
        String panel = Files.readString(source("ExchangeRatesPanel.java"));
        String dialog = Files.readString(source("ExchangeRateFormDialog.java"));
        assertThat(panel).contains("UIHelper.icon(").doesNotContain("JOptionPane");
        assertThat(dialog).contains("new ModernFormDialog(").contains("setOnSaveAsync(")
                .contains("exchangeRateApiClient.create(").contains("exchangeRateApiClient.correct(")
                .doesNotContain("JOptionPane");
    }

    private static Path source(String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "currency", file);
        return Files.exists(nested) ? nested : Path.of("desktop").resolve(nested);
    }

    private static <T extends Component> T firstOf(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = firstOf(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() instanceof Exception exception) throw exception;
    }
}
