package com.phcpro.modules.pos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "till_movements")
@Getter
@Setter
public class TillMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "till_session_id", nullable = false)
    private TillSession tillSession;

    @Column(name = "movement_type", nullable = false)
    private String movementType; // SALE, SUPRIMENTO (entry), SANGRIA (exit)

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate = LocalDateTime.now();

    @Column(name = "description")
    private String description;
}
