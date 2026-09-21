package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AccountStatementPanelHarnessTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Mock
    private AccountStatementApiClient statementApiClient;
    @Mock
    private ComercialApiClient comercialApiClient;
    @Mock
    private PurchaseApiClient purchaseApiClient;
    @Mock
    private PrintApiClient printApiClient;
    @Mock
    private CreditRiskApiClient creditRiskApiClient;

    @Test
    @DisplayName("ECC-UI-01: CustomerStatementPanel inicializa e integra com ClientesPanel")
    void testCustomerStatementPanelInstantiationAndTab() {
        lenient().when(comercialApiClient.getClients()).thenReturn(List.of(
                new ClientDTO(1L, "Cliente ABC Lda", "400111222", "Maputo", "c@abc.mz", 30, null)
        ));
        lenient().when(statementApiClient.getCustomerStatement(anyLong(), any(), any())).thenReturn(
                new CustomerStatementDTO(1L, "Cliente ABC Lda", "400111222", "Maputo", "c@abc.mz", null,
                        LocalDate.now().withDayOfYear(1), LocalDate.now(),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        Collections.emptyList())
        );

        CustomerStatementPanel statementPanel = new CustomerStatementPanel(statementApiClient, comercialApiClient);
        assertNotNull(statementPanel);

        ClientesPanel clientesPanel = new ClientesPanel(comercialApiClient, printApiClient, creditRiskApiClient, statementApiClient);
        assertNotNull(clientesPanel);

        // Deve conter 3 abas: Directório, Aging de Risco e Conta Corrente
        JTabbedPane tabs = null;
        for (Component comp : clientesPanel.getComponents()) {
            if (comp instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }
        assertNotNull(tabs, "ClientesPanel deve conter JTabbedPane");
        assertEquals(3, tabs.getTabCount(), "ClientesPanel deve possuir 3 abas com a inclusão de Conta Corrente");
        assertEquals("Conta Corrente & Reconciliação", tabs.getTitleAt(2));
    }

    @Test
    @DisplayName("ECC-UI-02: SupplierStatementPanel inicializa e integra com ComprasPanel")
    void testSupplierStatementPanelInstantiationAndTab() {
        lenient().when(purchaseApiClient.getSuppliers()).thenReturn(List.of(
                new SupplierDTO(1L, "Fornecedor XYZ SA", "400333444", "Matola", "vendas@xyz.mz", "841234567", "Carlos", true, 1L)
        ));
        lenient().when(statementApiClient.getSupplierStatement(anyLong(), any(), any())).thenReturn(
                new SupplierStatementDTO(1L, "Fornecedor XYZ SA", "400333444", "Matola", "vendas@xyz.mz", "841234567",
                        LocalDate.now().withDayOfYear(1), LocalDate.now(),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        Collections.emptyList())
        );

        SupplierStatementPanel supplierPanel = new SupplierStatementPanel(statementApiClient, purchaseApiClient);
        assertNotNull(supplierPanel);

        ComprasPanel comprasPanel = new ComprasPanel(purchaseApiClient, null, comercialApiClient, null, statementApiClient);
        assertNotNull(comprasPanel);

        // Verifica que a aba de Conta Corrente de Fornecedor está presente no JTabbedPane
        JTabbedPane tabs = null;
        for (Component comp : comprasPanel.getComponents()) {
            if (comp instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }
        assertNotNull(tabs, "ComprasPanel deve conter JTabbedPane");
        boolean foundStatementTab = false;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if ("Conta Corrente & Reconciliação".equals(tabs.getTitleAt(i))) {
                foundStatementTab = true;
                break;
            }
        }
        assertTrue(foundStatementTab, "ComprasPanel deve conter aba Conta Corrente & Reconciliação");
    }
}
