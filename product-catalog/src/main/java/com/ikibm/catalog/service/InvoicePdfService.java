package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.InvoiceCurrencySummary;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.InvoiceItem;
import com.ikibm.catalog.util.InvoiceCalculator;
import com.ikibm.catalog.util.InvoiceStatusText;
import com.ikibm.catalog.util.PriceFormatter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Fatura PDF üreticisi — mevcut PdfService (teklif PDF'i) ile AYNI görsel dil ve OpenPDF altyapısını
 * kullanır (renkler, gömülü Türkçe Roboto fontu, tablo/özet hücre yardımcıları PdfService'ten birebir
 * kopyalanmıştır çünkü PdfService'in kendisi QuotePdfRequest'e sıkı bağlı, doğrudan metod paylaşımı
 * mümkün değil). GERÇEK bir e-Fatura/e-Arşiv XML'i DEĞİLDİR — bu açıkça PDF üzerinde belirtilir.
 */
@Service
public class InvoicePdfService {

    private static final Color SIGNAL = new Color(31, 95, 209);
    private static final Color INK = new Color(16, 21, 29);
    private static final Color INK_500 = new Color(107, 118, 132);
    private static final Color LINE = new Color(227, 231, 236);
    private static final Color ALT = new Color(247, 248, 250);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr", "TR"));
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final PriceFormatter priceFormatter;
    private final InvoiceStatusText invoiceStatusText;
    private final BaseFont regular;
    private final BaseFont bold;

    public InvoicePdfService(PriceFormatter priceFormatter, InvoiceStatusText invoiceStatusText) throws Exception {
        this.priceFormatter = priceFormatter;
        this.invoiceStatusText = invoiceStatusText;
        this.regular = loadFont("fonts/Roboto-Regular.ttf");
        this.bold = loadFont("fonts/Roboto-Bold.ttf");
    }

    private BaseFont loadFont(String path) throws Exception {
        byte[] bytes = new ClassPathResource(path).getInputStream().readAllBytes();
        return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, bytes, null);
    }

    private Font font(BaseFont bf, float size, Color color) {
        Font f = new Font(bf, size);
        f.setColor(color);
        return f;
    }

    public byte[] generate(Invoice invoice) {
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ---- Başlık: satıcı (sol) + FATURA (sağ) ----
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1, 1});

            Paragraph left = new Paragraph();
            left.add(new Chunk(nz(invoice.getSellerCompanyName(), "—"), font(bold, 14, INK)));
            left.add(new Chunk("\nVergi No: " + nz(invoice.getSellerTaxNumber(), "—"), font(regular, 9, INK_500)));
            left.add(new Chunk("\nVergi Dairesi: " + nz(invoice.getSellerTaxOffice(), "—"), font(regular, 9, INK_500)));
            left.add(new Chunk("\n" + nz(invoice.getSellerAddress(), "—"), font(regular, 9, INK_500)));
            header.addCell(borderless(left, Element.ALIGN_LEFT));

            Paragraph right = new Paragraph();
            right.add(new Chunk("FATURA\n", font(bold, 20, SIGNAL)));
            right.add(new Chunk("Fatura No: " + nz(invoice.getInvoiceNumber(), "—") + "\n", font(regular, 9.5f, INK)));
            right.add(new Chunk("Tarih: " + DATE.format(invoice.getIssuedAt().atZone(ZONE)) + "\n", font(regular, 9.5f, INK)));
            String due = invoice.getDueDate() != null ? DATE.format(invoice.getDueDate().atZone(ZONE)) : "—";
            right.add(new Chunk("Vade Tarihi: " + due + "\n", font(regular, 9.5f, INK)));
            right.add(new Chunk("Durum: " + invoiceStatusText.label(invoice.getStatus()), font(bold, 9.5f, INK)));
            header.addCell(borderless(right, Element.ALIGN_RIGHT));
            doc.add(header);

            doc.add(new LineSeparator(0.8f, 100, SIGNAL, Element.ALIGN_CENTER, -4));
            doc.add(new Paragraph(" ", font(regular, 4, INK)));
            doc.add(new Paragraph(
                    "Bu belge, gerçek bir e-Fatura/e-Arşiv veya GİB entegrasyonu çıktısı değildir; sadece dahili bir referans belgesidir.",
                    font(regular, 7.5f, INK_500)));
            doc.add(new Paragraph(" ", font(regular, 6, INK)));

            // ---- Müşteri bloğu ----
            doc.add(new Paragraph("MÜŞTERİ", font(bold, 10, INK_500)));
            Paragraph customer = new Paragraph();
            customer.add(new Chunk(nz(invoice.getCustomerCompanyName(), "—") + "\n", font(bold, 10.5f, INK)));
            customer.add(new Chunk("Vergi No/TCKN: " + nz(invoice.getCustomerTaxNumber(), "—") + "\n", font(regular, 9.5f, INK_500)));
            customer.add(new Chunk("Vergi Dairesi: " + nz(invoice.getCustomerTaxOffice(), "—") + "\n", font(regular, 9.5f, INK_500)));
            customer.add(new Chunk(nz(invoice.getCustomerAddress(), "—"), font(regular, 9.5f, INK_500)));
            doc.add(customer);
            doc.add(new Paragraph(" ", font(regular, 6, INK)));

            // ---- Kalem tablosu ----
            PdfPTable table = new PdfPTable(new float[]{16, 34, 8, 14, 8, 12, 14});
            table.setWidthPercentage(100);
            for (String h : new String[]{"Ürün Kodu", "Ürün Adı", "Miktar", "Birim Fiyat", "KDV %", "KDV Tutarı", "Satır Toplamı"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, font(bold, 8, Color.WHITE)));
                hc.setBackgroundColor(SIGNAL);
                hc.setPadding(4);
                hc.setBorderColor(LINE);
                table.addCell(hc);
            }
            int idx = 0;
            for (InvoiceItem item : invoice.getItems()) {
                Color bg = (idx++ % 2 == 1) ? ALT : Color.WHITE;
                addCell(table, nz(item.getProductCode(), ""), bg, Element.ALIGN_LEFT);
                addCell(table, nz(item.getProductName(), ""), bg, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(item.getQuantity()), bg, Element.ALIGN_RIGHT);
                addCell(table, priceFormatter.format(item.getUnitPrice(), item.getCurrency()), bg, Element.ALIGN_RIGHT);
                addCell(table, "%" + item.getVatRate().stripTrailingZeros().toPlainString(), bg, Element.ALIGN_CENTER);
                addCell(table, priceFormatter.format(item.getVatAmount(), item.getCurrency()), bg, Element.ALIGN_RIGHT);
                addCellBold(table, priceFormatter.format(item.getGrandTotal(), item.getCurrency()), bg, Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(new Paragraph(" ", font(regular, 8, INK)));

            // ---- Özet: farklı para birimleri KESİNLİKLE toplanmaz, her biri ayrı bloktur ----
            List<InvoiceCurrencySummary> summaries = InvoiceCalculator.summarize(invoice.getItems());
            PdfPTable summaryWrap = new PdfPTable(1);
            summaryWrap.setWidthPercentage(45);
            summaryWrap.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (summaries.isEmpty()) {
                summaryWrap.addCell(borderless(new Paragraph("Fatura kalemi bulunmuyor.", font(regular, 9.5f, INK_500)), Element.ALIGN_RIGHT));
            } else {
                for (InvoiceCurrencySummary s : summaries) {
                    summaryWrap.addCell(summaryRow("Ara Toplam (" + s.currency() + ")", priceFormatter.format(s.subtotal(), s.currency()), false));
                    summaryWrap.addCell(summaryRow("KDV (" + s.currency() + ")", "+" + priceFormatter.format(s.vatAmount(), s.currency()), false));
                    summaryWrap.addCell(summaryRow("Genel Toplam (" + s.currency() + ")", priceFormatter.format(s.grandTotal(), s.currency()), true));
                }
            }
            doc.add(summaryWrap);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Fatura PDF'i oluşturulamadı", e);
        }
    }

    private PdfPCell summaryRow(String label, String value, boolean total) {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        try { row.setWidths(new float[]{1.4f, 1}); } catch (DocumentException ignored) {}
        Font lf = font(total ? bold : regular, total ? 11 : 9.5f, total ? SIGNAL : INK_500);
        row.addCell(borderless(new Paragraph(label, lf), Element.ALIGN_LEFT));
        row.addCell(borderless(new Paragraph(value, lf), Element.ALIGN_RIGHT));
        PdfPCell wrap = new PdfPCell(row);
        wrap.setBorder(total ? Rectangle.TOP : Rectangle.NO_BORDER);
        wrap.setBorderColor(LINE);
        wrap.setPadding(2);
        return wrap;
    }

    private PdfPCell borderless(Element el, int align) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.addElement(el);
        return c;
    }

    private void addCell(PdfPTable t, String text, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(regular, 8.5f, INK)));
        c.setBackgroundColor(bg);
        c.setBorderColor(LINE);
        c.setPadding(3);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void addCellBold(PdfPTable t, String text, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(bold, 8.5f, INK)));
        c.setBackgroundColor(bg);
        c.setBorderColor(LINE);
        c.setPadding(3);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private String nz(String s, String def) {
        return (s != null && !s.isBlank()) ? s : def;
    }
}
