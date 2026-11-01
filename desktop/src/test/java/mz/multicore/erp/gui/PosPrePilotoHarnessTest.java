package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSale;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSaleStore;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ver docs/POS_PRE_PILOTO_SPEC.md. */
class PosPrePilotoHarnessTest {

    @TempDir Path dir;

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("PP-05: M-Pesa e e-Mola exigem referência; os outros métodos não")
    void pp05_mobilePaymentNeedsReference() {
        assertThatThrownBy(() -> PosPaymentDialog.requireMobileReference("MPESA", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PosPaymentDialog.requireMobileReference("EMOLA", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> PosPaymentDialog.requireMobileReference("MPESA", "ABC123")).doesNotThrowAnyException();
        assertThatCode(() -> PosPaymentDialog.requireMobileReference("CARD", "")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PP-06: Depois da venda, cliente avulso e crédito não passam para a seguinte")
    void pp06_customerIsResetAfterSale() throws Exception {
        onEdt(pos -> {
            pos.clientsList = List.of(client(7L, "Ana Lda"), client(9L, "Consumidor Final"));
            pos.clientSearchField.setText("Joana Avulsa");
            pos.creditCheck.setSelected(true);

            pos.clearCart();

            assertThat(pos.clientSearchField.getText()).isEmpty();
            assertThat(pos.clientCombo.getSelectedIndex()).isEqualTo(1);
            assertThat(pos.creditCheck.isSelected()).isFalse();
        });
    }

    @Test
    @DisplayName("PP-07: Suspender guarda a venda e deixa o caixa pronto para o cliente seguinte")
    void pp07_suspendClearsTheCart() throws Exception {
        onEdt(pos -> {
            PosSuspendedSaleStore store = store();
            PosSuspendedSaleActions actions = new PosSuspendedSaleActions(pos, store);
            pos.clientsList = List.of(client(9L, "Consumidor Final"));
            pos.catalogController.filterClients("");
            pos.clientSearchField.setText("Joana Avulsa");
            pos.cartItems.add(item(1L, "2"));

            actions.suspendCurrentSale();

            assertThat(pos.cartItems).isEmpty();
            assertThat(pos.clientSearchField.getText()).isEmpty();
            assertThat(store.forCompany(1L)).singleElement().satisfies(sale -> {
                assertThat(sale.clientName()).isEqualTo("Joana Avulsa");
                assertThat(sale.clientId()).isNull();
                assertThat(sale.lines()).singleElement().satisfies(line -> assertThat(line.productId()).isEqualTo(1L));
            });
            assertThat(pos.suspendedSalesBtn.getText()).isEqualTo("Em espera (1)");
        });
    }

    @Test
    @DisplayName("PP-08: Retomar repõe a venda com o preço actual e tira-a da espera")
    void pp08_resumeUsesCurrentPrice() throws Exception {
        onEdt(pos -> {
            PosSuspendedSaleStore store = store();
            PosSuspendedSaleActions actions = new PosSuspendedSaleActions(pos, store);
            pos.clientsList = List.of(client(9L, "Consumidor Final"));
            pos.catalogController.filterClients("");
            pos.clientSearchField.setText("Joana Avulsa");
            pos.cartItems.add(item(1L, "2"));
            actions.suspendCurrentSale();
            PosSuspendedSale sale = store.forCompany(1L).get(0);

            actions.apply(sale, List.of(product(1L, "120.00")));

            assertThat(pos.cartItems).singleElement().satisfies(item -> {
                assertThat(item.qty).isEqualByComparingTo("2");
                assertThat(item.product.unitPrice()).isEqualByComparingTo("120.00");
            });
            assertThat(pos.clientSearchField.getText()).isEqualTo("Joana Avulsa");
            assertThat(store.forCompany(1L)).isEmpty();
            assertThat(pos.suspendedSalesBtn.isVisible()).isFalse();
        });
    }

    @Test
    @DisplayName("PP-09: Venda sem artigos válidos ou carrinho ocupado: nada se perde")
    void pp09_failedResumeKeepsTheSale() throws Exception {
        onEdt(pos -> {
            PosSuspendedSaleStore store = store();
            PosSuspendedSaleActions actions = new PosSuspendedSaleActions(pos, store);
            pos.cartItems.add(item(1L, "1"));
            actions.suspendCurrentSale();
            PosSuspendedSale sale = store.forCompany(1L).get(0);

            actions.apply(sale, List.of());
            assertThat(pos.cartItems).isEmpty();
            assertThat(store.forCompany(1L)).hasSize(1);

            pos.cartItems.add(item(2L, "1"));
            actions.apply(sale, List.of(product(1L, "95.00")));
            assertThat(pos.cartItems).hasSize(1);
            assertThat(pos.cartItems.get(0).product.id()).isEqualTo(2L);
            assertThat(store.forCompany(1L)).hasSize(1);
        });
    }

    @Test
    @DisplayName("PP-10: Vendas em espera sobrevivem a um reinício e separam-se por empresa")
    void pp10_storeSurvivesRestart() {
        Path file = dir.resolve("data").resolve("espera.json");
        PosSuspendedSaleStore first = new PosSuspendedSaleStore(file);
        first.add(sale("a", 1L));
        first.add(sale("b", 2L));

        PosSuspendedSaleStore reopened = new PosSuspendedSaleStore(file);

        assertThat(reopened.forCompany(1L)).extracting(PosSuspendedSale::id).containsExactly("a");
        assertThat(reopened.forCompany(2L)).extracting(PosSuspendedSale::id).containsExactly("b");
        assertThat(reopened.forCompany(1L).get(0).total()).isEqualByComparingTo("190.00");
    }

    @Test
    @DisplayName("PP-11: Ficheiro ilegível é posto de lado, sem ser apagado")
    void pp11_corruptFileIsKept() throws Exception {
        Path file = dir.resolve("espera.json");
        Files.writeString(file, "{ quebrado");

        PosSuspendedSaleStore store = new PosSuspendedSaleStore(file);

        assertThat(store.forCompany(1L)).isEmpty();
        assertThat(dir.resolve("espera.json.corrupt")).hasContent("{ quebrado");
    }

    @Test
    @DisplayName("PP-12: Se a gravação falhar, a venda continua no carrinho")
    void pp12_failedSaveKeepsTheCart() throws Exception {
        Path blocker = Files.writeString(dir.resolve("ficheiro"), "x");
        onEdt(pos -> {
            PosSuspendedSaleActions actions = new PosSuspendedSaleActions(pos,
                    new PosSuspendedSaleStore(blocker.resolve("espera.json")));
            pos.cartItems.add(item(1L, "1"));

            actions.suspendCurrentSale();

            assertThat(pos.cartItems).hasSize(1);
        });
    }

    @Test
    @DisplayName("PP-13: F5 grava percentagem: 10% em 500 MT dá 450 MT, não metade")
    void pp13_discountIsAPercentage() {
        POSPanel.CartItem item = new POSPanel.CartItem(product(1L, "500.00"), BigDecimal.ONE, BigDecimal.ZERO, null, null);

        item.discount = PosKeyboardShortcutsHandler.parseDiscountPercent("10%");

        assertThat(item.getTotal()).isEqualByComparingTo("450.00");
        assertThatThrownBy(() -> PosKeyboardShortcutsHandler.parseDiscountPercent("101"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PosSuspendedSaleStore store() {
        return new PosSuspendedSaleStore(dir.resolve("espera.json"));
    }

    private interface PosTest { void run(POSPanel pos) throws Exception; }

    private static void onEdt(PosTest test) throws Exception {
        Long company = CurrentUserContext.getCurrentCompanyId();
        String username = CurrentUserContext.getUsername();
        String role = CurrentUserContext.getRole();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser(username, role);
            CurrentUserContext.setCurrentCompanyId(company);
            try {
                POSPanel pos = pos();
                pos.activeSession = Mockito.mock(TillSessionDTO.class);
                test.run(pos);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                CurrentUserContext.clear();
            }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static POSPanel pos() {
        return new POSPanel(Mockito.mock(POSApiClient.class), Mockito.mock(ComercialApiClient.class),
                Mockito.mock(InventoryApiClient.class), Mockito.mock(FinanceApiClient.class),
                Mockito.mock(PromotionApiClient.class), null);
    }

    private static PosSuspendedSale sale(String id, Long companyId) {
        return new PosSuspendedSale(id, companyId, "ana", java.time.LocalDateTime.now(), null, "Joana", null, null, null,
                new BigDecimal("190.00"), List.of(new PosSuspendedSale.Line(1L, "Massa", new BigDecimal("2"),
                        BigDecimal.ZERO, null, null, null, null)));
    }

    private static POSPanel.CartItem item(long productId, String qty) {
        return new POSPanel.CartItem(product(productId, "95.00"), new BigDecimal(qty), BigDecimal.ZERO, null, null);
    }

    private static ClientDTO client(long id, String name) {
        ClientDTO client = Mockito.mock(ClientDTO.class);
        Mockito.when(client.id()).thenReturn(id);
        Mockito.when(client.name()).thenReturn(name);
        return client;
    }

    private static ProductDTO product(long id, String price) {
        ProductDTO product = Mockito.mock(ProductDTO.class);
        Mockito.when(product.id()).thenReturn(id);
        Mockito.when(product.name()).thenReturn("Massa Esparguete 500g");
        Mockito.when(product.unitPrice()).thenReturn(new BigDecimal(price));
        Mockito.when(product.taxRate()).thenReturn(BigDecimal.ZERO);
        return product;
    }
}
