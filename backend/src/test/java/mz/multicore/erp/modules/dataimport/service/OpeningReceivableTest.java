package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateOpeningReceivableRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceDocumentKind;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.OpeningReceivableCommandService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.numbering.model.DocumentSequence;
import mz.multicore.erp.modules.numbering.repository.DocumentSequenceRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-SI-01: cada dívida de cliente importada é um documento "Saldo Inicial" numerado na
 * série própria {@code SI-aaaa/n} da empresa activa, que guarda o {@code numero_original} do sistema antigo; a chave é
 * cliente + nº original: já importada → {@code JÁ EXISTE} (nunca alterada), repetida no ficheiro → {@code ERRO} em
 * ambas (SPEC §5.2, §3.3).
 */
class OpeningReceivableTest {

    private static final String HEADER = "nuit_cliente;numero_original;data;vencimento;valor_em_divida;descricao";
    private static final long COMPANY_ID = 7L;
    private static final int YEAR = LocalDate.now().getYear();

    private final ClientLookup clientLookup = mock(ClientLookup.class);
    private final InvoiceLookup invoiceLookup = mock(InvoiceLookup.class);
    private final OpeningReceivableImportService service =
            new OpeningReceivableImportService(clientLookup, invoiceLookup);

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final CompanyLookup companyLookup = mock(CompanyLookup.class);
    private final DocumentSequenceRepository sequenceRepository = mock(DocumentSequenceRepository.class);
    private final OpeningReceivableCommandService commandService = new OpeningReceivableCommandService(
            invoiceRepository, clientLookup, companyLookup, new DocumentNumberService(sequenceRepository));
    private final OpeningReceivableWriter writer = new OpeningReceivableWriter(commandService);

    private final Client acme = client(31L, "400123456", "Acme Lda");
    private final Client beta = client(32L, "400654321", "Beta Comercial");
    private final Company company = new Company();

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        company.setId(COMPANY_ID);
        when(clientLookup.findForCompanyByTaxId(anyString(), eq(COMPANY_ID))).thenReturn(Optional.empty());
        when(clientLookup.findForCompanyByTaxId(eq("400123456"), eq(COMPANY_ID))).thenReturn(Optional.of(acme));
        when(clientLookup.findForCompanyByTaxId(eq("400654321"), eq(COMPANY_ID))).thenReturn(Optional.of(beta));
        when(clientLookup.requireForCompany(eq(31L), eq(COMPANY_ID))).thenReturn(acme);
        when(clientLookup.requireForCompany(eq(32L), eq(COMPANY_ID))).thenReturn(beta);
        when(companyLookup.require(COMPANY_ID)).thenReturn(company);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentSequence sequence = new DocumentSequence();
        sequence.setCompanyId(COMPANY_ID);
        sequence.setSeries(DocumentSeries.OPENING_RECEIVABLE);
        sequence.setYear(YEAR);
        sequence.setLastNumber(0);
        when(sequenceRepository.lockByCompanyAndSeriesAndYear(COMPANY_ID, DocumentSeries.OPENING_RECEIVABLE, YEAR))
                .thenReturn(Optional.of(sequence));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static Client client(Long id, String taxId, String name) {
        Client client = new Client();
        client.setId(id);
        client.setTaxId(taxId);
        client.setName(name);
        return client;
    }

    private static byte[] file(String... lines) {
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private ImportPreviewDTO preview(String... lines) {
        return service.preview(file(lines));
    }

    @Test
    void seriesIsSiAndCreatedInAdvanceWithTheOthers() {
        assertThat(DocumentSeries.OPENING_RECEIVABLE).isEqualTo("SI");
        assertThat(DocumentSeries.ALL).contains(DocumentSeries.OPENING_RECEIVABLE);
    }

    @Test
    void newDebtIsNovoAndCarriesClientOriginalNumberDatesAmountAndDescription() {
        ImportPlan<CreateOpeningReceivableRequest> plan = service.plan(file(HEADER,
                "400123456;FT 2025/118;15/10/2025;2025-11-14;12.500,75;Fornecimento de Outubro"));

        assertThat(plan.lines()).hasSize(1);
        assertThat(plan.lines().get(0).row().status()).isEqualTo(ImportRowStatus.NOVO);
        CreateOpeningReceivableRequest request = plan.lines().get(0).item();
        assertThat(request.clientId()).isEqualTo(31L);
        assertThat(request.originalNumber()).isEqualTo("FT 2025/118");
        assertThat(request.documentDate()).isEqualTo(LocalDate.of(2025, 10, 15));
        assertThat(request.dueDate()).isEqualTo(LocalDate.of(2025, 11, 14));
        assertThat(request.amount()).isEqualByComparingTo("12500.75");
        assertThat(request.description()).isEqualTo("Fornecimento de Outubro");
    }

    @Test
    void alreadyImportedClientAndOriginalNumberIsJaExiste() {
        when(invoiceLookup.existsOpeningReceivable(COMPANY_ID, 31L, "FT 2025/118")).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER,
                "400123456;FT 2025/118;15/10/2025;14/11/2025;1000;",
                "400654321;FT 2025/118;15/10/2025;14/11/2025;500;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO);
    }

    @Test
    void sameOriginalNumberOfAnotherClientIsADifferentKey() {
        ImportPreviewDTO preview = preview(HEADER,
                "400123456;FT 2025/118;15/10/2025;14/11/2025;1000;",
                "400654321;FT 2025/118;15/10/2025;14/11/2025;500;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsOnly(ImportRowStatus.NOVO);
    }

    @Test
    void sameClientAndOriginalNumberTwiceInTheFileIsErrorInBoth() {
        ImportPreviewDTO preview = preview(HEADER,
                "400123456;FT 2025/118;15/10/2025;14/11/2025;1000;",
                "400654321;FT 2025/200;15/10/2025;14/11/2025;500;",
                " 400123456 ; FT 2025/118 ;20/10/2025;20/11/2025;300;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.NOVO, ImportRowStatus.ERRO);
        ImportRowDTO first = preview.rows().get(0);
        assertThat(first.column()).isEqualTo("numero_original");
        assertThat(first.message()).contains("repetid").contains("2, 4");
    }

    @Test
    void writesOneSiDocumentPerNewDebtNumberedPerCompanyAndKeepingTheOriginalNumber() {
        when(invoiceLookup.existsOpeningReceivable(COMPANY_ID, 31L, "FT 2025/100")).thenReturn(true);

        ImportPreviewDTO written = writer.write(service.plan(file(HEADER,
                "400123456;FT 2025/100;01/09/2025;01/10/2025;999;",
                "400123456;FT 2025/118;15/10/2025;14/11/2025;12.500,75;Fornecimento de Outubro",
                "400654321;VD-77;03/11/2025;03/12/2025;480,00;")));

        assertThat(written.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO, ImportRowStatus.NOVO);
        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        List<Invoice> documents = saved.getAllValues();

        Invoice first = documents.get(0);
        assertThat(first.getInvoiceNumber()).isEqualTo("SI-" + YEAR + "/1");
        assertThat(first.getDocumentKind()).isEqualTo(InvoiceDocumentKind.OPENING_BALANCE);
        assertThat(first.getOriginalNumber()).isEqualTo("FT 2025/118");
        assertThat(first.getClient()).isSameAs(acme);
        assertThat(first.getCompany()).isSameAs(company);
        assertThat(first.getDocumentDate()).isEqualTo(LocalDate.of(2025, 10, 15));
        assertThat(first.issueDate()).isEqualTo(LocalDate.of(2025, 10, 15));
        assertThat(first.getDueDate()).isEqualTo(LocalDate.of(2025, 11, 14));
        assertThat(first.getTotalAmount()).isEqualByComparingTo("12500.75");
        assertThat(first.getTotalBeforeTax()).isEqualByComparingTo("12500.75");
        assertThat(first.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(first.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(first.getLines()).isEmpty();
        assertThat(first.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
        assertThat(first.getDescription()).isEqualTo("Fornecimento de Outubro");

        assertThat(documents.get(1).getInvoiceNumber()).isEqualTo("SI-" + YEAR + "/2");
        assertThat(documents.get(1).getOriginalNumber()).isEqualTo("VD-77");
        assertThat(documents.get(1).getClient()).isSameAs(beta);
    }

    @Test
    void ordinaryInvoicesKeepTheSaleKind() {
        Invoice sale = new Invoice();

        assertThat(sale.getDocumentKind()).isEqualTo(InvoiceDocumentKind.SALE);
        assertThat(sale.getOriginalNumber()).isNull();
    }

    @Test
    void fileWithErrorsWritesNothing() {
        ImportPlan<CreateOpeningReceivableRequest> plan = service.plan(file(HEADER,
                "400123456;FT 2025/118;15/10/2025;14/11/2025;1000;",
                "400123456;FT 2025/118;15/10/2025;14/11/2025;1000;"));

        assertThatThrownBy(() -> writer.write(plan)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nada foi gravado");
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void commandRefusesAnAlreadyImportedKeyEvenIfThePreviewMissedIt() {
        when(invoiceRepository.existsByCompanyIdAndClientIdAndOriginalNumber(COMPANY_ID, 31L, "FT 2025/118"))
                .thenReturn(true);

        assertThatThrownBy(() -> commandService.create(new CreateOpeningReceivableRequest(31L, "FT 2025/118",
                LocalDate.of(2025, 10, 15), LocalDate.of(2025, 11, 14), new BigDecimal("10"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("FT 2025/118");
        verify(invoiceRepository, org.mockito.Mockito.never()).save(any());
    }

    // ---- IMP-SI-02: valor > 0, vencimento ≥ data, data não futura, cliente existente (§5.2, §5) ----

    private ImportRowDTO singleRow(String line) {
        ImportPreviewDTO preview = preview(HEADER, line);
        assertThat(preview.rows()).hasSize(1);
        return preview.rows().get(0);
    }

    @Test
    void zeroOrNegativeAmountIsErrorOnTheAmountColumn() {
        for (String amount : List.of("0", "0,00", "-150")) {
            ImportRowDTO row = singleRow("400123456;FT 1;15/10/2025;14/11/2025;" + amount + ";");

            assertThat(row.status()).as(amount).isEqualTo(ImportRowStatus.ERRO);
            assertThat(row.column()).isEqualTo("valor_em_divida");
            assertThat(row.message()).contains("maior que zero");
        }
    }

    @Test
    void dueDateBeforeTheDocumentDateIsErrorOnTheDueDateColumn() {
        ImportRowDTO row = singleRow("400123456;FT 1;15/10/2025;14/10/2025;1000;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("vencimento");
        assertThat(row.message()).contains("anterior");
    }

    @Test
    void dueDateOnTheDocumentDateIsAccepted() {
        assertThat(singleRow("400123456;FT 1;15/10/2025;15/10/2025;1000;").status())
                .isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void futureDocumentDateIsErrorOnTheDateColumn() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        ImportRowDTO row = singleRow("400123456;FT 1;" + tomorrow + ";" + tomorrow.plusDays(30) + ";1000;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("data");
        assertThat(row.message()).contains("futura");
    }

    @Test
    void todayIsAValidDocumentDate() {
        LocalDate today = LocalDate.now();

        assertThat(singleRow("400123456;FT 1;" + today + ";" + today.plusDays(30) + ";1000;").status())
                .isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void missingOrMalformedValuesAreErrorsOfTheirColumn() {
        assertThat(singleRow("400123456;;15/10/2025;14/11/2025;1000;").column()).isEqualTo("numero_original");
        assertThat(singleRow("400123456;FT 1;31-12-2025;14/11/2025;1000;").column()).isEqualTo("data");
        assertThat(singleRow("400123456;FT 1;15/10/2025;;1000;").column()).isEqualTo("vencimento");
        assertThat(singleRow("400123456;FT 1;15/10/2025;14/11/2025;abc;").column()).isEqualTo("valor_em_divida");
    }

    @Test
    void fileWithAnInvalidDebtWritesNothing() {
        ImportPlan<CreateOpeningReceivableRequest> plan = service.plan(file(HEADER,
                "400123456;FT 1;15/10/2025;14/11/2025;1000;",
                "400654321;FT 2;15/10/2025;14/11/2025;0;"));

        assertThatThrownBy(() -> writer.write(plan)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void commandRefusesNonPositiveAmountFutureDateAndDueDateBeforeTheDate() {
        LocalDate date = LocalDate.of(2025, 10, 15);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> commandService.create(new CreateOpeningReceivableRequest(31L, "FT 1",
                date, date.plusDays(30), BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> commandService.create(new CreateOpeningReceivableRequest(31L, "FT 1",
                tomorrow, tomorrow.plusDays(30), BigDecimal.TEN, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("futura");
        assertThatThrownBy(() -> commandService.create(new CreateOpeningReceivableRequest(31L, "FT 1",
                date, date.minusDays(1), BigDecimal.TEN, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("anterior");
        verify(invoiceRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void unknownClientCannotBeKeyed() {
        ImportPreviewDTO preview = preview(HEADER, "499999999;FT 1;15/10/2025;14/11/2025;1000;");

        ImportRowDTO row = preview.rows().get(0);
        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("nuit_cliente");
        verify(invoiceLookup, org.mockito.Mockito.never()).existsOpeningReceivable(anyLong(), anyLong(), anyString());
    }
}
