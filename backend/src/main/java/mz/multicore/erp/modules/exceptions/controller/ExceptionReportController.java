package mz.multicore.erp.modules.exceptions.controller;

import mz.multicore.erp.modules.exceptions.dto.ExceptionReportDTO;
import mz.multicore.erp.modules.exceptions.service.ExceptionReportService;
import mz.multicore.erp.modules.printing.ExceptionReportPrintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Anti-fraud exception report (managers only). */
@RestController
@RequestMapping("/api/exceptions")
public class ExceptionReportController {

    private final ExceptionReportService service;
    private final ExceptionReportPrintService printService;

    public ExceptionReportController(ExceptionReportService service, ExceptionReportPrintService printService) {
        this.service = service;
        this.printService = printService;
    }

    @GetMapping
    public ResponseEntity<ExceptionReportDTO> report(@RequestParam Long companyId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.report(companyId, from, to));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Long companyId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = printService.render(companyId, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"excepcoes-" + from + ".pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
