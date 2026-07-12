package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.Invoice;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.InvoiceRepository;
import com.naon.grid.modules.billing.service.InvoicePdfGenerator;
import com.naon.grid.modules.billing.service.InvoiceService;
import com.naon.grid.modules.billing.service.dto.InvoiceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final GridOrderRepository orderRepository;
    private final InvoicePdfGenerator pdfGenerator;

    @Override
    @Transactional
    public Invoice generateInvoice(String orderNo) {
        GridOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));

        // Check if invoice already exists
        invoiceRepository.findByOrderId(order.getId()).ifPresent(existing -> {
            log.info("Invoice already exists for order {}", orderNo);
        });

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setOrderId(order.getId());
        invoice.setUserId(order.getUserId());
        invoice.setOrgId(order.getOrgId());
        invoice.setCurrency(order.getCurrency());
        invoice.setSubtotal(order.getSubtotal() != null ? order.getSubtotal() : order.getAmount());
        invoice.setTaxAmount(order.getTaxAmount() != null ? order.getTaxAmount() : java.math.BigDecimal.ZERO);
        invoice.setTotalAmount(order.getAmount());
        invoice.setRegion(order.getRegion());

        // Choose format based on region
        if ("C".equals(order.getRegion())) {
            invoice.setInvoiceFormat("CHINESE");
            invoice.setInvoiceType("SELF_GEN");
        } else {
            invoice.setInvoiceFormat("INTERNATIONAL");
            invoice.setInvoiceType("FASTSPRING");
        }
        if (order.getOrgId() != null) {
            invoice.setInvoiceFormat("INSTITUTION");
            invoice.setInvoiceType("SELF_GEN");
        }

        invoice.setSellerName("YourRoad 有路中文");
        invoice.setStatus("ISSUED");
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setNotes("Thank you for your purchase. / 感谢您的购买。");

        // Generate PDF for self-generated invoices only (non-FastSpring)
        if (!"FASTSPRING".equals(invoice.getInvoiceType())) {
            try {
                byte[] pdfBytes = pdfGenerator.generatePdf(invoice, invoice.getInvoiceFormat());
                // TODO: upload to OSS and set pdfUrl
                invoice.setPdfUrl(null); // Will be set after OSS upload
                log.info("Invoice PDF generated for {} ({} bytes)", invoice.getInvoiceNo(), pdfBytes.length);
            } catch (Exception e) {
                log.error("Failed to generate invoice PDF for {}", invoice.getInvoiceNo(), e);
                // Don't block: invoice record is created even if PDF generation fails
            }
        }

        invoiceRepository.save(invoice);
        log.info("Invoice created: {} for order {}", invoice.getInvoiceNo(), orderNo);
        return invoice;
    }

    @Override
    public Optional<Invoice> findByInvoiceNo(String invoiceNo) {
        return invoiceRepository.findByInvoiceNo(invoiceNo);
    }

    @Override
    public Optional<Invoice> findByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }

    @Override
    public Page<InvoiceVO> listByUser(Long userId, Pageable pageable) {
        return invoiceRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable)
                .map(this::toVO);
    }

    @Override
    public Page<Invoice> listAll(String region, String status, String startDate, String endDate, Pageable pageable) {
        // TODO: Add JPA Specification for dynamic filtering
        return invoiceRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Invoice regeneratePdf(String invoiceNo) {
        Invoice invoice = invoiceRepository.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new RuntimeException("发票不存在: " + invoiceNo));
        if (!"FASTSPRING".equals(invoice.getInvoiceType())) {
            byte[] pdfBytes = pdfGenerator.generatePdf(invoice, invoice.getInvoiceFormat());
            // TODO: re-upload to OSS
            log.info("Invoice PDF regenerated for {} ({} bytes)", invoiceNo, pdfBytes.length);
        }
        return invoice;
    }

    @Override
    @Transactional
    public void voidInvoice(String invoiceNo) {
        Invoice invoice = invoiceRepository.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new RuntimeException("发票不存在: " + invoiceNo));
        invoice.setStatus("VOIDED");
        invoiceRepository.save(invoice);
        log.info("Invoice voided: {}", invoiceNo);
    }

    private InvoiceVO toVO(Invoice inv) {
        return InvoiceVO.builder()
                .invoiceNo(inv.getInvoiceNo())
                .invoiceFormat(inv.getInvoiceFormat())
                .currency(inv.getCurrency())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .buyerName(inv.getBuyerName())
                .buyerTaxId(inv.getBuyerTaxId())
                .buyerEmail(inv.getBuyerEmail())
                .status(inv.getStatus())
                .pdfUrl(inv.getPdfUrl())
                .fastspringUrl(inv.getFastspringUrl())
                .issuedAt(inv.getIssuedAt())
                .build();
    }

    private String generateInvoiceNo() {
        return "INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}
