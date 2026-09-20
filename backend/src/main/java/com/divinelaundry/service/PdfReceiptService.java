package com.divinelaundry.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfReceiptService {
    private static final PDRectangle PAGE = PDRectangle.A4;
    private static final float LEFT = 48;
    private static final float TOP = PAGE.getHeight() - 48;
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final ZoneId businessZone;

    public PdfReceiptService(@Value("${app.business-zone:Asia/Kolkata}") String businessZone) {
        this.businessZone = ZoneId.of(businessZone);
    }

    public byte[] render(PaymentReceiptService.PaymentReceiptDocument receipt) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PAGE);
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                text(content, receipt.business().name(), LEFT, TOP, BOLD, 20);
                text(content, receipt.business().address(), LEFT, TOP - 22, REGULAR, 10);
                text(content, receipt.business().phone(), LEFT, TOP - 38, REGULAR, 10);
                text(content, "PAYMENT RECEIPT", 350, TOP, BOLD, 16);
                text(content, receipt.receiptNumber(), 350, TOP - 22, REGULAR, 10);
                float y = TOP - 92;
                text(content, "Receipt number", LEFT, y, BOLD, 10);
                text(content, receipt.receiptNumber(), 190, y, REGULAR, 10);
                text(content, "Payment date", LEFT, y - 22, BOLD, 10);
                text(content, date(receipt.paymentDate()), 190, y - 22, REGULAR, 10);
                text(content, "Order number", LEFT, y - 44, BOLD, 10);
                text(content, receipt.orderNumber(), 190, y - 44, REGULAR, 10);
                text(content, "Invoice number", LEFT, y - 66, BOLD, 10);
                text(content, safe(receipt.invoiceNumber(), "Not available"), 190, y - 66, REGULAR, 10);
                text(content, "Customer", LEFT, y - 110, BOLD, 10);
                text(content, receipt.customerName(), 190, y - 110, REGULAR, 10);
                text(content, receipt.customerPhone(), 190, y - 132, REGULAR, 10);
                text(content, joinAddress(receipt), 190, y - 154, REGULAR, 10);
                text(content, "Payment mode", LEFT, y - 198, BOLD, 10);
                text(content, receipt.paymentMode(), 190, y - 198, REGULAR, 10);
                text(content, "Reference", LEFT, y - 220, BOLD, 10);
                text(content, safe(receipt.transactionReference(), "Not provided"), 190, y - 220, REGULAR, 10);
                text(content, "Recorded by", LEFT, y - 242, BOLD, 10);
                text(content, receipt.createdBy(), 190, y - 242, REGULAR, 10);
                text(content, "Order total", LEFT, y - 296, BOLD, 10);
                text(content, money(receipt.orderTotal()), 300, y - 296, REGULAR, 10);
                text(content, "Previous outstanding", LEFT, y - 318, BOLD, 10);
                text(content, money(receipt.previousOutstanding()), 300, y - 318, REGULAR, 10);
                text(content, "Amount received", LEFT, y - 340, BOLD, 10);
                text(content, money(receipt.amountReceived()), 300, y - 340, BOLD, 11);
                text(content, "Remaining outstanding", LEFT, y - 362, BOLD, 10);
                text(content, money(receipt.remainingOutstanding()), 300, y - 362, BOLD, 11);
                text(content, "Payment status", LEFT, y - 384, BOLD, 10);
                text(content, receipt.paymentStatus(), 300, y - 384, BOLD, 11);
                text(content, "Thank you for choosing " + receipt.business().name() + ".", LEFT, 48, REGULAR, 9);
            }
            pdf.save(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("Could not create the payment receipt PDF", error);
        }
    }

    private String date(java.time.Instant value) {
        return value == null ? "Not set" : DATE_TIME.format(value.atZone(businessZone));
    }

    private static String joinAddress(PaymentReceiptService.PaymentReceiptDocument receipt) {
        String address = safe(receipt.customerAddress(), "");
        String area = safe(receipt.customerArea(), "");
        return (address + (address.isBlank() || area.isBlank() ? "" : ", ") + area).trim();
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.replaceAll("[\\r\\n]", " ");
        return normalized.length() > 120 ? normalized.substring(0, 117) + "..." : normalized;
    }

    private static void text(PDPageContentStream content, String value, float x, float y, PDFont font, float size)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(value, ""));
        content.endText();
    }

    private static String money(BigDecimal value) {
        return "INR " + (value == null ? BigDecimal.ZERO : value.setScale(2).toPlainString());
    }
}