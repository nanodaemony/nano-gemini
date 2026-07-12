package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.Invoice;
import com.naon.grid.modules.billing.service.dto.InvoiceVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface InvoiceService {
    Invoice generateInvoice(String orderNo);
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    Optional<Invoice> findByOrderId(Long orderId);
    Page<InvoiceVO> listByUser(Long userId, Pageable pageable);
    Page<Invoice> listAll(String region, String status, String startDate, String endDate, Pageable pageable);
    Invoice regeneratePdf(String invoiceNo);
    void voidInvoice(String invoiceNo);
}
