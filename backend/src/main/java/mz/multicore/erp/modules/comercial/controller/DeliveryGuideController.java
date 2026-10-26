package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.modules.comercial.dto.CreateDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.CreateStandaloneDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.dto.RejectionReasonRequest;
import mz.multicore.erp.modules.comercial.service.DeliveryGuideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HTTP da Guia de Remessa ao cliente. Só delega no serviço — regras, tenant, permissão e
 * auditoria vivem em {@link DeliveryGuideService}.
 */
@RestController
@RequestMapping("/api/comercial/delivery-guides")
public class DeliveryGuideController {

    private final DeliveryGuideService deliveryGuideService;

    public DeliveryGuideController(DeliveryGuideService deliveryGuideService) {
        this.deliveryGuideService = deliveryGuideService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryGuideDTO>> list(@RequestParam(required = false) Long companyId) {
        if (companyId == null) {
            companyId = mz.multicore.erp.architecture.security.CurrentUserContext.getCurrentCompanyId();
        }
        return ResponseEntity.ok(deliveryGuideService.findByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryGuideDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryGuideService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DeliveryGuideDTO> create(@RequestBody @Valid CreateDeliveryGuideRequest request) {
        return ResponseEntity.ok(deliveryGuideService.createFromOrder(request));
    }

    @PostMapping("/standalone")
    public ResponseEntity<DeliveryGuideDTO> createStandalone(
            @RequestBody @Valid CreateStandaloneDeliveryGuideRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(deliveryGuideService.createStandalone(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DeliveryGuideDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryGuideService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DeliveryGuideDTO> reject(@PathVariable Long id,
                                                   @RequestBody(required = false) @Valid RejectionReasonRequest body) {
        String reason = body == null ? null : body.rejectionReason();
        return ResponseEntity.ok(deliveryGuideService.reject(id, reason));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<DeliveryGuideDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryGuideService.cancel(id));
    }
}
