package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.accounting.AccountingPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DesktopInitializationTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testPanelsInitialization() {
        AccountingApiClient accountingApi = Mockito.mock(AccountingApiClient.class);
        AccountingPanel accountingPanel = new AccountingPanel(accountingApi);
        assertNotNull(accountingPanel);

        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
        CreditRiskApiClient creditRiskApi = Mockito.mock(CreditRiskApiClient.class);
        AccountStatementApiClient stmtApi = Mockito.mock(AccountStatementApiClient.class);
        ClientesPanel clientesPanel = new ClientesPanel(comercialApi, printApi, creditRiskApi, stmtApi);
        assertNotNull(clientesPanel);

        ForensicAuditApiClient forensicApi = Mockito.mock(ForensicAuditApiClient.class);
        ForensicAuditPanel forensicPanel = new ForensicAuditPanel(forensicApi);
        assertNotNull(forensicPanel);

        CreditRiskPanel creditRiskPanel = new CreditRiskPanel(creditRiskApi);
        assertNotNull(creditRiskPanel);

        CustomerStatementPanel custStmtPanel = new CustomerStatementPanel(stmtApi, comercialApi);
        assertNotNull(custStmtPanel);

        SupplierStatementPanel suppStmtPanel = new SupplierStatementPanel(stmtApi, Mockito.mock(mz.multicore.erp.desktop.client.PurchaseApiClient.class));
        assertNotNull(suppStmtPanel);

        StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        StockWastePanel wastePanel = new StockWastePanel(wasteApi, invApi, comercialApi);
        assertNotNull(wastePanel);

        BankReconciliationApiClient bankReconApi = Mockito.mock(BankReconciliationApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);
        BankReconciliationPanel bankReconPanel = new BankReconciliationPanel(bankReconApi, financeApi);
        assertNotNull(bankReconPanel);

        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        ComprasPanel comprasPanel = new ComprasPanel(purchaseApi, invApi, comercialApi, financeApi, stmtApi);
        assertNotNull(comprasPanel);

        POSApiClient posApi = Mockito.mock(POSApiClient.class);
        PromotionApiClient promoApi = Mockito.mock(PromotionApiClient.class);
        mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser parser = Mockito.mock(mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser.class);
        POSPanel posPanel = new POSPanel(posApi, comercialApi, invApi, financeApi, promoApi, parser);
        assertNotNull(posPanel);

        CRMApiClient crmApi = Mockito.mock(CRMApiClient.class);
        CRMPanel crmPanel = new CRMPanel(crmApi, comercialApi);
        assertNotNull(crmPanel);

        HRApiClient hrApi = Mockito.mock(HRApiClient.class);
        HRPanel hrPanel = new HRPanel(hrApi, printApi);
        assertNotNull(hrPanel);

        NotificationFeed feed = Mockito.mock(NotificationFeed.class);
        NotificationReadStore store = new NotificationReadStore();
        NotificationsPanel notificationsPanel = new NotificationsPanel(feed, store, m -> {}, c -> {});
        assertNotNull(notificationsPanel);
    }
}
