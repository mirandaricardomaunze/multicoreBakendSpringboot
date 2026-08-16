package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoTipo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Leitura agregada dos documentos comerciais (fatura, encomenda, nota de crédito, nota de
 * débito) numa só lista filtrável por cliente/nº e período. Read-only: não cria nem altera
 * documentos. Reutiliza os repositórios de {@code comercial} (mesmo padrão de {@code ReportService}).
 */
@Service
public class MovimentosService {

    private static final String SEM_CLIENTE = "—";

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final DeliveryGuideRepository deliveryGuideRepository;

    public MovimentosService(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository,
            DeliveryGuideRepository deliveryGuideRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.deliveryGuideRepository = deliveryGuideRepository;
    }

    /**
     * Lista os movimentos comerciais da empresa, ordenados por data descendente.
     *
     * @param companyId empresa activa (validada contra o contexto do utilizador)
     * @param query     substring case-insensitive sobre nº OU cliente; nulo/vazio = sem filtro
     * @param from      data mínima inclusiva; nulo = sem limite inferior
     * @param to        data máxima inclusiva; nulo = sem limite superior
     */
    @Transactional(readOnly = true)
    public List<MovimentoDTO> listar(Long companyId, String query, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);

        List<MovimentoDTO> movimentos = new ArrayList<>();

        for (Invoice inv : invoiceRepository.findByCompanyId(companyId)) {
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.FATURA,
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    nameOf(inv.getClient(), inv.getCustomerName()),
                    inv.getCreatedAt(),
                    statusName(inv.getStatus()),
                    inv.getTotalAmount()));
        }
        for (Order order : orderRepository.findByCompanyId(companyId)) {
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.ENCOMENDA,
                    order.getId(),
                    order.getOrderNumber(),
                    nameOf(order.getClient(), order.getWalkInName()),
                    order.getCreatedAt(),
                    order.getStatus(),
                    order.getTotalAmount()));
        }
        for (DeliveryGuide guide : deliveryGuideRepository.findByCompanyId(companyId)) {
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.GUIA_REMESSA,
                    guide.getId(),
                    guide.getGuideNumber(),
                    nameOf(guide.getClient(), guide.getWalkInName()),
                    guide.getGuideDate(),
                    statusName(guide.getStatus()),
                    guide.getTotalAmount()));
        }
        for (CreditNote note : creditNoteRepository.findByCompanyId(companyId)) {
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.NOTA_CREDITO,
                    note.getId(),
                    note.getNoteNumber(),
                    nameOf(note.getClient(), null),
                    note.getIssueDate(),
                    statusName(note.getStatus()),
                    note.getTotalAmount()));
        }
        for (DebitNote note : debitNoteRepository.findByCompanyId(companyId)) {
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.NOTA_DEBITO,
                    note.getId(),
                    note.getNoteNumber(),
                    nameOf(note.getClient(), null),
                    note.getIssueDate(),
                    statusName(note.getStatus()),
                    note.getTotalAmount()));
        }

        return movimentos.stream()
                .filter(m -> matchesQuery(m, query))
                .filter(m -> withinPeriod(m, from, to))
                .sorted(byDateDescNullsLast())
                .toList();
    }

    private static String nameOf(Client client, String fallback) {
        if (client != null && client.getName() != null && !client.getName().isBlank()) {
            return client.getName();
        }
        return fallback != null && !fallback.isBlank() ? fallback : SEM_CLIENTE;
    }

    private static String statusName(Enum<?> status) {
        return status == null ? "" : status.name();
    }

    private static boolean matchesQuery(MovimentoDTO m, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String term = query.trim().toLowerCase();
        String numero = m.numero() == null ? "" : m.numero().toLowerCase();
        String cliente = m.cliente() == null ? "" : m.cliente().toLowerCase();
        return numero.contains(term) || cliente.contains(term);
    }

    private static boolean withinPeriod(MovimentoDTO m, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (m.data() == null) {
            return false;
        }
        LocalDate d = m.data().toLocalDate();
        if (from != null && d.isBefore(from)) {
            return false;
        }
        return to == null || !d.isAfter(to);
    }

    private static Comparator<MovimentoDTO> byDateDescNullsLast() {
        return Comparator.comparing(MovimentoDTO::data,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
