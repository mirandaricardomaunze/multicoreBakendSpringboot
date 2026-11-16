package mz.multicore.erp.desktop.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * HTTP clients of the business-sector modules (advances, butchery, expiry, production plan, custom orders), in one
 * bean so that every new sector module does not add a parameter to {@code MainFrame}. Any client may be
 * {@code null} in tests; its module then simply is not created.
 */
@Component
@Profile("desktop")
public class SectorApiClients {

    private final AdvancesApiClient advances;
    private final ButcheryApiClient butchery;
    private final ExpiryApiClient expiry;
    private final ProductionPlanApiClient productionPlan;
    private final CustomOrdersApiClient customOrders;
    private final ConsignmentApiClient consignment;
    private final ReturnablesApiClient returnables;
    private final VariantsApiClient variants;
    private final WeighbridgeApiClient weighbridge;
    private final ExceptionsApiClient exceptions;

    public SectorApiClients(AdvancesApiClient advances, ButcheryApiClient butchery, ExpiryApiClient expiry,
                            ProductionPlanApiClient productionPlan, CustomOrdersApiClient customOrders,
                            ConsignmentApiClient consignment, ReturnablesApiClient returnables, VariantsApiClient variants,
                            WeighbridgeApiClient weighbridge, ExceptionsApiClient exceptions) {
        this.advances = advances;
        this.butchery = butchery;
        this.expiry = expiry;
        this.productionPlan = productionPlan;
        this.customOrders = customOrders;
        this.consignment = consignment;
        this.returnables = returnables;
        this.variants = variants;
        this.weighbridge = weighbridge;
        this.exceptions = exceptions;
    }

    public AdvancesApiClient advances() {
        return advances;
    }

    public ButcheryApiClient butchery() {
        return butchery;
    }

    public ExpiryApiClient expiry() {
        return expiry;
    }

    public ProductionPlanApiClient productionPlan() {
        return productionPlan;
    }

    public CustomOrdersApiClient customOrders() {
        return customOrders;
    }

    public ConsignmentApiClient consignment() {
        return consignment;
    }

    public ReturnablesApiClient returnables() {
        return returnables;
    }

    public VariantsApiClient variants() {
        return variants;
    }

    public WeighbridgeApiClient weighbridge() {
        return weighbridge;
    }

    public ExceptionsApiClient exceptions() {
        return exceptions;
    }
}
