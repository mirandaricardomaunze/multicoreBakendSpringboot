package mz.multicore.erp.modules.company.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    /** Logótipo da empresa (imagem reduzida) para os cabeçalhos dos documentos. Espelha Product.imageData. */
    @Column(name = "logo")
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
}
