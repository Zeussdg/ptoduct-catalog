package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.QuotePdfRequest;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Teklif PDF üreticisi (OpenPDF). React src/utils/quotePdf.js generateQuotePdf
 * düzenini yeniden üretir: satıcı başlığı, "FİYAT TEKLİFİ", müşteri bloğu,
 * kalem tablosu ve para birimi bazlı kâr marjı + %20 KDV özeti. Türkçe
 * karakterler gömülü Roboto (IDENTITY_H) ile.
 */
@Service
public class PdfService {

    private static final Color SIGNAL = new Color(31, 95, 209);
    private static final Color INK = new Color(16, 21, 29);
    private static final Color INK_500 = new Color(107, 118, 132);
    private static final Color LINE = new Color(227, 231, 236);
    private static final Color ALT = new Color(247, 248, 250);
    private static final BigDecimal VAT = new BigDecimal("0.20");

    private final PriceFormatter priceFormatter;
    private final BaseFont regular;
    private final BaseFont bold;

    public PdfService(PriceFormatter priceFormatter) throws Exception {
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

    public byte[] generate(QuotePdfRequest req) {
        double marginPct = req.margin();
        BigDecimal factor = BigDecimal.valueOf(1 + marginPct / 100);
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String no = "TKLF-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));

        QuotePdfRequest.Party seller = req.seller() != null ? req.seller() : new QuotePdfRequest.Party("", "", "", "");
        QuotePdfRequest.Party contact = req.contact() != null ? req.contact() : new QuotePdfRequest.Party("", "", "", "");
        List<QuotePdfRequest.Item> items = req.items() != null ? req.items() : List.of();

        try {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ---- Başlık: satıcı (sol) + FİYAT TEKLİFİ (sağ) ----
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1, 1});

            Paragraph left = new Paragraph();
            left.add(new Chunk(nz(seller.firma(), "—"), font(bold, 15, INK)));
            if (nn(seller.yetkili())) left.add(new Chunk("\nYetkili: " + seller.yetkili(), font(regular, 9, INK_500)));
            if (nn(seller.telefon())) left.add(new Chunk("\nTel: " + seller.telefon(), font(regular, 9, INK_500)));
            if (nn(seller.eposta())) left.add(new Chunk("\nE-posta: " + seller.eposta(), font(regular, 9, INK_500)));
            header.addCell(borderless(left, Element.ALIGN_LEFT));

            Paragraph right = new Paragraph();
            right.add(new Chunk("FİYAT TEKLİFİ\n", font(bold, 20, SIGNAL)));
            right.add(new Chunk("Tarih: " + date + "\n", font(regular, 9.5f, INK)));
            right.add(new Chunk("Teklif No: " + no, font(regular, 9.5f, INK)));
            header.addCell(borderless(right, Element.ALIGN_RIGHT));
            doc.add(header);

            LineSeparator sep = new LineSeparator(0.8f, 100, SIGNAL, Element.ALIGN_CENTER, -4);
            doc.add(sep);
            doc.add(new Paragraph(" ", font(regular, 6, INK)));

            // ---- Müşteri bloğu ----
            doc.add(new Paragraph("MÜŞTERİ", font(bold, 10, INK_500)));
            boolean anyContact = false;
            anyContact |= addContactRow(doc, "Firma", contact.firma());
            anyContact |= addContactRow(doc, "Yetkili", contact.yetkili());
            anyContact |= addContactRow(doc, "Telefon", contact.telefon());
            anyContact |= addContactRow(doc, "E-posta", contact.eposta());
            if (!anyContact) doc.add(new Paragraph("—", font(regular, 10.5f, INK_500)));
            doc.add(new Paragraph(" ", font(regular, 6, INK)));

            // ---- Kalem tablosu ----
            PdfPTable table = new PdfPTable(new float[]{22, 60, 24, 12, 24, 14, 24});
            table.setWidthPercentage(100);
            for (String h : new String[]{"Marka", "Ürün Adı", "Stok Kodu", "Adet", "Birim Fiyat", "Döviz", "Tutar"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, font(bold, 8.5f, Color.WHITE)));
                hc.setBackgroundColor(SIGNAL);
                hc.setPadding(4);
                hc.setBorderColor(LINE);
                table.addCell(hc);
            }
            int idx = 0;
            for (QuotePdfRequest.Item it : items) {
                Color bg = (idx++ % 2 == 1) ? ALT : Color.WHITE;
                boolean noPrice = it.price() == null;
                String unit = noPrice ? "Fiyat isteyin" : priceFormatter.format(it.price().multiply(factor), it.currency());
                String lineTotal = noPrice ? "—" : priceFormatter.format(it.price().multiply(factor).multiply(BigDecimal.valueOf(it.qty())), it.currency());
                addCell(table, nz(it.brand(), ""), bg, Element.ALIGN_LEFT);
                addCell(table, nz(it.name(), ""), bg, Element.ALIGN_LEFT);
                addCell(table, nz(it.code(), ""), bg, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(it.qty()), bg, Element.ALIGN_RIGHT);
                addCell(table, unit, bg, Element.ALIGN_RIGHT);
                addCell(table, nz(it.currency(), ""), bg, Element.ALIGN_CENTER);
                addCellBold(table, lineTotal, bg, Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(new Paragraph(" ", font(regular, 8, INK)));

            // ---- Özet (para birimi bazında) ----
            Map<String, BigDecimal> totals = new LinkedHashMap<>();
            for (QuotePdfRequest.Item it : items) {
                if (it.price() == null) continue;
                String cur = nz(it.currency(), "USD");
                totals.merge(cur, it.price().multiply(BigDecimal.valueOf(it.qty())), BigDecimal::add);
            }

            PdfPTable summaryWrap = new PdfPTable(1);
            summaryWrap.setWidthPercentage(45);
            summaryWrap.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (totals.isEmpty()) {
                summaryWrap.addCell(borderless(new Paragraph("Fiyatlandırılabilir ürün bulunmuyor.", font(regular, 9.5f, INK_500)), Element.ALIGN_RIGHT));
            } else {
                for (Map.Entry<String, BigDecimal> e : totals.entrySet()) {
                    String cur = e.getKey();
                    BigDecimal before = e.getValue();
                    BigDecimal after = before.multiply(factor);
                    BigDecimal vat = after.multiply(VAT);
                    BigDecimal grand = after.add(vat);
                    summaryWrap.addCell(summaryRow("Ara Toplam (" + cur + ")", priceFormatter.format(after, cur), false));
                    summaryWrap.addCell(summaryRow("KDV (%20)", "+" + priceFormatter.format(vat, cur), false));
                    summaryWrap.addCell(summaryRow("Genel Toplam (" + cur + ")", priceFormatter.format(grand, cur), true));
                }
            }
            doc.add(summaryWrap);

            // ---- Alt not ----
            doc.add(new Paragraph(" ", font(regular, 10, INK)));
            doc.add(new Paragraph(
                    "Bu bir fiyat teklifidir; ödeme veya satın alma işlemi içermez. Fiyatlar döviz bazında geçerlidir; farklı para birimleri toplanmaz.",
                    font(regular, 8, INK_500)));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF oluşturulamadı", e);
        }
    }

    private boolean addContactRow(Document doc, String label, String value) throws DocumentException {
        if (!nn(value)) return false;
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", font(bold, 10.5f, INK)));
        p.add(new Chunk(value, font(regular, 10.5f, INK)));
        doc.add(p);
        return true;
    }

    private PdfPCell summaryRow(String label, String value, boolean total) {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        try { row.setWidths(new float[]{1.4f, 1}); } catch (DocumentException ignored) {}
        Font lf = font(total ? bold : regular, total ? 11 : 9.5f, total ? SIGNAL : INK_500);
        Font vf = font(total ? bold : regular, total ? 11 : 9.5f, total ? SIGNAL : INK_500);
        row.addCell(borderless(new Paragraph(label, lf), Element.ALIGN_LEFT));
        row.addCell(borderless(new Paragraph(value, vf), Element.ALIGN_RIGHT));
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
        if (el instanceof Phrase phrase) c.addElement(phrase);
        else c.addElement((Element) el);
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

    private boolean nn(String s) { return s != null && !s.isBlank(); }
    private String nz(String s, String def) { return nn(s) ? s : def; }
}
