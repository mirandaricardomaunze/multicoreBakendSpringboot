package mz.multicore.erp.modules.comercial.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.comercial.dto.CancelReasonRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.ExtendQuotationValidityRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.service.QuotationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HTTP da cotação. Só delega no serviço — regras, validade, tenant, permissão e auditoria vivem em
 * {@link QuotationService}.
 */
@RestController
@RequestMapping("/api/comercial/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    public ResponseEntity<List<QuotationDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(quotationService.findByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuotationDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.findById(id));
    }

    @PostMapping
    public ResponseEntity<QuotationDTO> create(@RequestBody @Valid CreateQuotationRequest request) {
        return ResponseEntity.ok(quotationService.create(request));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<QuotationDTO> send(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.send(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<QuotationDTO> accept(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.accept(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<QuotationDTO> reject(@PathVariable Long id,
                                                @RequestBody(required = false) CancelReasonRequest body) {
        return ResponseEntity.ok(quotationService.reject(id, body == null ? null : body.reason()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<QuotationDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.cancel(id));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<QuotationDTO> extend(@PathVariable Long id,
                                                @RequestBody @Valid ExtendQuotationValidityRequest request) {
        return ResponseEntity.ok(quotationService.extendValidity(id, request.validUntil()));
    }

    /** Converte na encomenda e devolve-a — quem converte quer o número da encomenda gerada. */
    @PostMapping("/{id}/convert")
    public ResponseEntity<OrderDTO> convert(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.convert(id));
    }
}
