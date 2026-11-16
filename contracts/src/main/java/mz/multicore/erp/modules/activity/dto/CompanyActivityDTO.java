package mz.multicore.erp.modules.activity.dto;

import java.util.List;

/**
 * Active profiles and capabilities of a company. {@code configured=false} when the company has not chosen
 * any profile yet, in which case the "Comércio geral" profile applies.
 */
public record CompanyActivityDTO(
        Long companyId,
        boolean configured,
        List<String> profiles,
        List<String> capabilities
) {}
