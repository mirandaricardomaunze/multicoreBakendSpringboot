package mz.multicore.erp.gui;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lógica pura da página de notificações — a linha da tabela e o resumo. Cenários NL-06/NL-07
 * (a interação Swing fica nos manuais NL-55..57).
 */
class NotificationsPanelTest {

    private static final NotificationItem ITEM =
            new NotificationItem("Stock", "Stock baixo: Arroz", "Loja — disponível 2, mínimo 5",
                    "Repor stock", "stock", 2);

    @Test // NL-06
    void rowFor_acrescentaColunaDeLeitura() {
        assertArrayEquals(
                new Object[]{"Stock", "Stock baixo: Arroz", "Loja — disponível 2, mínimo 5", "Repor stock", "Por ler"},
                NotificationsPanel.rowFor(ITEM, false));
        assertEquals("Lida", NotificationsPanel.rowFor(ITEM, true)[4]);
    }

    @Test // NL-07
    void summaryText_reflecteNaoLidas() {
        assertEquals("Não há notificações pendentes.", NotificationsPanel.summaryText(0, 0));
        assertEquals("2 por ler de 5 notificações.", NotificationsPanel.summaryText(5, 2));
        assertEquals("1 por ler de 1 notificação.", NotificationsPanel.summaryText(1, 1));
        assertEquals("5 notificações, todas lidas.", NotificationsPanel.summaryText(5, 0));
        assertEquals("1 notificação, já lida.", NotificationsPanel.summaryText(1, 0));
    }
}
