package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ComprasPanel;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.PurchasePayablesViewCoordinator;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.currency.dto.BaseCurrencyDTO;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Harness de UI da moeda estrangeira (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.5).
 * FX-UI-03: Configurações têm a tabela de câmbios (data, moeda, câmbio) e a moeda base da empresa,
 * só de leitura depois de haver movimentos.
 * FX-UI-01: moeda e câmbio nos formulários de encomenda e de compra; contas a pagar com a moeda.
 * FX-UI-02: pagamento a fornecedor com câmbio do pagamento e a diferença cambial mostrada antes de confirmar.
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

    @Test
    @DisplayName("FX-UI-01: Campo de moeda do documento — moeda base com câmbio 1, estrangeira com câmbio proposto e editável")
    void fxUi01_campoDeMoedaPropoeCambioEditavel() throws Exception {
        onEdt(() -> {
            DocumentCurrencyField field = new DocumentCurrencyField(null);
            assertThat(field.currencyCombo().getItemCount()).as("lista fechada MZN, USD, ZAR, EUR").isEqualTo(4);
            assertThat(field.currency()).isEqualTo("MZN");
            assertThat(field.isForeign()).isFalse();
            assertThat(field.rateField().isEnabled()).as("moeda base tem câmbio 1, sem edição").isFalse();
            assertThat(field.exchangeRate()).as("moeda base não envia câmbio").isNull();

            field.currencyCombo().setSelectedItem("USD");
            assertThat(field.isForeign()).isTrue();
            assertThat(field.rateField().isEnabled()).isTrue();

            field.applyProposal(new ProposedExchangeRateDTO("USD", LocalDate.of(2026, 11, 22),
                    new BigDecimal("63.9055"), LocalDate.of(2026, 11, 20), false));
            assertThat(field.rateField().getText()).isEqualTo("63,9055");
            assertThat(field.hint().getText()).contains("20/11/2026");
            assertThat(field.exchangeRate()).isEqualByComparingTo("63.9055");

            field.rateField().setText("64,1");
            assertThat(field.exchangeRate()).as("o câmbio proposto é editável").isEqualByComparingTo("64.1");

            field.applyProposal(new ProposedExchangeRateDTO("ZAR", LocalDate.of(2026, 11, 22), null, null, true));
            assertThat(field.rateField().getText()).as("proposta de outra moeda é ignorada").isEqualTo("64,1");

            field.currencyCombo().setSelectedItem("ZAR");
            field.applyProposal(new ProposedExchangeRateDTO("ZAR", LocalDate.of(2026, 11, 22), null, null, true));
            assertThat(field.rateField().getText()).isEmpty();
            assertThat(field.hint().getText()).contains("escreva o câmbio");
            assertThat(field.exchangeRate()).as("vazio: o backend propõe ou recusa").isNull();
        });
    }

    @Test
    @DisplayName("FX-UI-01: Documento gravado mostra a sua moeda e câmbio, sem edição; novo documento volta à moeda base")
    void fxUi01_documentoGravadoMostraMoedaBloqueada() throws Exception {
        onEdt(() -> {
            DocumentCurrencyField field = new DocumentCurrencyField(null);
            field.showDocument("EUR", new BigDecimal("70.12"));
            assertThat(field.currency()).isEqualTo("EUR");
            assertThat(field.rateField().getText()).isEqualTo("70,1200");
            assertThat(field.currencyCombo().isEnabled()).isFalse();
            assertThat(field.rateField().isEnabled()).isFalse();

            field.reset();
            assertThat(field.currency()).isEqualTo("MZN");
            assertThat(field.currencyCombo().isEnabled()).isTrue();
        });
    }

    @Test
    @DisplayName("FX-UI-01: Formulários de encomenda e de compra enviam a moeda e o câmbio escolhidos")
    void fxUi01_formulariosDeEncomendaECompraTemMoedaECambio() throws Exception {
        onEdt(() -> {
            ComprasPanel panel = new ComprasPanel(mock(PurchaseApiClient.class), mock(InventoryApiClient.class),
                    mock(ComercialApiClient.class), mock(FinanceApiClient.class), null, null);
            assertThat(panel.purchaseCurrencyField.getParent()).as("moeda no formulário da compra").isNotNull();
            assertThat(SwingUtilities.isDescendingFrom(panel.poCurrencyField, panel))
                    .as("moeda no formulário da encomenda").isTrue();
        });
        String purchase = Files.readString(gui("PurchaseInvoiceOperations.java"));
        String order = Files.readString(gui("PurchaseOrdersPanel.java"));
        assertThat(purchase).contains("owner.purchaseCurrencyField.currency(), owner.purchaseCurrencyField.exchangeRate()");
        assertThat(order).contains("owner.poCurrencyField.currency(), owner.poCurrencyField.exchangeRate()");
        assertThat(Files.readString(gui("PurchaseOrderOperationsCoordinator.java")))
                .as("encomenda gravada mostra a sua moeda").contains("poCurrencyField.showDocument(");
    }

    @Test
    @DisplayName("FX-UI-01: Contas a pagar mostram a moeda, a dívida na moeda do documento e em moeda base")
    void fxUi01_contasAPagarComMoeda() throws Exception {
        onEdt(() -> {
            ComprasPanel panel = new ComprasPanel(mock(PurchaseApiClient.class), mock(InventoryApiClient.class),
                    mock(ComercialApiClient.class), mock(FinanceApiClient.class), null, null);
            JTabbedPane tabs = firstOf(panel, JTabbedPane.class);
            JTable table = firstOf((Container) tabs.getComponentAt(tabs.indexOfTab("Contas a Pagar")), JTable.class);
            assertThat(table.getColumnName(3)).isEqualTo("Moeda");
            assertThat(table.getColumnName(6)).isEqualTo("Em Dívida");
            assertThat(table.getColumnName(7)).isEqualTo("Em Dívida (moeda base)");

            JLabel footer = new JLabel();
            JComboBox<String> units = new JComboBox<>();
            PurchasePayablesViewCoordinator.applyPayables(List.of(
                    new PayableDTO(1L, "V/FT 1", 9L, "Fornecedor USD", new BigDecimal("100.00"), BigDecimal.ZERO,
                            new BigDecimal("100.00"), LocalDateTime.of(2026, 11, 20, 10, 0), null, null,
                            "USD", new BigDecimal("63.9055"), new BigDecimal("6390.55"), new BigDecimal("6390.55")),
                    new PayableDTO(2L, "V/FT 2", 8L, "Fornecedor MZN", new BigDecimal("500.00"), BigDecimal.ZERO,
                            new BigDecimal("500.00"), LocalDateTime.of(2026, 11, 21, 10, 0))),
                    panel, units, footer);

            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getValueAt(0, 3)).isEqualTo("USD");
            assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("100.00");
            assertThat((BigDecimal) model.getValueAt(0, 7)).isEqualByComparingTo("6390.55");
            assertThat(model.getValueAt(1, 3)).as("compra antiga sem moeda = moeda base").isEqualTo("MZN");
            assertThat(footer.getText()).as("total a pagar soma em moeda base").contains("6.890,55");
        });
    }

    private static PayableDTO usdPayable(String amountPaid, String outstanding, String baseOutstanding) {
        return new PayableDTO(1L, "V/FT 1", 9L, "Fornecedor USD", new BigDecimal("100.00"), new BigDecimal(amountPaid),
                new BigDecimal(outstanding), LocalDateTime.of(2026, 11, 20, 10, 0), null, null,
                "USD", new BigDecimal("63.9055"), new BigDecimal("6390.55"), new BigDecimal(baseOutstanding));
    }

    @Test
    @DisplayName("FX-UI-02: Compra na moeda base paga como hoje, sem campo de câmbio")
    void fxUi02_compraNaMoedaBaseNaoPedeCambio() throws Exception {
        onEdt(() -> {
            PayableDTO mzn = new PayableDTO(2L, "V/FT 2", 8L, "Fornecedor MZN", new BigDecimal("500.00"),
                    BigDecimal.ZERO, new BigDecimal("500.00"), LocalDateTime.of(2026, 11, 21, 10, 0));
            PaymentExchangeRateField field = new PaymentExchangeRateField(mzn, null);
            assertThat(field.isForeign()).isFalse();
            assertThat(field.isVisible()).as("sem câmbio na moeda base").isFalse();
            assertThat(field.exchangeRate()).as("moeda base não envia câmbio").isNull();
        });
    }

    @Test
    @DisplayName("FX-UI-02: Pagamento em moeda estrangeira mostra câmbio editável e a diferença cambial antes de confirmar")
    void fxUi02_diferencaCambialVisivelAntesDeConfirmar() throws Exception {
        onEdt(() -> {
            PaymentExchangeRateField field = new PaymentExchangeRateField(usdPayable("0", "100.00", "6390.55"), null);
            assertThat(field.isForeign()).isTrue();
            assertThat(field.rateField().isEnabled()).isTrue();

            field.applyProposal(new ProposedExchangeRateDTO("USD", LocalDate.now(),
                    new BigDecimal("65"), LocalDate.of(2026, 11, 21), false));
            assertThat(field.rateField().getText()).isEqualTo("65,0000");
            assertThat(field.exchangeRate()).isEqualByComparingTo("65");

            field.updateAmount(new BigDecimal("100.00"));
            assertThat(field.difference().getText()).as("liquidação ao câmbio maior: perda")
                    .contains("6.500,00").contains("6.390,55").contains("Perda cambial").contains("6601")
                    .contains("109,45");

            field.rateField().setText("63");
            field.updateAmount(new BigDecimal("100.00"));
            assertThat(field.difference().getText()).as("câmbio menor: ganho")
                    .contains("Ganho cambial").contains("7601").contains("90,55");

            field.rateField().setText("63,9055");
            field.updateAmount(new BigDecimal("100.00"));
            assertThat(field.difference().getText()).contains("Sem diferença cambial");

            field.rateField().setText("");
            field.updateAmount(new BigDecimal("100.00"));
            assertThat(field.exchangeRate()).as("vazio: o backend propõe ou recusa").isNull();
        });
    }

    @Test
    @DisplayName("FX-UI-02: Pré-visualização segue a regra do backend — parcial proporcional, liquidação sem resíduos")
    void fxUi02_previsaoSegueRegraDoBackend() {
        PaymentExchangeRateField.Preview partial = PaymentExchangeRateField.preview(
                usdPayable("0", "100.00", "6390.55"), new BigDecimal("40.00"), new BigDecimal("65"));
        assertThat(partial.treasuryAmount()).isEqualByComparingTo("2600.00");
        assertThat(partial.supplierReduction()).isEqualByComparingTo("2556.22");
        assertThat(partial.difference()).isEqualByComparingTo("43.78");

        PaymentExchangeRateField.Preview settle = PaymentExchangeRateField.preview(
                usdPayable("40.00", "60.00", "3834.33"), new BigDecimal("60.00"), new BigDecimal("63"));
        assertThat(settle.supplierReduction()).as("liquidação baixa o que restava").isEqualByComparingTo("3834.33");
        assertThat(settle.treasuryAmount()).isEqualByComparingTo("3780.00");
        assertThat(settle.difference()).isEqualByComparingTo("-54.33");
    }

    @Test
    @DisplayName("FX-UI-02: O diálogo de pagamento usa o campo de câmbio e envia o câmbio do pagamento")
    void fxUi02_dialogoEnviaCambioDoPagamento() throws Exception {
        String dialog = Files.readString(gui("SupplierPaymentDialog.java"));
        assertThat(dialog).contains("new PaymentExchangeRateField(")
                .contains("registerSupplierPayment(pa.purchaseId(), amount, accountId, reference, paymentRate)");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "PurchaseApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("&exchangeRate=");
    }

    private static Path gui(String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", file);
        return Files.exists(nested) ? nested : Path.of("desktop").resolve(nested);
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
