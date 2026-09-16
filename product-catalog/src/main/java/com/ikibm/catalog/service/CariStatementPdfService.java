package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.CariStatementData;
import com.ikibm.catalog.dto.CariStatementLine;
import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.InvoiceItem;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
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
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Cari hesap ekstresi PDF üreticisi — mevcut InvoicePdfService/PdfService ile AYNI görsel dil ve
 * OpenPDF altyapısını kullanır (renkler, gömülü Türkçe Roboto fontu, tablo/özet hücre yardımcıları
 * birebir aynı desenle kopyalanmıştır — InvoicePdfService'in kendi Javadoc'unda açıkladığı gibi bu
 * servisler birbirine bağımlı olmadığı için doğrudan metod paylaşımı yapılmıyor).
 *
 * SALT RENDER: Bu sınıf hiçbir sorgu/hesaplama yapmaz, sadece CariStatementService'in ürettiği
 * CariStatementData'yı PDF'e döker. Bakiye/toplam değerleri olduğu gibi kullanılır, yeniden
 * hesaplanmaz (bkz. CariStatementData Javadoc'u).
 */
@Service
public class CariStatementPdfService {

    private static final Color SIGNAL = new Color(31, 95, 209);
    private static final Color INK = new Color(16, 21, 29);
    private static final Color INK_500 = new Color(107, 118, 132);
    private static final Color LINE = new Color(227, 231, 236);
    private static final Color ALT = new Color(247, 248, 250);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr", "TR"));
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final PriceFormatter priceFormatter;
    private final BaseFont regular;
    private final BaseFont bold;

    public CariStatementPdfService(PriceFormatter priceFormatter) throws Exception {
        this.priceFormatter = priceFormatter;
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

    public byte[] generate(CariStatementData data) {
        try {
            Document doc = new Document(PageSize.A4, 40, 36, 40, 36);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, data);
            addLedgerTable(doc, data);
            addSummary(doc, data);
            if (data.detailed()) {
                addDetailSections(doc, data);
            }

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Cari ekstre PDF'i oluşturulamadı", e);
        }
    }

    private void addHeader(Document doc, CariStatementData data) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 1});

        Paragraph left = new Paragraph();
        left.add(new Chunk(nz(data.customer().getCompanyName(), data.customer().getDisplayName()), font(bold, 14, INK)));
        left.add(new Chunk("\n" + nz(data.customer().getEmail(), "—"), font(regular, 9, INK_500)));
        if (nn(data.customer().getPhone())) {
            left.add(new Chunk("\n" + data.customer().getPhone(), font(regular, 9, INK_500)));
        }
        header.addCell(borderless(left, Element.ALIGN_LEFT));

        Paragraph right = new Paragraph();
        right.add(new Chunk("CARİ HESAP EKSTRESİ\n", font(bold, 18, SIGNAL)));
        right.add(new Chunk("Rapor Tarihi: " + DATE.format(data.reportGeneratedAt().atZone(ZONE)) + "\n", font(regular, 9.5f, INK)));
        if (data.filterFrom() != null || data.filterTo() != null) {
            String from = data.filterFrom() != null ? DATE.format(data.filterFrom().atZone(ZONE)) : "…";
            String to = data.filterTo() != null ? DATE.format(data.filterTo().minusSeconds(1).atZone(ZONE)) : "…";
            right.add(new Chunk("Dönem: " + from + " – " + to + "\n", font(regular, 9.5f, INK)));
        }
        right.add(new Chunk("Para Birimi: " + data.account().getCurrency() + "\n", font(regular, 9.5f, INK)));
        right.add(new Chunk("Rapor Türü: " + (data.detailed() ? "Detaylı" : "Detaysız"), font(bold, 9.5f, INK)));
        header.addCell(borderless(right, Element.ALIGN_RIGHT));
        doc.add(header);

        doc.add(new LineSeparator(0.8f, 100, SIGNAL, Element.ALIGN_CENTER, -4));
        doc.add(new Paragraph(" ", font(regular, 6, INK)));
    }

    private void addLedgerTable(Document doc, CariStatementData data) throws DocumentException {
        doc.add(new Paragraph("CARİ HAREKETLER", font(bold, 10, INK_500)));
        doc.add(new Paragraph(" ", font(regular, 3, INK)));

        if (data.lines().isEmpty()) {
            doc.add(new Paragraph("Seçilen aralıkta cari hareket bulunmuyor.", font(regular, 9.5f, INK_500)));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{12, 12, 30, 15, 15, 16});
        table.setWidthPercentage(100);
        for (String h : new String[]{"Tarih", "İşlem", "Açıklama", "Borç", "Alacak", "Bakiye"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h, font(bold, 8, Color.WHITE)));
            hc.setBackgroundColor(SIGNAL);
            hc.setPadding(4);
            hc.setBorderColor(LINE);
            table.addCell(hc);
        }
        int idx = 0;
        for (CariStatementLine line : data.lines()) {
            CariTransaction t = line.transaction();
            Color bg = (idx++ % 2 == 1) ? ALT : Color.WHITE;
            boolean isDebtColumn = t.getType() == CariTransactionType.DEBT || t.getType() == CariTransactionType.ADJUSTMENT;
            addCell(table, DATE.format(t.getTransactionDate().atZone(ZONE)), bg, Element.ALIGN_LEFT);
            addCell(table, t.getType().name(), bg, Element.ALIGN_LEFT);
            addCell(table, nz(t.getDescription(), "—"), bg, Element.ALIGN_LEFT);
            addCell(table, isDebtColumn ? priceFormatter.format(t.getAmount(), t.getCurrency()) : "—", bg, Element.ALIGN_RIGHT);
            addCell(table, !isDebtColumn ? priceFormatter.format(t.getAmount(), t.getCurrency()) : "—", bg, Element.ALIGN_RIGHT);
            addCell(table, t.getBalanceAfter() != null ? priceFormatter.format(t.getBalanceAfter(), t.getCurrency()) : "—", bg, Element.ALIGN_RIGHT);
        }
        doc.add(table);
        doc.add(new Paragraph(" ", font(regular, 8, INK)));
    }

    private void addSummary(Document doc, CariStatementData data) throws DocumentException {
        PdfPTable summaryWrap = new PdfPTable(1);
        summaryWrap.setWidthPercentage(50);
        summaryWrap.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Map<Currency, BigDecimal> debt = data.totalDebtByCurrency();
        Map<Currency, BigDecimal> credit = data.totalCreditByCurrency();
        if (debt.isEmpty() && credit.isEmpty()) {
            summaryWrap.addCell(borderless(new Paragraph("Seçilen aralıkta hareket bulunmuyor.", font(regular, 9.5f, INK_500)), Element.ALIGN_RIGHT));
        } else {
            for (Currency c : Currency.values()) {
                BigDecimal d = debt.getOrDefault(c, BigDecimal.ZERO);
                BigDecimal cr = credit.getOrDefault(c, BigDecimal.ZERO);
                if (d.signum() == 0 && cr.signum() == 0) continue;
                summaryWrap.addCell(summaryRow("Toplam Borç (" + c + ")", priceFormatter.format(d, c), false));
                summaryWrap.addCell(summaryRow("Toplam Alacak (" + c + ")", priceFormatter.format(cr, c), false));
            }
        }
        summaryWrap.addCell(summaryRow("Güncel Bakiye (" + data.account().getCurrency() + ")",
                priceFormatter.format(data.account().getCurrentBalance(), data.account().getCurrency()), true));
        doc.add(summaryWrap);
    }

    private void addDetailSections(Document doc, CariStatementData data) throws DocumentException {
        boolean any = data.lines().stream().anyMatch(l -> l.order() != null || l.invoice() != null);
        if (!any) return;

        doc.add(new Paragraph(" ", font(regular, 10, INK)));
        doc.add(new LineSeparator(0.5f, 100, LINE, Element.ALIGN_CENTER, -4));
        doc.add(new Paragraph(" ", font(regular, 6, INK)));
        doc.add(new Paragraph("İŞLEM DETAYLARI", font(bold, 12, SIGNAL)));
        doc.add(new Paragraph(" ", font(regular, 6, INK)));

        for (CariStatementLine line : data.lines()) {
            if (line.order() != null) {
                addOrderDetail(doc, line.transaction(), line.order());
            } else if (line.invoice() != null) {
                addInvoiceDetail(doc, line.transaction(), line.invoice());
            }
        }
    }

    private void addOrderDetail(Document doc, CariTransaction t, Order order) throws DocumentException {
        Paragraph title = new Paragraph();
        title.add(new Chunk("Sipariş #" + order.getId(), font(bold, 10.5f, INK)));
        title.add(new Chunk("  ·  Sipariş Tarihi: " + DATE.format(order.getCreatedAt().atZone(ZONE)), font(regular, 9, INK_500)));
        title.add(new Chunk("  ·  Durum: " + order.getStatus(), font(regular, 9, INK_500)));
        title.add(new Chunk("  ·  Cari Harekete Yansıyan Borç: " + priceFormatter.format(t.getAmount(), t.getCurrency()), font(regular, 9, INK_500)));
        doc.add(title);

        PdfPTable table = new PdfPTable(new float[]{14, 30, 8, 14, 12, 12, 14});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        for (String h : new String[]{"Ürün Kodu", "Ürün Adı", "Miktar", "Birim Fiyat", "KDV", "Fiyat Listesi", "Satır Toplamı"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h, font(bold, 7.5f, Color.WHITE)));
            hc.setBackgroundColor(INK_500);
            hc.setPadding(3);
            hc.setBorderColor(LINE);
            table.addCell(hc);
        }
        int idx = 0;
        for (OrderItem item : order.getItems()) {
            Color bg = (idx++ % 2 == 1) ? ALT : Color.WHITE;
            addCell(table, nz(item.getProductCode(), ""), bg, Element.ALIGN_LEFT);
            addCell(table, nz(item.getProductName(), ""), bg, Element.ALIGN_LEFT);
            addCell(table, String.valueOf(item.getQty()), bg, Element.ALIGN_RIGHT);
            addCell(table, priceFormatter.format(item.getUnitPrice(), item.getCurrency()), bg, Element.ALIGN_RIGHT);
            addCell(table, Boolean.TRUE.equals(item.getPriceIncludesVat()) ? "Dahil" : "Hariç", bg, Element.ALIGN_CENTER);
            addCell(table, nz(item.getPriceListName(), "—"), bg, Element.ALIGN_LEFT);
            addCellBold(table, priceFormatter.format(item.getTotalPrice(), item.getCurrency()), bg, Element.ALIGN_RIGHT);
        }
        doc.add(table);
    }

    private void addInvoiceDetail(Document doc, CariTransaction t, Invoice invoice) throws DocumentException {
        Paragraph title = new Paragraph();
        title.add(new Chunk("Fatura " + nz(invoice.getInvoiceNumber(), "#" + invoice.getId()), font(bold, 10.5f, INK)));
        title.add(new Chunk("  ·  Fatura Tarihi: " + DATE.format(invoice.getIssuedAt().atZone(ZONE)), font(regular, 9, INK_500)));
        String due = invoice.getDueDate() != null ? DATE.format(invoice.getDueDate().atZone(ZONE)) : "—";
        title.add(new Chunk("  ·  Vade Tarihi: " + due, font(regular, 9, INK_500)));
        title.add(new Chunk("  ·  Cari Harekete Yansıyan Tutar: " + priceFormatter.format(t.getAmount(), t.getCurrency()), font(regular, 9, INK_500)));
        doc.add(title);

        PdfPTable table = new PdfPTable(new float[]{14, 28, 8, 12, 8, 12, 14});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        for (String h : new String[]{"Ürün Kodu", "Ürün Adı", "Miktar", "Birim Fiyat", "KDV %", "KDV Tutarı", "Satır Toplamı"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h, font(bold, 7.5f, Color.WHITE)));
            hc.setBackgroundColor(INK_500);
            hc.setPadding(3);
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

    private boolean nn(String s) {
        return s != null && !s.isBlank();
    }
}
