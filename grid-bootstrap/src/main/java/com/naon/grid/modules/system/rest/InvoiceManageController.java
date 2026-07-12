package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.billing.domain.Invoice;
import com.naon.grid.modules.billing.service.InvoiceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
@Api(tags = "管理：发票管理")
public class InvoiceManageController {

    private final InvoiceService invoiceService;

    @ApiOperation("发票列表")
    @GetMapping
    public ResponseEntity<Page<Invoice>> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceService.listAll(region, status, startDate, endDate,
                PageRequest.of(page, size)));
    }

    @ApiOperation("发票详情")
    @GetMapping("/{invoiceNo}")
    public ResponseEntity<Invoice> detail(@PathVariable String invoiceNo) {
        return invoiceService.findByInvoiceNo(invoiceNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ApiOperation("重新生成PDF")
    @PostMapping("/{invoiceNo}/regenerate")
    public ResponseEntity<Invoice> regeneratePdf(@PathVariable String invoiceNo) {
        return ResponseEntity.ok(invoiceService.regeneratePdf(invoiceNo));
    }

    @ApiOperation("作废发票")
    @PostMapping("/{invoiceNo}/void")
    public ResponseEntity<Void> voidInvoice(@PathVariable String invoiceNo) {
        invoiceService.voidInvoice(invoiceNo);
        return ResponseEntity.ok().build();
    }
}
