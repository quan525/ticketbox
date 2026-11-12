package com.example.ticketbox.payment.sepay;
import com.example.ticketbox.entity.SepayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SepayEventRepository extends JpaRepository<SepayEvent, Long> {
    boolean existsByModeAndTransactionId(String mode, Long transactionId);
    List<SepayEvent> findTop100ByOrderByCreatedAtDesc();
}
