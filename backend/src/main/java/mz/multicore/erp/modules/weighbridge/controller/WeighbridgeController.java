package mz.multicore.erp.modules.weighbridge.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.printing.WeighingTicketPrintService;
import mz.multicore.erp.modules.weighbridge.dto.FirstWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.SecondWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.WeighingTicketDTO;
import mz.multicore.erp.modules.weighbridge.service.WeighbridgeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Truck scale: weighing tickets, the two weighings and the printed ticket. */
@RestController
@RequestMapping("/api/weighbridge")
public class WeighbridgeController {

    private final WeighbridgeService service;
    private final WeighingTicketPrintService printService;

    public WeighbridgeController(WeighbridgeService service, WeighingTicketPrintService printService) {
        this.service = service;
        this.printService = printService;
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<WeighingTicketDTO>> tickets(@RequestParam Long companyId, @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(service.tickets(companyId, days));
    }

    @PostMapping("/tickets")
    public ResponseEntity<WeighingTicketDTO> first(@RequestBody @Valid FirstWeighingRequest request) {
        return ResponseEntity.ok(service.first(request));
    }

    @PostMapping("/tickets/{id}/second")
    public ResponseEntity<WeighingTicketDTO> second(@PathVariable Long id, @RequestBody @Valid SecondWeighingRequest request) {
        return ResponseEntity.ok(service.second(id, request));
    }

    @GetMapping("/tickets/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestParam Long companyId) {
        byte[] pdf = printService.render(companyId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pesagem-" + id + ".pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
