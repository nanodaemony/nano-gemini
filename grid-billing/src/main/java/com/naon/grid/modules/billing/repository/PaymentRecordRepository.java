package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByOrderId(Long orderId);
    List<PaymentRecord> findByTransactionId(String transactionId);
}
