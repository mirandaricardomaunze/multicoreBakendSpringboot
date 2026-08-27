package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.printing.OrderPickingPrintService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CustomerOrderFulfillmentHarnessTest {
    @AfterEach void clear() { CurrentUserContext.clear(); }

    @Test
    void reprintRequiresDifferentApproverBeforePasswordValidation() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);
        Order order = new Order();
        order.setId(7L); order.setStatus("IN_SEPARATION");
        Company company = new Company(); company.setId(1L); order.setCompany(company);
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        CustomerOrderFulfillmentService service = new CustomerOrderFulfillmentService(
                mock(ComercialService.class), orders, mock(OrderLineRepository.class),
                mock(OrderEventRepository.class), mock(InventoryService.class),
                mock(OrderPickingPrintService.class), mock(AppUserService.class));

        assertThrows(BusinessRuleException.class, () -> service.reprint(7L,
                new ReprintAuthorizationRequest("gerente", "senha", "papel danificado", "POS-1")));
    }

    // ─── Mensagens de estado errado (SEP-10..SEP-13) ────────────────────────
    // Ver docs/SEPARACAO_MENSAGENS_SPEC.md. O que se verifica aqui não é a recusa (essa já
    // existia) — é a mensagem DIZER O QUE FAZER. "Estado actual invalido: PENDING" é verdade e
    // não ajuda ninguém que esteja ao balcão com um cliente à espera.

    private CustomerOrderFulfillmentService serviceFor(Order order, OrderRepository orders) {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);
        Company company = new Company(); company.setId(1L); order.setCompany(company);
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        return new CustomerOrderFulfillmentService(
                mock(ComercialService.class), orders, mock(OrderLineRepository.class),
                mock(OrderEventRepository.class), mock(InventoryService.class),
                mock(OrderPickingPrintService.class), mock(AppUserService.class));
    }

    /** Pedido do circuito de separação — é sobre estes que as mensagens de estado se aplicam. */
    private Order order(String status) {
        return order(status, mz.multicore.erp.modules.comercial.model.OrderKind.PICKING_REQUEST);
    }

    private Order order(String status, mz.multicore.erp.modules.comercial.model.OrderKind kind) {
        Order order = new Order();
        order.setId(7L);
        order.setOrderNumber("EC-2026/9");
        order.setStatus(status);
        order.setKind(kind);
        return order;
    }

    @Test // SEP-10
    void separarSemTerImpressoALista_dizQueFaltaImprimir() {
        Order order = order("AWAITING_SEPARATION");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparation(7L,
                        new mz.multicore.erp.modules.comercial.dto.OrderActionRequest(null, "POS-1")));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("lista de separação"),
                error.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("EC-2026/9"),
                "diz de que encomenda se trata");
    }

    @Test // SEP-11
    void separarEncomendaDoFluxoClassico_explicaQueNaoEDesteCircuito() {
        Order order = order("PENDING");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparation(7L,
                        new mz.multicore.erp.modules.comercial.dto.OrderActionRequest(null, "POS-1")));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("fatura-se directamente"),
                error.getMessage());
    }

    @Test // SEP-12
    void separarOQueJaEstaSeparado_dizOEstadoEmPortugues() {
        Order order = order("SEPARATED");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparation(7L,
                        new mz.multicore.erp.modules.comercial.dto.OrderActionRequest(null, "POS-1")));

        // Nada de códigos internos na cara do operador.
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("Separado"),
                error.getMessage());
        org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains("IN_SEPARATION"),
                error.getMessage());
    }

    @Test // SEP-13
    void imprimirListaComEncomendaJaSeparada_tambemExplica() {
        Order order = order("SEPARATED");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.printForPicking(7L, "POS-1"));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().startsWith("Não é possível"),
                error.getMessage());
    }

    // ─── Circuito de separação fechado à via A4 (ED-09..13) ─────────────────
    // Ver docs/ENCOMENDA_DUAS_VIAS_SPEC.md §4 (R4). O que se prova aqui é que a recusa vem da
    // VIA e não do estado: uma encomenda A4 nunca esteve em separação, pelo que dizer-lhe "ainda
    // aguarda separação" mandaria o operador imprimir uma lista que não existe.

    private Order formalOrder(String status) {
        return order(status, mz.multicore.erp.modules.comercial.model.OrderKind.FORMAL_ORDER);
    }

    @Test // ED-09
    void imprimirListaDeSeparacaoNumaEncomendaA4_recusaPelaVia() {
        Order order = formalOrder("PENDING");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.printForPicking(7L, "POS-1"));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("EC-2026/9"),
                "diz de que encomenda se trata: " + error.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("não passa pelo armazém"),
                error.getMessage());
    }

    @Test // ED-10
    void marcarComoSeparadoNumaEncomendaA4_recusaPelaViaEnaoPeloEstado() {
        // Estado que, num pedido de separação, seria o estado CERTO para separar. A recusa tem de
        // vir da via — se viesse do estado, esta encomenda passava.
        Order order = formalOrder("IN_SEPARATION");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparation(7L,
                        new mz.multicore.erp.modules.comercial.dto.OrderActionRequest(null, "POS-1")));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("não passa pelo armazém"),
                error.getMessage());
    }

    @Test // ED-11
    void reimprimirNumaEncomendaA4_recusa() {
        Order order = formalOrder("IN_SEPARATION");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        assertThrows(BusinessRuleException.class, () -> service.reprint(7L,
                new ReprintAuthorizationRequest("outro", "senha", "papel danificado", "POS-1")));
    }

    @Test // ED-12
    void recusaDaViaErrada_naoMostraCodigosInternos() {
        Order order = formalOrder("PENDING");
        CustomerOrderFulfillmentService service = serviceFor(order, mock(OrderRepository.class));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.printForPicking(7L, "POS-1"));

        org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains("FORMAL_ORDER"),
                error.getMessage());
        org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains("PICKING_REQUEST"),
                error.getMessage());
    }
}
