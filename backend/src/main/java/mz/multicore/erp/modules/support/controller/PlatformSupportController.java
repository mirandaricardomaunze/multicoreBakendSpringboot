package mz.multicore.erp.modules.support.controller;

import mz.multicore.erp.modules.support.dto.AddMessageRequest;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;
import mz.multicore.erp.modules.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lado do superadmin: ver todos os pedidos, responder e mudar o estado. Caminho /api/platform/**. */
@RestController
@RequestMapping("/api/platform/support/tickets")
public class PlatformSupportController {

    private final SupportService supportService;

    public PlatformSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    public List<SupportTicketDTO> all() {
        return supportService.listAllTickets();
    }

    @GetMapping("/{id}/messages")
    public List<SupportMessageDTO> messages(@PathVariable Long id) {
        return supportService.listPlatformMessages(id);
    }

    @PostMapping("/{id}/messages")
    public SupportTicketDTO reply(@PathVariable Long id, @Valid @RequestBody AddMessageRequest request) {
        return supportService.addSuperAdminReply(id, request.body());
    }

    @PatchMapping("/{id}/status")
    public SupportTicketDTO changeStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return supportService.changeStatus(id, request.status());
    }

    @GetMapping("/status-options")
    public List<String> statusOptions() {
        return supportService.statusOptions();
    }

    public record StatusRequest(String status) {}
}
