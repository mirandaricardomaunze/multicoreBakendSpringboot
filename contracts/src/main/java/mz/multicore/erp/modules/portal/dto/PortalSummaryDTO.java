package mz.multicore.erp.modules.portal.dto;

import mz.multicore.erp.modules.reports.dto.LowStockAlertDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resumo só de leitura do painel do portal do gestor (PORTAL_GESTOR_SPEC §3.3). */
public record PortalSummaryDTO(
        LocalDate date,
        BigDecimal salesToday,
        BigDecimal salesMonth,
        List<PortalStoreSalesDTO> salesByStore,
        List<PortalOpenTillDTO> openTills,
        List<LowStockAlertDTO> lowStock,
        BigDecimal overdueTotal,
        List<PortalOverdueClientDTO> overdueClients,
        long pendingApprovals
) {}
