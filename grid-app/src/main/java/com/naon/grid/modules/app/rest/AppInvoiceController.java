package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.domain.Invoice;
import com.naon.grid.modules.billing.service.InvoiceService;
import com.naon.grid.modules.billing.service.dto.InvoiceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/invoices")
@Api(tags = "用户：发票接口")
public class AppInvoiceController {

    private final InvoiceService invoiceService;

    @ApiOperation("我的发票列表")
    @GetMapping
    public ResponseEntity<Page<InvoiceVO>> listMyInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invoiceService.listByUser(userId, PageRequest.of(page, size)));
    }

    @ApiOperation("发票详情")
    @GetMapping("/{invoiceNo}")
    public ResponseEntity<InvoiceVO> getInvoice(@PathVariable String invoiceNo) {
        return invoiceService.findByInvoiceNo(invoiceNo)
                .map(inv -> ResponseEntity.ok(toVO(inv)))
                .orElse(ResponseEntity.notFound().build());
    }

    @ApiOperation("下载发票PDF")
    @GetMapping("/{invoiceNo}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String invoiceNo) {
        return invoiceService.findByInvoiceNo(invoiceNo)
                .filter(inv -> inv.getPdfUrl() != null)
                .map(inv -> {
                    // TODO: fetch PDF bytes from OSS using pdfUrl
                    byte[] pdfBytes = new byte[0]; // placeholder
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", invoiceNo + ".pdf");
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private InvoiceVO toVO(Invoice inv) {
        return com.naon.grid.modules.billing.service.dto.InvoiceVO.builder()
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
}
