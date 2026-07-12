package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    Optional<Invoice> findByOrderId(Long orderId);
    Page<Invoice> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    Page<Invoice> findByOrgIdOrderByCreateTimeDesc(Integer orgId, Pageable pageable);
}
