package com.example.ticketbox.entity;

import com.example.ticketbox.enums.PaymentMethods;
import com.example.ticketbox.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payments_transaction_id",
                columnNames = "transaction_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payments extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethods method;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "checkout_provider", length = 20)
    private String checkoutProvider;

    @Column(name = "sepay_reference", length = 40, unique = true)
    private String sepayReference;
}
