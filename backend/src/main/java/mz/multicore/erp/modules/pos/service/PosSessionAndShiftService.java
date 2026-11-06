package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.dto.TillMovementDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import mz.multicore.erp.modules.pos.model.ShiftReconciliation;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.ShiftReconciliationRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PosSessionAndShiftService {

    private final TillSessionRepository tillSessionRepository;
    private final TillMovementRepository tillMovementRepository;
    private final ShiftReconciliationRepository shiftReconciliationRepository;
    private final AuditLogService auditLogService;

    public PosSessionAndShiftService(TillSessionRepository tillSessionRepository,
                                     TillMovementRepository tillMovementRepository,
                                     ShiftReconciliationRepository shiftReconciliationRepository,
                                     AuditLogService auditLogService) {
        this.tillSessionRepository = tillSessionRepository;
        this.tillMovementRepository = tillMovementRepository;
        this.shiftReconciliationRepository = shiftReconciliationRepository;
        this.auditLogService = auditLogService;
    }

    public Optional<TillSession> getActiveSession(String operator, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return tillSessionRepository.findActiveSessionForOperator(operator, "OPEN", companyId);
    }

    public List<TillSession> getSessionsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return tillSessionRepository.findByCompanyId(companyId);
    }

    public List<TillMovement> getMovementsBySession(Long sessionId) {
        List<TillMovement> movements = tillMovementRepository.findByTillSessionId(sessionId);
        movements.stream().findFirst().ifPresent(m ->
                CurrentUserContext.requireCompany(m.getTillSession().getCompany().getId()));
        return movements;
    }

    public BigDecimal computeExpectedCash(Long sessionId, BigDecimal openingBalance) {
        BigDecimal expected = openingBalance;
        for (TillMovement m : tillMovementRepository.findByTillSessionId(sessionId)) {
            TillMovementType type = m.getMovementType();
            if (type == TillMovementType.SALE || type == TillMovementType.SUPRIMENTO) {
                expected = expected.add(m.getAmount());
            } else if (type == TillMovementType.SANGRIA || type == TillMovementType.REFUND) {
                expected = expected.subtract(m.getAmount());
            }
        }
        return expected;
    }

    public TillMovement addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa está fechada. Impossível realizar movimentos.");
        }

        CurrentUserContext.requireCompany(session.getCompany().getId());
        TillMovementType movementType;
        try {
            movementType = TillMovementType.valueOf(type == null ? "" : type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de movimento de caixa inválido: " + type);
        }

        if (movementType == TillMovementType.SANGRIA) {
            PermissionGuard.requireManagerOrAdmin("realizar sangria de caixa");
            BigDecimal expected = computeExpectedCash(sessionId, session.getOpeningBalance());
            if (expected.compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo de caixa insuficiente para realizar a sangria. Disponível: " + DisplayNumbers.money(expected) + " MT");
            }
        }

        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(movementType);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(session.getOperator());
        TillMovement saved = tillMovementRepository.save(movement);
        if (movementType == TillMovementType.SANGRIA || movementType == TillMovementType.SUPRIMENTO
                || movementType == TillMovementType.REFUND) {
            auditLogService.logCurrent("POS_CASH_MOVEMENT",
                    movementType + " na sessão " + sessionId + ": " + amount + " MT. " + description);
        }
        return saved;
    }

    public ShiftReconciliationDTO performShiftHandover(ShiftHandoverRequest request) {
        TillSession session = tillSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Impossível realizar passagem de turno numa sessão fechada.");
        }
        CurrentUserContext.requireCompany(session.getCompany().getId());

        String effectiveOperator = session.getCurrentOperator() != null
                ? session.getCurrentOperator() : session.getOperator();
        if (!effectiveOperator.equalsIgnoreCase(request.outgoingOperator())) {
            throw new BusinessRuleException("O operador de saída ('" + request.outgoingOperator()
                    + "') não corresponde ao operador actual da sessão ('" + effectiveOperator + "').");
        }
        if (request.incomingOperator() == null || request.incomingOperator().isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o operador de entrada.");
        }
        if (request.outgoingOperator().equalsIgnoreCase(request.incomingOperator())) {
            throw new BusinessRuleException("O operador de entrada não pode ser o mesmo que o de saída.");
        }
        if (request.countedCash() == null) {
            throw new BusinessRuleException("É obrigatório indicar o valor contado na gaveta.");
        }

        BigDecimal expected = computeExpectedCash(session.getId(), session.getOpeningBalance());
        BigDecimal difference = request.countedCash().subtract(expected);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            PermissionGuard.requireManagerOrAdmin("passagem de turno com diferença");
        }

        ShiftReconciliation recon = new ShiftReconciliation();
        recon.setTillSession(session);
        recon.setOutgoingOperator(request.outgoingOperator());
        recon.setIncomingOperator(request.incomingOperator());
        recon.setReconciledAt(LocalDateTime.now());
        recon.setExpectedCash(expected);
        recon.setCountedCash(request.countedCash());
        recon.setDifference(difference);
        recon.setCashBreakdownJson(request.cashBreakdownJson() != null && !request.cashBreakdownJson().isBlank()
                ? request.cashBreakdownJson().trim() : null);
        recon.setNotes(request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null);
        recon.setCreatedBy(request.outgoingOperator());
        recon.setCompany(session.getCompany());
        recon = shiftReconciliationRepository.save(recon);

        session.setCurrentOperator(request.incomingOperator());
        tillSessionRepository.save(session);

        auditLogService.logCurrent("POS_SHIFT_HANDOVER",
                "Passagem de turno na sessão " + session.getId()
                        + ": " + request.outgoingOperator() + " -> " + request.incomingOperator()
                        + ". Esperado: " + expected + " MT; contado: " + request.countedCash()
                        + " MT; diferença: " + difference + " MT."
                        + (recon.getNotes() != null ? " Obs: " + recon.getNotes() : ""));

        return toDTO(recon);
    }

    public List<ShiftReconciliationDTO> getShiftReconciliations(Long sessionId) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));
        CurrentUserContext.requireCompany(session.getCompany().getId());
        return shiftReconciliationRepository.findByTillSessionIdOrderByReconciledAtAsc(sessionId)
                .stream().map(this::toDTO).toList();
    }

    public void requireOwnOperatorForSeller(String operator) {
        if ("SELLER".equalsIgnoreCase(CurrentUserContext.getRole())
                && (operator == null || !operator.equalsIgnoreCase(CurrentUserContext.getUsername()))) {
            throw new BusinessRuleException("O vendedor só pode operar a sua própria sessão de caixa.");
        }
    }

    public TillSessionDTO toDTO(TillSession s) {
        return new TillSessionDTO(
                s.getId(), s.getOperator(), s.getCurrentOperator(),
                s.getCompany() != null ? s.getCompany().getId() : null,
                s.getOpeningBalance(), s.getClosingBalanceExpected(), s.getClosingBalanceReal(),
                s.getDifference(), s.getOpenDate(), s.getCloseDate(), s.getStatus(),
                s.getClosingNotes(), s.getCashBreakdownJson()
        );
    }

    public ShiftReconciliationDTO toDTO(ShiftReconciliation r) {
        return new ShiftReconciliationDTO(
                r.getId(), r.getTillSession() != null ? r.getTillSession().getId() : null,
                r.getOutgoingOperator(), r.getIncomingOperator(), r.getReconciledAt(),
                r.getExpectedCash(), r.getCountedCash(), r.getDifference(),
                r.getCashBreakdownJson(), r.getNotes()
        );
    }

    public TillMovementDTO toDTO(TillMovement m) {
        return new TillMovementDTO(
                m.getId(), m.getTillSession() != null ? m.getTillSession().getId() : null,
                m.getMovementType() != null ? m.getMovementType().name() : null,
                m.getAmount(), m.getDescription(), m.getMovementDate()
        );
    }
}
