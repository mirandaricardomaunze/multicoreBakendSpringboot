package mz.multicore.erp.architecture.version;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registo de que uma empresa foi vista a usar uma dada versão do programa.
 *
 * <p><b>Para que serve:</b> antes de subir a versão mínima é preciso saber quem fica de fora.
 * Sem isto, decidir era às cegas — podia-se bloquear cinco lojas sem sequer saber que existiam
 * nessa versão. Uma linha por (empresa, versão): não é um histórico de acessos, é uma fotografia
 * de quem está em quê.
 */
@Entity
@Table(name = "client_version_sightings", uniqueConstraints = @UniqueConstraint(
        name = "uk_client_version_company_version", columnNames = {"company_id", "client_version"}))
@Getter
@Setter
public class ClientVersionSighting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empresa que estava activa no pedido. */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "client_version", nullable = false, length = 40)
    private String clientVersion;

    /** Último utilizador visto nesta versão — ajuda a saber a quem telefonar. */
    @Column(name = "last_username", length = 80)
    private String lastUsername;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
}
