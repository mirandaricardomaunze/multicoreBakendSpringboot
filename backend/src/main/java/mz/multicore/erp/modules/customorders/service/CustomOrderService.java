package mz.multicore.erp.modules.customorders.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.service.OrderCommandService;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.customorders.dto.*;
import mz.multicore.erp.modules.customorders.model.CustomOrderStage;
import mz.multicore.erp.modules.customorders.model.DeliveryMode;
import mz.multicore.erp.modules.customorders.model.OrderCustomization;
import mz.multicore.erp.modules.customorders.repository.CustomOrderAgendaRepository;
import mz.multicore.erp.modules.customorders.repository.OrderCustomizationRepository;
import mz.multicore.erp.modules.productionplan.service.ProductionPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Orders with a date and personalisation (SA-C13, {@code docs/ENCOMENDAS_PERSONALIZADAS_SPEC.md}): a birthday cake
 * for Saturday at 15:00 with "Parabéns Ana" and a photo, meat for a party delivered at an address. The order stays
 * the commercial document; this module keeps delivery, personalisation and the kitchen stage, builds the agenda
 * and sends the quantities to the production plan of the delivery day.
 */
@Service
public class CustomOrderService {

    private static final int MAX_IMAGE_BYTES = 2_000_000;

    private final CustomOrderAgendaRepository agendaRepository;
    private final OrderCustomizationRepository customizationRepository;
    private final OrderLookup orderLookup;
    private final OrderCommandService orderCommands;
    private final ProductionPlanService planService;
    private final AuditLogService auditLogService;

    public CustomOrderService(CustomOrderAgendaRepository agendaRepository, OrderCustomizationRepository customizationRepository,
                              OrderLookup orderLookup, OrderCommandService orderCommands,
                              ProductionPlanService planService, AuditLogService auditLogService) {
        this.agendaRepository = agendaRepository;
        this.customizationRepository = customizationRepository;
        this.orderLookup = orderLookup;
        this.orderCommands = orderCommands;
        this.planService = planService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CustomOrderDTO> agenda(Long companyId, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        if (to.isBefore(from) || from.plusDays(62).isBefore(to)) {
            throw new BusinessRuleException("Escolha um período de até 2 meses.");
        }
        List<Order> orders = agendaRepository.agenda(companyId, from, to);
        List<Long> ids = orders.stream().map(Order::getId).toList();
        Map<Long, OrderCustomization> custom = new HashMap<>();
        if (!ids.isEmpty()) customizationRepository.findByOrders(ids).forEach(c -> custom.put(c.getOrder().getId(), c));
        Map<Long, BigDecimal> advances = agendaRepository.advances(ids);
        List<CustomOrderDTO> result = new ArrayList<>();
        for (Order o : orders) result.add(toDto(o, custom.get(o.getId()), advances.get(o.getId())));
        result.sort(Comparator.comparing(CustomOrderDTO::deliveryDate)
                .thenComparing(d -> d.deliveryTime() == null ? java.time.LocalTime.MAX : d.deliveryTime()));
        return result;
    }

    @Transactional
    public CustomOrderDTO save(Long orderId, SaveCustomOrderRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("registar encomenda personalizada");
        Order order = requireOrder(orderId, request.companyId());
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " está anulada.");
        }
        if (request.mode() == DeliveryMode.DELIVERY && (request.deliveryAddress() == null || request.deliveryAddress().isBlank())) {
            throw new BusinessRuleException("Indique a morada de entrega.");
        }
        OrderCustomization c = customizationRepository.findByOrder(orderId, request.companyId()).orElseGet(() -> {
            OrderCustomization created = new OrderCustomization();
            created.setCompany(order.getCompany());
            created.setOrder(order);
            return created;
        });
        order.setExpectedDeliveryDate(request.deliveryDate());
        orderCommands.saveOrder(order);
        c.setDeliveryTime(request.deliveryTime());
        c.setMode(request.mode());
        c.setDeliveryAddress(blankToNull(request.deliveryAddress()));
        c.setContactPhone(blankToNull(request.contactPhone()));
        c.setPersonalization(blankToNull(request.personalization()));
        if (request.removeImage()) c.setImageData(null);
        if (request.imageBase64() != null && !request.imageBase64().isBlank()) c.setImageData(decodeImage(request.imageBase64()));
        c = customizationRepository.save(c);
        auditLogService.logCurrent("CUSTOM_ORDER_SAVE", "Encomenda " + order.getOrderNumber() + " para " + request.deliveryDate()
                + (request.deliveryTime() == null ? "" : " às " + request.deliveryTime()) + ".");
        return toDto(order, c, agendaRepository.advances(List.of(orderId)).get(orderId));
    }

    @Transactional
    public CustomOrderDTO stage(Long orderId, CustomOrderStageRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("mudar estado da encomenda");
        Order order = requireOrder(orderId, request.companyId());
        OrderCustomization c = customizationRepository.findByOrder(orderId, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Defina primeiro a entrega da encomenda."));
        c.setStage(request.stage());
        c = customizationRepository.save(c);
        auditLogService.logCurrent("CUSTOM_ORDER_STAGE", "Encomenda " + order.getOrderNumber() + ": " + request.stage().name() + ".");
        return toDto(order, c, agendaRepository.advances(List.of(orderId)).get(orderId));
    }

    @Transactional
    public SendToPlanResultDTO sendToPlan(Long orderId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireSeller("enviar encomenda para produção");
        Order order = requireOrder(orderId, companyId);
        OrderCustomization c = customizationRepository.findByOrder(orderId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Defina primeiro a entrega da encomenda."));
        if (c.getSentToPlanAt() != null) throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " já foi enviada para produção.");
        if (order.getExpectedDeliveryDate() == null || order.getWarehouse() == null) {
            throw new BusinessRuleException("A encomenda precisa de data de entrega e armazém.");
        }
        Map<Long, BigDecimal> byRecipe = new LinkedHashMap<>();
        List<String> planned = new ArrayList<>();
        List<String> withoutRecipe = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            Optional<Long> recipe = planService.recipeFor(companyId, line.getProduct().getId());
            if (recipe.isPresent()) {
                byRecipe.merge(recipe.get(), line.getQuantity(), BigDecimal::add);
                planned.add(line.getProduct().getName());
            } else {
                withoutRecipe.add(line.getProduct().getName());
            }
        }
        if (byRecipe.isEmpty()) {
            throw new BusinessRuleException("Nenhum artigo desta encomenda tem receita activa: crie a receita no Plano de Produção.");
        }
        planService.addToPlan(companyId, order.getWarehouse().getId(), order.getExpectedDeliveryDate(), byRecipe);
        c.setSentToPlanAt(LocalDateTime.now());
        if (c.getStage() == CustomOrderStage.TO_PRODUCE) c.setStage(CustomOrderStage.IN_PRODUCTION);
        customizationRepository.save(c);
        auditLogService.logCurrent("CUSTOM_ORDER_TO_PLAN", "Encomenda " + order.getOrderNumber() + " enviada para o plano de "
                + order.getExpectedDeliveryDate() + ": " + String.join(", ", planned) + ".");
        return new SendToPlanResultDTO(planned, withoutRecipe);
    }

    @Transactional(readOnly = true)
    public byte[] image(Long orderId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return customizationRepository.findByOrder(orderId, companyId).map(OrderCustomization::getImageData)
                .orElseThrow(() -> new BusinessRuleException("A encomenda não tem fotografia de referência."));
    }

    /** Order and customisation for the kitchen sheet. */
    @Transactional(readOnly = true)
    public Sheet sheet(Long orderId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Order order = requireOrder(orderId, companyId);
        OrderCustomization c = customizationRepository.findByOrder(orderId, companyId).orElse(null);
        order.getLines().forEach(l -> l.getProduct().getName());
        if (order.getClient() != null) order.getClient().getName();
        order.getCompany().getName();
        return new Sheet(order, c, toDto(order, c, agendaRepository.advances(List.of(orderId)).get(orderId)));
    }

    public record Sheet(Order order, OrderCustomization customization, CustomOrderDTO view) {}

    private Order requireOrder(Long orderId, Long companyId) {
        return orderLookup.findOrderForCompany(orderId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
    }

    static byte[] decodeImage(String base64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("A fotografia não é válida.");
        }
        if (bytes.length > MAX_IMAGE_BYTES) throw new BusinessRuleException("A fotografia tem mais de 2 MB: reduza-a antes de anexar.");
        boolean jpeg = bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
        boolean png = bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        if (!jpeg && !png) throw new BusinessRuleException("Use uma fotografia JPEG ou PNG.");
        return bytes;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static CustomOrderDTO toDto(Order o, OrderCustomization c, BigDecimal advance) {
        List<CustomOrderLineDTO> lines = o.getLines().stream()
                .map(l -> new CustomOrderLineDTO(l.getProduct().getId(), l.getProduct().getName(), l.getQuantity())).toList();
        String client = o.getClient() != null ? o.getClient().getName() : o.getWalkInName();
        return new CustomOrderDTO(o.getId(), o.getOrderNumber(), o.getStatus(), o.getClient() == null ? null : o.getClient().getId(),
                client, c == null ? null : c.getContactPhone(), o.getExpectedDeliveryDate(), c == null ? null : c.getDeliveryTime(),
                c == null ? DeliveryMode.PICKUP : c.getMode(), c == null ? null : c.getDeliveryAddress(),
                c == null ? null : c.getPersonalization(), c != null && c.getImageData() != null && c.getImageData().length > 0,
                c == null ? CustomOrderStage.TO_PRODUCE : c.getStage(), o.getTotalAmount(), advance == null ? BigDecimal.ZERO : advance,
                c != null && c.getSentToPlanAt() != null, lines);
    }
}
