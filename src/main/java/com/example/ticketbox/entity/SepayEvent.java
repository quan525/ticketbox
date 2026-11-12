package com.example.ticketbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Getter @Setter
@Table(name = "sepay_events", uniqueConstraints = @UniqueConstraint(name = "uk_sepay_event_mode_transaction", columnNames = {"mode", "sepay_transaction_id"}))
public class SepayEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10) private String mode;
    @Column(name = "sepay_transaction_id", nullable = false) private Long transactionId;
    @Column(name = "payment_id") private Long paymentId;
    @Column(precision = 19, scale = 2) private BigDecimal amount;
    @Column(length = 40) private String reference;
    @Column(nullable = false, length = 40) private String outcome;
}
