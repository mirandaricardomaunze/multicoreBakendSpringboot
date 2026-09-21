package mz.multicore.erp.modules.financeira.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.service.CashFlowForecastService;
import mz.multicore.erp.modules.printing.CashFlowForecastPrintService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/forecast")
public class CashFlowForecastController {

    private final CashFlowForecastService forecastService;
    private final CashFlowForecastPrintService printService;

    public CashFlowForecastController(
            CashFlowForecastService forecastService,
            CashFlowForecastPrintService printService
    ) {
        this.forecastService = forecastService;
        this.printService = printService;
    }

    @GetMapping
    public ResponseEntity<CashFlowForecastDTO> getForecast() {
        return ResponseEntity.ok(forecastService.generateForecast());
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = printService.render(companyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "fluxo_caixa_previsional.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
