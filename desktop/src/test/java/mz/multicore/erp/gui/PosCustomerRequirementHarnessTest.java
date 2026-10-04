package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PosCustomerRequirementHarnessTest {

    @Test
    @DisplayName("POS-CUST-01: Campo de cliente no cabeçalho sinaliza obrigatoriedade do nome")
    void testClientFieldShowsNameRequirement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("caixa", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.clientSearchField, "clientSearchField deve existir");
                assertTrue(pos.clientSearchField.getToolTipText() != null
                                && pos.clientSearchField.getToolTipText().toLowerCase().contains("obrigatório"),
                        "Tooltip do campo de cliente deve indicar obrigatoriedade");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-CUST-02: Código fonte do POSPanel e POSService proíbem venda sem nome de cliente")
    void testSourceCodeEnforcesCustomerNameValidation() throws IOException {
        String posPanelSource = Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "POSPanel.java"));
        assertTrue(posPanelSource.contains("promptRequiredText"), "POSPanel deve solicitar o nome do cliente avulso se não informado");
        assertTrue(posPanelSource.contains("É obrigatório indicar o nome do cliente"), "POSPanel deve exibir aviso de nome obrigatório");

        String posServiceSource = Files.readString(Path.of("..", "backend", "src", "main", "java", "mz", "multicore", "erp", "modules", "pos", "service", "POSService.java"));
        assertTrue(posServiceSource.contains("É obrigatório indicar o nome do cliente para efetuar a venda no POS"),
                "POSService deve lançar BusinessRuleException quando a venda for avulsa sem nome");
    }

    private POSPanel createTestPosPanel() {
        POSApiClient posApi = Mockito.mock(POSApiClient.class);
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);
        PromotionApiClient promoApi = Mockito.mock(PromotionApiClient.class);
        mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scale = Mockito.mock(mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser.class);

        return new POSPanel(posApi, comercialApi, invApi, finApi, promoApi, scale);
    }
}
