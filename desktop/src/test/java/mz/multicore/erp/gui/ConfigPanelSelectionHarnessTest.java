package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConfigPanelSelectionHarnessTest {

    private UserApiClient userApiClient;
    private AuditApiClient auditApiClient;
    private BackupApiClient backupApiClient;
    private DocumentConfigApiClient documentConfigApiClient;
    private SupportApiClient supportApiClient;
    private MySubscriptionApiClient mySubscriptionApiClient;
    private SystemMonitoringApiClient monitoringApiClient;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);

        userApiClient = Mockito.mock(UserApiClient.class);
        auditApiClient = Mockito.mock(AuditApiClient.class);
        backupApiClient = Mockito.mock(BackupApiClient.class);
        documentConfigApiClient = Mockito.mock(DocumentConfigApiClient.class);
        supportApiClient = Mockito.mock(SupportApiClient.class);
        mySubscriptionApiClient = Mockito.mock(MySubscriptionApiClient.class);
        monitoringApiClient = Mockito.mock(SystemMonitoringApiClient.class);

        when(userApiClient.getAllUsers()).thenReturn(Collections.emptyList());
        when(auditApiClient.getLogsByCompany(any())).thenReturn(Collections.emptyList());
        when(backupApiClient.files()).thenReturn(Collections.emptyList());
        when(supportApiClient.listCompanyTickets()).thenReturn(Collections.emptyList());
        when(mySubscriptionApiClient.getMySubscription()).thenReturn(new MySubscriptionDTO(
                "Empresa Teste", true, "PRO", "Profissional", "ACTIVE", "Activa",
                LocalDate.now(), LocalDate.now().plusMonths(1), 30L, new BigDecimal("3500.00")
        ));
    }

    @Test
    @DisplayName("CP-01: ConfigPanel e onPanelSelected executam sem NullPointerException")
    void testConfigPanelCreationAndSelection() {
        ConfigPanel configPanel = new ConfigPanel(
                userApiClient, auditApiClient, backupApiClient,
                documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient
        );

        assertNotNull(configPanel);
        assertDoesNotThrow(configPanel::onPanelSelected,
                "onPanelSelected() não deve lançar NullPointerException ao selecionar o painel no sidebar");
    }
}
