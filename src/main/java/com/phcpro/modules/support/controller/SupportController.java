package com.phcpro.modules.support.controller;

import com.phcpro.modules.support.dto.AddMessageRequest;
import com.phcpro.modules.support.dto.CreateTicketRequest;
import com.phcpro.modules.support.dto.SupportMessageDTO;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import com.phcpro.modules.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lado da empresa (tenant-scoped): abrir pedidos e conversar. Requer token + X-Company-Id. */
@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    public List<SupportTicketDTO> myTickets() {
        return supportService.listCompanyTickets();
    }

    @PostMapping
    public SupportTicketDTO open(@Valid @RequestBody CreateTicketRequest request) {
        return supportService.openTicket(request);
    }

    @GetMapping("/{id}/messages")
    public List<SupportMessageDTO> messages(@PathVariable Long id) {
        return supportService.listCompanyMessages(id);
    }

    @PostMapping("/{id}/messages")
    public void reply(@PathVariable Long id, @Valid @RequestBody AddMessageRequest request) {
        supportService.addCompanyMessage(id, request.body());
    }
}
