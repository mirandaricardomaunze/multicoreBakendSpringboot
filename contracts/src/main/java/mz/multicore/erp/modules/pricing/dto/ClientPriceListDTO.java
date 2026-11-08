package mz.multicore.erp.modules.pricing.dto;

/** List assigned to a client in this company; {@code priceListId=null} when there is none. */
public record ClientPriceListDTO(
        Long clientId,
        String clientName,
        Long priceListId,
        String priceListName
) {}
