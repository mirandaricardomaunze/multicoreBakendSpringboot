package mz.multicore.erp.modules.pricing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.model.Company;

/** Price list assigned to a client per company; a shared client may have a different list in each company. */
@Entity
@Table(name = "client_price_lists", uniqueConstraints = {
        @UniqueConstraint(name = "uk_client_price_list", columnNames = {"company_id", "client_id"})
})
@Getter
@Setter
public class ClientPriceList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;
}
