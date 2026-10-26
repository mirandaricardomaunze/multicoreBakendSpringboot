package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.modules.quality.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QualityModuleUiHarnessTest {

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("QST-UI-01: NotificationFeed gera alertas proativos de equipamentos expirados")
    void testNotificationFeedQualityAlerts() {
        ApprovalApiClient approval = mock(ApprovalApiClient.class);
        InventoryApiClient inv = mock(InventoryApiClient.class);
        MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);
        QualityApiClient quality = mock(QualityApiClient.class);

        ExpiredWorkerAlertDTO alertExpired = new ExpiredWorkerAlertDTO(
                101L, "João Munguambe", "Produção", "Soldador",
                1, 0,
                List.of(new ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO(
                        10L, 1L, "EPI-001", "Máscara de Solda Automática",
                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5), -5L,
                        AssignmentStatus.EXPIRED, "Expirado"
                ))
        );
        ExpiredWorkerAlertDTO alertExpiring = new ExpiredWorkerAlertDTO(
                102L, "Maria Cossa", "Laboratório", "Técnica de Laboratório",
                0, 1,
                List.of(new ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO(
                        11L, 2L, "EPI-002", "Óculos de Proteção UV",
                        LocalDate.now().minusMonths(5), LocalDate.now().plusDays(3), 3L,
                        AssignmentStatus.EXPIRING_SOON, "A Expirar"
                ))
        );

        when(quality.getExpiredWorkerAlerts(1L)).thenReturn(List.of(alertExpired, alertExpiring));

        NotificationFeed feed = new NotificationFeed(
                approval, inv, sub, hr, null, null, null, null, null, quality
        );

        List<NotificationFeed.NotificationItem> items = feed.load(1L);
        assertNotNull(items);

        boolean foundExpired = false;
        boolean foundExpiring = false;

        for (NotificationFeed.NotificationItem item : items) {
            if ("Qualidade & HST".equals(item.type())) {
                if (item.title().contains("João Munguambe")) {
                    foundExpired = true;
                    assertEquals(3, item.priority(), "Alerta de expirado deve ser crítico (prioridade 3)");
                    assertEquals("quality", item.moduleCard());
                } else if (item.title().contains("Maria Cossa")) {
                    foundExpiring = true;
                    assertEquals(2, item.priority(), "Alerta de a expirar deve ser atenção (prioridade 2)");
                    assertEquals("quality", item.moduleCard());
                }
            }
        }

        assertTrue(foundExpired, "Deve ter gerado notificação de equipamento expirado para João Munguambe");
        assertTrue(foundExpiring, "Deve ter gerado notificação de equipamento a expirar para Maria Cossa");
    }

    @Test
    @DisplayName("QST-UI-02: QualityPanel inicializa estrutura canónica de abas e KPIs")
    void testQualityPanelCanonicalStructure() {
        QualityApiClient quality = mock(QualityApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);

        when(quality.searchAssignments(anyLong(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0));
        when(quality.searchEquipments(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0));
        when(quality.getExpiredWorkerAlerts(anyLong())).thenReturn(List.of());
        when(quality.getQualityKpis(anyLong())).thenReturn(new QualityKpiDTO(0L, 0L, 0L, 0L, 0L));

        QualityPanel panel = new QualityPanel(quality, hr);
        assertNotNull(panel);

        // Verifica existência de JTabbedPane
        JTabbedPane tabbedPane = findComponent(panel, JTabbedPane.class);
        assertNotNull(tabbedPane, "Painel de Qualidade deve conter abas ergonómicas");
        assertEquals(3, tabbedPane.getTabCount(), "Deve conter 3 abas operacionais");
        assertTrue(tabbedPane.getTitleAt(0).contains("Atribuições"));
        assertTrue(tabbedPane.getTitleAt(1).contains("Radar de Validades"));
        assertTrue(tabbedPane.getTitleAt(2).contains("Catálogo de Equipamentos"));
    }

    @Test
    @DisplayName("QST-UI-03: Diálogos de Qualidade usam limites padrão e sem emojis")
    void testQualityDialogsLimitsAndNoEmojis() {
        QualityEquipmentFormDialog eqDialog = new QualityEquipmentFormDialog(null, null);
        assertNotNull(eqDialog);
        assertTrue(eqDialog.getHeight() <= 650, "Altura do diálogo não pode estourar ecrã");
        assertFalse(containsEmoji(eqDialog.getTitle()), "Título do diálogo de equipamento não pode conter emojis");
        eqDialog.dispose();

        QualityAssignmentDialog asDialog = new QualityAssignmentDialog(null, null, null, null, List.of(), List.of());
        assertNotNull(asDialog);
        assertTrue(asDialog.getHeight() <= 650, "Altura do diálogo de atribuição não pode estourar ecrã");
        assertFalse(containsEmoji(asDialog.getTitle()), "Título do diálogo de atribuição não pode conter emojis");

        // Verifica presença de DateField com botão de calendário
        mz.multicore.erp.gui.components.DateField dateField = findComponent(asDialog, mz.multicore.erp.gui.components.DateField.class);
        assertNotNull(dateField, "Diálogo de atribuição deve possuir DateField com seletor de calendário");
        assertNotNull(dateField.getCalendarButton(), "DateField deve possuir trailing calendar button");

        asDialog.dispose();
    }

    @SuppressWarnings("unchecked")
    private static <T> T findComponent(java.awt.Container container, Class<T> clazz) {
        for (java.awt.Component comp : container.getComponents()) {
            if (clazz.isInstance(comp)) return (T) comp;
            if (comp instanceof java.awt.Container sub) {
                T found = findComponent(sub, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean containsEmoji(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            int type = Character.getType(s.charAt(i));
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                return true;
            }
        }
        return false;
    }
}
