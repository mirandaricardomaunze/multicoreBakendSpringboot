package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.crm.model.SupportTicket;
import mz.multicore.erp.modules.crm.model.TicketPriority;
import mz.multicore.erp.modules.crm.model.TicketStatus;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A folha de obra era o único documento do sistema sem PDF — o técnico fechava a intervenção em
 * casa do cliente e não tinha nada para lhe deixar assinado.
 *
 * <p>Estes casos lêem o <b>texto do PDF</b>, não o código que o gera.
 */
class WorkSheetPrintServiceTest {

    private static final Long COMPANY = 4L;

    private WorkSheetRepository repository;
    private WorkSheetPrintService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkSheetRepository.class);
        service = new WorkSheetPrintService(repository);
        CurrentUserContext.setCurrentUser("tecnico", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(COMPANY);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void identificaAEmpresaOClienteEOPedido() throws Exception {
        String text = renderText(workSheet(new BigDecimal("2.5"), new BigDecimal("100.00"), false));

        assertTrue(text.contains("Multicore Loja Central"), text);
        assertTrue(text.contains("400123456"), "falta o NUIT: " + text);
        assertTrue(text.contains("FOLHA DE OBRA"), "o cabeçalho escreve o título em maiúsculas: " + text);
        assertTrue(text.contains("FO-000060"), "falta o número da folha: " + text);
        assertTrue(text.contains("Padaria Central"), "falta o cliente: " + text);
        assertTrue(text.contains("Impressora avariada"), "falta o assunto do pedido: " + text);
        assertTrue(text.contains("Urgente"), "falta a prioridade: " + text);
    }

    @Test
    void mostraHorasTarifaPecasETotalSemIva() throws Exception {
        String text = renderText(workSheet(new BigDecimal("2.5"), new BigDecimal("100.00"), false));

        assertTrue(text.contains("2.5"), "as horas têm de sair como foram registadas: " + text);
        assertTrue(text.contains("TOTAL (sem IVA)"), text);
        assertTrue(text.contains("212,50") || text.contains("212.50"), "falta o total 212,50: " + text);
        // O papel que o cliente assina não pode dizer-lhe que o serviço não leva imposto.
        assertFalse(text.contains("IVA 0,00"), "não pode anunciar IVA zero: " + text);
        assertTrue(text.contains("não incluem IVA") || text.contains("nao incluem IVA"),
                "falta a nota de que não é factura: " + text);
    }

    @Test
    void temBlocoDeAssinaturaDoTecnicoEDoCliente() throws Exception {
        String text = renderText(workSheet(new BigDecimal("1"), BigDecimal.ZERO, false));

        assertTrue(text.contains("O Técnico"), text);
        assertTrue(text.contains("Mário"), "o nome do técnico assina a folha: " + text);
        assertTrue(text.contains("conformidade"), "falta a linha de aceitação do cliente: " + text);
    }

    @Test
    void folhaAnuladaSaiCarimbada() throws Exception {
        String text = renderText(workSheet(new BigDecimal("1"), BigDecimal.ZERO, true));

        assertTrue(text.contains("FOLHA ANULADA"), "uma folha anulada não pode sair igual às outras: " + text);
        assertTrue(text.contains("Lançada no pedido errado"), "falta o motivo da anulação: " + text);
    }

    @Test
    void folhaDeOutraEmpresa_naoImprime() {
        when(repository.findByIdAndSupportTicketCompanyId(60L, COMPANY)).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.render(60L));
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────────────────────

    /** Texto de todas as páginas: o documento cresce com a descrição da intervenção. */
    private String renderText(WorkSheet ws) throws Exception {
        when(repository.findByIdAndSupportTicketCompanyId(ws.getId(), COMPANY)).thenReturn(Optional.of(ws));
        byte[] pdf = service.render(ws.getId());
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder all = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            all.append(extractor.getTextFromPage(page)).append('\n');
        }
        return all.toString();
    }

    private WorkSheet workSheet(BigDecimal hours, BigDecimal partsCost, boolean voided) {
        Company company = new Company();
        company.setId(COMPANY);
        company.setName("Multicore Loja Central");
        company.setTaxId("400123456");
        company.setAddress("Av. 25 de Setembro, Maputo");

        Client client = new Client();
        client.setId(9L);
        client.setName("Padaria Central");
        client.setTaxId("123456789");

        SupportTicket ticket = new SupportTicket();
        ticket.setId(11L);
        ticket.setCompany(company);
        ticket.setClient(client);
        ticket.setSubject("Impressora avariada");
        ticket.setDescription("Não imprime os talões");
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setPriority(TicketPriority.URGENT);
        ticket.setCreatedAt(LocalDateTime.of(2026, 8, 20, 9, 30));

        WorkSheet ws = new WorkSheet();
        ws.setId(60L);
        ws.setSupportTicket(ticket);
        ws.setTechnicianName("Mário");
        ws.setHoursWorked(hours);
        ws.setHourlyRate(new BigDecimal("45.00"));
        ws.setPartsCost(partsCost);
        ws.setPartsUsed(partsCost.signum() > 0 ? "Cabeça térmica" : null);
        ws.setDescription("Substituição da cabeça de impressão");
        ws.setTotalValue(hours.multiply(new BigDecimal("45.00")).add(partsCost));
        ws.setIsBilled(false);
        ws.setCreatedAt(LocalDateTime.of(2026, 8, 21, 14, 0));
        if (voided) {
            ws.setVoided(true);
            ws.setVoidReason("Lançada no pedido errado");
        }
        return ws;
    }
}
