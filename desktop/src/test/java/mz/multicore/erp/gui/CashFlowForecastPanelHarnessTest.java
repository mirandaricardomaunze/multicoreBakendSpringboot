package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.modules.financeira.dto.CashFlowAlertDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashFlowForecastPanelHarnessTest {

    @Mock
    private CashFlowForecastApiClient forecastApiClient;

    @Mock
    private FinanceApiClient financeApiClient;

    @Mock
    private ComercialApiClient comercialApiClient;

    @Mock
    private BankReconciliationApiClient bankReconciliationApiClient;

    private CashFlowForecastPanel forecastPanel;
    private FinanceiroPanel financeiroPanel;

    private final LocalDate today = LocalDate.of(2026, 9, 17);

    @BeforeEach
    void setUp() {
        forecastPanel = new CashFlowForecastPanel(forecastApiClient);
        financeiroPanel = new FinanceiroPanel(financeApiClient, comercialApiClient, bankReconciliationApiClient, forecastApiClient);
    }

    @Test
    @DisplayName("TFC-07: CashFlowForecastPanel renderiza tabela de baldes e banner de alerta com dados do DTO")
    void tfc07_renderizaDadosCorretamente() {
        CashFlowBucketDTO b1 = new CashFlowBucketDTO("OVERDUE", "Vencido", new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"), new BigDecimal("105000.00"));
        CashFlowBucketDTO b2 = new CashFlowBucketDTO("TODAY", "Hoje", new BigDecimal("2000.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("106000.00"));

        CashFlowAlertDTO alert = new CashFlowAlertDTO("HEALTHY", "Liquidez estável", null, BigDecimal.ZERO, "Manter rotina");

        CashFlowItemDTO topR = new CashFlowItemDTO(1L, "RECEIVABLE", "FT-01", "Cliente A", today, new BigDecimal("5000.00"), "OVERDUE");
        CashFlowItemDTO topP = new CashFlowItemDTO(1L, "PAYABLE", "V/FT-01", "Fornecedor B", today, new BigDecimal("1000.00"), "TODAY");

        CashFlowForecastDTO dto = new CashFlowForecastDTO(
                today,
                new BigDecimal("20000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("7000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("106000.00"),
                List.of(b1, b2),
                List.of(topR),
                List.of(topP),
                alert
        );

        forecastPanel.displayForecast(dto);

        assertThat(forecastPanel.getCurrentForecast()).isNotNull();
        assertThat(forecastPanel.getBucketsTable().getRowCount()).isEqualTo(2);
        assertThat(forecastPanel.getBucketsTable().getValueAt(0, 0)).isEqualTo("Vencido");
        assertThat(forecastPanel.getBucketsTable().getValueAt(1, 0)).isEqualTo("Hoje");
        assertThat(forecastPanel.getAlertBanner()).isNotNull();
    }

    @Test
    @DisplayName("TFC-07b: FinanceiroPanel inclui aba de Projeção Previsional")
    void tfc07b_financeiroPanelComAbaForecast() {
        assertThat(financeiroPanel.getForecastPanel()).isNotNull();
        financeiroPanel.showForecastTab();
        assertThat(financeiroPanel.getForecastPanel()).isInstanceOf(CashFlowForecastPanel.class);
    }
}
