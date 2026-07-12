package com.naon.grid.modules.app.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published after a successful payment to trigger async invoice generation.
 */
@Getter
public class InvoiceGenerateEvent extends ApplicationEvent {
    private final String orderNo;

    public InvoiceGenerateEvent(Object source, String orderNo) {
        super(source);
        this.orderNo = orderNo;
    }
}
