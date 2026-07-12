package com.naon.grid.modules.app.listener;

import com.naon.grid.modules.app.event.InvoiceGenerateEvent;
import com.naon.grid.modules.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async listener that generates invoices after payment success.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final InvoiceService invoiceService;

    @Async
    @EventListener
    public void handleInvoiceGenerate(InvoiceGenerateEvent event) {
        try {
            log.info("Async invoice generation triggered for order {}", event.getOrderNo());
            invoiceService.generateInvoice(event.getOrderNo());
        } catch (Exception e) {
            log.error("Async invoice generation failed for order {}", event.getOrderNo(), e);
        }
    }
}
