package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoTipo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cobre o harness MU-01..MU-07 da vista unificada de movimentos: agregação dos quatro tipos,
 * ordenação por data desc, filtros por nº/cliente e período, e guarda multi-tenant.
 * Repositórios mockados (mesmo padrão de {@code InventoryServiceTest}).
 */
class MovimentosServiceTest {

    private static final Long COMPANY_ID = 1L;

    private InvoiceRepository invoiceRepository;
    private OrderRepository orderRepository;
    private CreditNoteRepository creditNoteRepository;
    private DebitNoteRepository debitNoteRepository;
    private DeliveryGuideRepository deliveryGuideRepository;
    private mz.multicore.erp.modules.comercial.repository.QuotationRepository quotationRepository;
    private MovimentosService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        orderRepository = mock(OrderRepository.class);
        creditNoteRepository = mock(CreditNoteRepository.class);
        debitNoteRepository = mock(DebitNoteRepository.class);
        deliveryGuideRepository = mock(DeliveryGuideRepository.class);
        quotationRepository = mock(mz.multicore.erp.modules.comercial.repository.QuotationRepository.class);
        service = new MovimentosService(invoiceRepository, orderRepository,
                creditNoteRepository, debitNoteRepository, deliveryGuideRepository, quotationRepository);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private void stubOneOfEach() {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice("FT-1", clientNamed("Ana"), null, days(3), InvoiceStatus.PAID, "100")));
        when(orderRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                order("EC-1", null, "Balcão", days(2), "PENDING", "50")));
        when(creditNoteRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                creditNote("NC-1", clientNamed("Bruno"), days(1), NoteStatus.APPROVED, "20")));
        when(debitNoteRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                debitNote("ND-1", clientNamed("Ana"), days(4), NoteStatus.APPROVED, "30")));
    }

    @Test // MU-01
    void listar_umDeCadaTipo_devolveQuatro() {
        stubOneOfEach();
        List<MovimentoDTO> r = service.listar(COMPANY_ID, null, null, null);
        assertEquals(4, r.size());
        assertTrue(r.stream().anyMatch(m -> m.tipo() == MovimentoTipo.FATURA && "FT-1".equals(m.numero())));
        assertTrue(r.stream().anyMatch(m -> m.tipo() == MovimentoTipo.ENCOMENDA && "Balcão".equals(m.cliente())));
        assertTrue(r.stream().anyMatch(m -> m.tipo() == MovimentoTipo.NOTA_CREDITO));
        assertTrue(r.stream().anyMatch(m -> m.tipo() == MovimentoTipo.NOTA_DEBITO));
    }

    @Test // MU-02
    void listar_ordenadoPorDataDesc() {
        stubOneOfEach();
        List<MovimentoDTO> r = service.listar(COMPANY_ID, null, null, null);
        for (int i = 1; i < r.size(); i++) {
            assertFalse(r.get(i - 1).data().isBefore(r.get(i).data()),
                    "Movimentos devem vir por data descendente");
        }
        // o mais recente é a ND (days(4))
        assertEquals(MovimentoTipo.NOTA_DEBITO, r.get(0).tipo());
    }

    @Test // MU-03
    void listar_filtraPorNumero_caseInsensitive() {
        stubOneOfEach();
        List<MovimentoDTO> r = service.listar(COMPANY_ID, "nc-1", null, null);
        assertEquals(1, r.size());
        assertEquals(MovimentoTipo.NOTA_CREDITO, r.get(0).tipo());
    }

    @Test // MU-04
    void listar_filtraPorCliente_incluiWalkIn() {
        stubOneOfEach();
        List<MovimentoDTO> r = service.listar(COMPANY_ID, "ana", null, null);
        assertEquals(2, r.size()); // fatura + nota de débito da Ana
        assertTrue(r.stream().allMatch(m -> "Ana".equals(m.cliente())));

        List<MovimentoDTO> walkIn = service.listar(COMPANY_ID, "balcão", null, null);
        assertEquals(1, walkIn.size());
        assertEquals(MovimentoTipo.ENCOMENDA, walkIn.get(0).tipo());
    }

    @Test // MU-05
    void listar_filtraPorPeriodoInclusivo() {
        stubOneOfEach(); // datas em days(1..4)
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to = LocalDate.now().plusDays(3);
        List<MovimentoDTO> r = service.listar(COMPANY_ID, null, from, to);
        assertEquals(2, r.size()); // encomenda (days2) + fatura (days3), inclusivo nas pontas
        assertTrue(r.stream().noneMatch(m -> m.tipo() == MovimentoTipo.NOTA_DEBITO)); // days4 fora
        assertTrue(r.stream().noneMatch(m -> m.tipo() == MovimentoTipo.NOTA_CREDITO)); // days1 fora
    }

    @Test // MU-06
    void listar_semFiltros_devolveTudo() {
        stubOneOfEach();
        assertEquals(4, service.listar(COMPANY_ID, "   ", null, null).size());
    }

    @Test // MU-07
    void listar_empresaDiferenteDaActiva_lancaBusinessRule() {
        assertThrows(BusinessRuleException.class, () -> service.listar(999L, null, null, null));
        verifyNoInteractions(invoiceRepository, orderRepository, creditNoteRepository, debitNoteRepository,
                deliveryGuideRepository);
    }

    // ---- builders ----

    private static LocalDateTime days(int d) {
        return LocalDate.now().plusDays(d).atTime(10, 0);
    }

    private static Client clientNamed(String name) {
        Client c = new Client();
        c.setName(name);
        return c;
    }

    private static Invoice invoice(String number, Client client, String customerName,
                                   LocalDateTime createdAt, InvoiceStatus status, String total) {
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setInvoiceNumber(number);
        inv.setClient(client);
        inv.setCustomerName(customerName);
        inv.setCreatedAt(createdAt);
        inv.setStatus(status);
        inv.setTotalAmount(new BigDecimal(total));
        return inv;
    }

    private static Order order(String number, Client client, String walkInName,
                               LocalDateTime createdAt, String status, String total) {
        Order o = new Order();
        o.setId(2L);
        o.setOrderNumber(number);
        o.setClient(client);
        o.setWalkInName(walkInName);
        o.setCreatedAt(createdAt);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal(total));
        return o;
    }

    private static CreditNote creditNote(String number, Client client, LocalDateTime issueDate,
                                         NoteStatus status, String total) {
        CreditNote n = new CreditNote();
        n.setId(3L);
        n.setNoteNumber(number);
        n.setClient(client);
        n.setIssueDate(issueDate);
        n.setStatus(status);
        n.setTotalAmount(new BigDecimal(total));
        return n;
    }

    private static DebitNote debitNote(String number, Client client, LocalDateTime issueDate,
                                       NoteStatus status, String total) {
        DebitNote n = new DebitNote();
        n.setId(4L);
        n.setNoteNumber(number);
        n.setClient(client);
        n.setIssueDate(issueDate);
        n.setStatus(status);
        n.setTotalAmount(new BigDecimal(total));
        return n;
    }
}
