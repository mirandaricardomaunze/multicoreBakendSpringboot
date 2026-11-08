package mz.multicore.erp.modules.company.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId; // NUIT / NIF

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "phone", length = 40)
    private String phone;

    // Professional profile (V113). All optional: companies registered before it keep working.

    /** Commercial name shown on documents when it differs from the registered (legal) name. */
    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_form", length = 30)
    private CompanyLegalForm legalForm;

    /** Registration number at the legal entities registry (Conservatória). */
    @Column(name = "registry_number", length = 60)
    private String registryNumber;

    @Column(name = "share_capital", precision = 19, scale = 2)
    private BigDecimal shareCapital;

    /** Activity licence (alvará) number. */
    @Column(name = "activity_license", length = 60)
    private String activityLicense;

    /** Economic activity code (CAE). */
    @Column(name = "activity_code", length = 20)
    private String activityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_regime", length = 20)
    private VatRegime vatRegime;

    /** Tax office the company reports to. */
    @Column(name = "tax_office", length = 120)
    private String taxOffice;

    @Column(name = "neighborhood", length = 120)
    private String neighborhood;

    @Column(name = "city", length = 120)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "province", length = 30)
    private MozambiqueProvince province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "secondary_phone", length = 40)
    private String secondaryPhone;

    @Column(name = "website", length = 200)
    private String website;

    /** Person the platform contacts about this company (billing, support). */
    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    /** Logótipo da empresa (imagem reduzida) para os cabeçalhos dos documentos. Espelha Product.imageData. */
    @Column(name = "logo", length = 16_777_216)
    private byte[] logo;

    /** Empresa inactiva não pode iniciar sessão (suspensa pelo superadmin / falta de pagamento). */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Bloqueio de stock: quando {@code true}, utilizadores sem papel ADMIN não vêem as quantidades
     * de stock (contagem cega / inventário). O ADMIN vê sempre. Controlado pelo ADMIN.
     */
    @Column(name = "stock_count_locked", nullable = false)
    private boolean stockCountLocked = false;

    /** Name printed as the document title: the trade name when set, otherwise the legal name. */
    public String displayName() {
        return hasText(tradeName) ? tradeName.trim() : name;
    }

    /**
     * Single-line postal address: street, neighbourhood, city with postal code and province.
     * The province is left out when it only repeats the city ("Maputo, Maputo Cidade").
     * Returns {@code null} when nothing is filled in.
     */
    public String formattedAddress() {
        List<String> parts = new ArrayList<>();
        if (hasText(address)) parts.add(address.trim());
        if (hasText(neighborhood)) parts.add(neighborhood.trim());
        String cityPart = hasText(city) ? city.trim() : null;
        if (hasText(postalCode)) cityPart = postalCode.trim() + (cityPart == null ? "" : " " + cityPart);
        if (cityPart != null) parts.add(cityPart);
        if (province != null && (!hasText(city)
                || !province.label().toLowerCase().contains(city.trim().toLowerCase()))) {
            parts.add(province.label());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
