package com.example.ticketbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TicketboxApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.ticketbox.payment.service.MockPaymentService mockPayments;

    @Test void mockPaymentsAreDisabledByDefault() {
        org.junit.jupiter.api.Assertions.assertFalse(mockPayments.enabled());
    }

    @Test
    void contextLoads() {
    }

}
