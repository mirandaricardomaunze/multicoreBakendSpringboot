package com.phcpro.modules.pos.model;

import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "till_sessions")
@Getter
@Setter
public class TillSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator", nullable = false)
    private String operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "open_date", nullable = false)
    private LocalDateTime openDate = LocalDateTime.now();

    @Column(name = "close_date")
    private LocalDateTime closeDate;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance_real", precision = 12, scale = 2)
    private BigDecimal closingBalanceReal;

    @Column(name = "closing_balance_expected", precision = 12, scale = 2)
    private BigDecimal closingBalanceExpected;

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, CLOSED

    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;
}
