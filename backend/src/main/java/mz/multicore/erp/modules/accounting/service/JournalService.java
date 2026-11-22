package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.CreateJournalEntryRequest;
import mz.multicore.erp.modules.accounting.dto.JournalEntryDTO;
import mz.multicore.erp.modules.accounting.dto.JournalLineDTO;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.accounting.repository.JournalEntryRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Diário — onde os lançamentos nascem, manuais ou automáticos.
 *
 * <p>Uma só porta de gravação ({@link #save}): venha o lançamento do contabilista ou de uma
 * fatura, passa pela mesma validação de partida dobrada. Um lançamento desequilibrado gravado é
 * um balancete que nunca mais fecha e ninguém consegue dizer quando começou.
 */
@Service
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountsService chartOfAccountsService;
    private final DocumentNumberService documentNumberService;
    private final CompanyService companyService;
    private final AuditLogService auditLogService;
    private final CostCenterService costCenterService;

    public JournalService(JournalEntryRepository journalEntryRepository,
                          ChartOfAccountsService chartOfAccountsService,
                          DocumentNumberService documentNumberService,
                          CompanyService companyService,
                          AuditLogService auditLogService,
                          CostCenterService costCenterService) {
        this.journalEntryRepository = journalEntryRepository;
        this.chartOfAccountsService = chartOfAccountsService;
        this.documentNumberService = documentNumberService;
        this.companyService = companyService;
        this.auditLogService = auditLogService;
        this.costCenterService = costCenterService;
    }

    /** Lançamento manual do contabilista. */
    @Transactional
    public JournalEntryDTO createManualEntry(CreateJournalEntryRequest request) {
        PermissionGuard.requireManagerOrAdmin("lançar na contabilidade");
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(request.entryDate());
        entry.setDescription(request.description());
        entry.setSource(JournalSource.MANUAL);

        for (CreateJournalEntryRequest.Line line : request.lines()) {
            Account account = chartOfAccountsService.requirePostableAccount(companyId, line.accountCode());
            JournalLine journalLine = new JournalLine();
            journalLine.setAccount(account);
            journalLine.setDebit(line.debit());
            journalLine.setCredit(line.credit());
            journalLine.setDescription(line.description());
            journalLine.setCostCenter(costCenterOrNull(companyId, line.costCenterId()));
            entry.addLine(journalLine);
        }

        JournalEntry saved = save(entry, companyId);
        auditLogService.logCurrent("JOURNAL_ENTRY",
                "Lançamento manual " + saved.getEntryNumber() + ": " + saved.getDescription()
                        + " (" + saved.totalDebit() + " MT)");
        return toDTO(saved);
    }

    /**
     * Grava um lançamento já montado (usado pelos lançamentos automáticos).
     *
     * <p>Não faz guarda de papel: quem regista uma venda não tem de ser contabilista. A guarda
     * está na operação de origem (emitir fatura já exige MANAGER/ADMIN).
     */
    @Transactional
    public JournalEntry save(JournalEntry entry, Long companyId) {
        entry.setCompany(companyService.getCurrentCompanyReference(companyId));
        assignDefaultCostCenter(entry, companyId);
        entry.validateForPosting();
        entry.validateCostCenters();
        if (entry.getEntryNumber() == null) {
            entry.setEntryNumber(documentNumberService.next(DocumentSeries.JOURNAL_ENTRY));
        }
        if (entry.getCreatedBy() == null) {
            entry.setCreatedBy(CurrentUserContext.getUsername());
        }
        return journalEntryRepository.save(entry);
    }

    /**
     * Centro pedido explicitamente para uma partida (lançamento manual, despesa — §3.2): tem de existir, ser da
     * empresa e estar activo. {@code null} deixa a partida sem centro; se for de classe 6/7, {@link #save} completa-a
     * com o centro por omissão.
     */
    @Transactional(readOnly = true)
    public CostCenter costCenterOrNull(Long companyId, Long costCenterId) {
        return costCenterId == null ? null : costCenterService.requirePostable(companyId, costCenterId);
    }

    /** Partidas das classes 6/7 sem centro ficam no centro por omissão da empresa (§3.2). */
    private void assignDefaultCostCenter(JournalEntry entry, Long companyId) {
        if (!entry.hasLinesMissingCostCenter()) {
            return;
        }
        CostCenter defaultCenter = costCenterService.requireDefault(companyId);
        entry.getLines().stream()
                .filter(line -> line.requiresCostCenter() && line.getCostCenter() == null)
                .forEach(line -> line.setCostCenter(defaultCenter));
    }

    /**
     * Lançamento já existente para um documento — a trava contra lançar a mesma venda duas
     * vezes quando o pedido HTTP é repetido.
     */
    @Transactional(readOnly = true)
    public Optional<JournalEntry> findByDocument(Long companyId, JournalSource source, Long documentId) {
        return journalEntryRepository.findByCompanyIdAndSourceAndSourceDocumentId(companyId, source, documentId);
    }

    /** Página do diário, do lançamento mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public PageResponse<JournalEntryDTO> getJournalPage(Integer page, Integer size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return mz.multicore.erp.architecture.paging.PageResponseMapper.from(
                journalEntryRepository.findByCompanyIdOrderByEntryDateDescIdDesc(companyId, PageQuery.of(page, size)),
                JournalService::toDTO);
    }

    /** Lançamentos de um período (base do razão e do balancete). */
    @Transactional(readOnly = true)
    public List<JournalEntry> findEntriesBetween(Long companyId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Indique o período (data inicial e final).");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("A data final não pode ser anterior à data inicial.");
        }
        return journalEntryRepository
                .findByCompanyIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(companyId, from, to);
    }

    static JournalEntryDTO toDTO(JournalEntry entry) {
        List<JournalLineDTO> lines = entry.getLines().stream()
                .map(line -> new JournalLineDTO(
                        line.getId(),
                        line.getAccount().getCode(),
                        line.getAccount().getName(),
                        line.safeDebit(),
                        line.safeCredit(),
                        line.getDescription()))
                .toList();
        return new JournalEntryDTO(
                entry.getId(),
                entry.getEntryNumber(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getSource(),
                entry.getSource().label(),
                entry.getSourceDocumentNumber(),
                entry.totalDebit().setScale(2, java.math.RoundingMode.HALF_UP),
                entry.totalCredit().setScale(2, java.math.RoundingMode.HALF_UP),
                lines);
    }

    static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
