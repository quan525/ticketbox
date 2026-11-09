package com.example.ticketbox.payment.service;

import com.example.ticketbox.enums.PaymentMethods;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Centralizes the methods currently supported by the API. Gateway-specific
 * implementations can replace this validation without changing payment state logic.
 */
@Component
public class PaymentMethodFactory {
    private static final EnumSet<PaymentMethods> SUPPORTED_METHODS =
            EnumSet.allOf(PaymentMethods.class);

    public PaymentMethods requireSupported(PaymentMethods method) {
        if (method == null || !SUPPORTED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Unsupported payment method");
        }
        return method;
    }
}
