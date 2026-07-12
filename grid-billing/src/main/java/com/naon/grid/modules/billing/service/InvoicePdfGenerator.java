package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Generates PDF invoices using Apache PDFBox.
 * For Phase 2, uses programmatic PDFBox API. Future enhancement: Thymeleaf → Flying Saucer → PDF.
 */
@Slf4j
@Component
public class InvoicePdfGenerator {

    /**
     * Generate a PDF invoice and return the bytes.
     * Currently generates a simple programmatic PDF; can be upgraded to HTML template rendering.
     */
    public byte[] generatePdf(Invoice invoice, String format) {
        log.info("Generating {} invoice PDF for {}", format, invoice.getInvoiceNo());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Load font with CJK support
                InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSansCJKsc-Regular.otf");
                PDType0Font font;
                if (fontStream != null) {
                    font = PDType0Font.load(document, fontStream, true);
                } else {
                    font = PDType0Font.load(document,
                            getClass().getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"),
                            true);
                }

                float y = 700;
                float margin = 50;

                cs.beginText();
                cs.setFont(font, 20);
                cs.newLineAtOffset(margin, y);
                String title = "CHINESE".equals(format) ? "发票 / Invoice" : "Commercial Invoice";
                cs.showText(title);
                cs.endText();
                y -= 30;

                // Invoice metadata
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice No: " + invoice.getInvoiceNo());
                cs.newLineAtOffset(0, -15);
                cs.showText("Date: " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : ""));
                cs.newLineAtOffset(0, -15);
                cs.showText("Seller: " + (invoice.getSellerName() != null ? invoice.getSellerName() : "YourRoad"));
                cs.endText();
                y -= 60;

                // Line items
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Description");
                cs.newLineAtOffset(250, 0);
                cs.showText("Amount");
                cs.endText();
                y -= 20;

                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Subscription / Digital Service");
                cs.newLineAtOffset(250, 0);
                cs.showText(invoice.getCurrency() + " " + invoice.getSubtotal().toPlainString());
                cs.endText();
                y -= 40;

                // Totals
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Subtotal: " + invoice.getCurrency() + " " + invoice.getSubtotal().toPlainString());
                cs.newLineAtOffset(0, -15);
                cs.showText("Tax: " + invoice.getCurrency() + " " +
                        (invoice.getTaxAmount() != null ? invoice.getTaxAmount().toPlainString() : "0.00"));
                cs.newLineAtOffset(0, -15);
                cs.setFont(font, 12);
                cs.showText("Total: " + invoice.getCurrency() + " " + invoice.getTotalAmount().toPlainString());
                cs.endText();
                y -= 50;

                // Notes
                if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                    cs.beginText();
                    cs.setFont(font, 8);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(invoice.getNotes());
                    cs.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for {}", invoice.getInvoiceNo(), e);
            throw new RuntimeException("Invoice PDF generation failed: " + e.getMessage(), e);
        }
    }
}
