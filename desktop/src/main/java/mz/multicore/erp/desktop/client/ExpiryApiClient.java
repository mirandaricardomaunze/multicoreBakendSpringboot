package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.expiry.dto.*;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** HTTP client for expiry dates and markdowns ({@code /api/expiry}, SA-C14 and SA-C15). */
@Component
@Profile("desktop")
public class ExpiryApiClient {

    private final DesktopClientFactory clientFactory;

    public ExpiryApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ExpiryBoardDTO board(Long companyId, int days) {
        return clientFactory.authenticatedClient().get("/api/expiry/board?companyId=" + companyId + "&days=" + days, ExpiryBoardDTO.class);
    }

    public List<MarkdownRuleDTO> rules(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/expiry/rules?companyId=" + companyId, MarkdownRuleDTO.class);
    }

    public MarkdownRuleDTO saveRule(SaveMarkdownRuleRequest request) {
        return clientFactory.authenticatedClient().put("/api/expiry/rules", request, MarkdownRuleDTO.class);
    }

    public ExpiryLotDTO markdown(Long companyId, Long batchId, BigDecimal percent) {
        return clientFactory.authenticatedClient().post("/api/expiry/markdowns", new MarkdownRequest(companyId, batchId, percent),
                ExpiryLotDTO.class);
    }

    public ApplyMarkdownsResultDTO applyRules(Long companyId) {
        return clientFactory.authenticatedClient().post("/api/expiry/markdowns/apply-rules?companyId=" + companyId, null,
                ApplyMarkdownsResultDTO.class);
    }

    public StockWasteDTO writeOff(WriteOffExpiredRequest request) {
        return clientFactory.authenticatedClient().post("/api/expiry/write-offs", request, StockWasteDTO.class);
    }
}
