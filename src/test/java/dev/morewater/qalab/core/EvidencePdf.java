package dev.morewater.qalab.core;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import dev.morewater.qalab.config.Config;
import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF de evidencia de UN caso de prueba, con la identidad gráfica de MoreWater (la de Academy):
 * navy #0D1B2A, cyan #00B4D8, naranja #FF8C00, Montserrat y JetBrains Mono. Ficha con el resultado y un bloque por paso con su captura.
 */
final class EvidencePdf {
    static final Color NAVY = new Color(0x0D, 0x1B, 0x2A), CYAN = new Color(0x00, 0xB4, 0xD8), ORANGE = new Color(0xFF, 0x8C, 0x00);
    static final Color GREEN = new Color(0x28, 0xA7, 0x45), RED = new Color(0xE6, 0x39, 0x46), BG = new Color(0xF2, 0xF5, 0xF9);
    static final Color LINE = new Color(0xE4, 0xE9, 0xF0), LINE2 = new Color(0xCD, 0xD6, 0xE1), MUTED = new Color(0x5A, 0x6B, 0x7A), CYAN_TXT = new Color(0x08, 0x6D, 0x8F);
    private static final Pattern NAME = Pattern.compile("^(?:\\[([a-z]+\\.[a-z0-9_]+)]\\s*)?(A_([A-Z]+)_(\\d+))_(.*)$");
    private static final float IMG_W = 430, IMG_H = 300;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static BaseFont mont5, mont6, mont7, mono6;
    private static byte[] logo;

    private EvidencePdf() {}

    private static BaseFont font(String file) throws Exception {
        try (InputStream in = EvidencePdf.class.getResourceAsStream("/fonts/" + file)) {
            return BaseFont.createFont(file, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        }
    }

    private static synchronized void load() throws Exception {
        if (mont5 != null) return;
        mont5 = font("montserrat-500.ttf"); mont6 = font("montserrat-600.ttf"); mont7 = font("montserrat-700.ttf"); mono6 = font("jetbrains-mono-600.ttf");
        try (InputStream in = EvidencePdf.class.getResourceAsStream("/brand/logo-mw.png")) { logo = in == null ? null : in.readAllBytes(); }
    }

    private static Font f(BaseFont b, float size, Color c) { return new Font(b, size, Font.NORMAL, c); }

    /** Encabezado navy con logo y franja cyan/naranja; pie con numeración «Página N de M». */
    private static final class Frame extends PdfPageEventHelper {
        private final String id;
        private final Image logoImg; // una sola instancia: así el logo se incrusta una vez y no en cada página
        private PdfTemplate total;
        Frame(String id, Image logoImg) { this.id = id; this.logoImg = logoImg; }

        @Override public void onOpenDocument(PdfWriter w, Document d) { total = w.getDirectContent().createTemplate(40, 10); }

        @Override public void onEndPage(PdfWriter w, Document d) {
            try {
                PdfContentByte cb = w.getDirectContentUnder();
                float W = d.getPageSize().getWidth(), H = d.getPageSize().getHeight();
                cb.setColorFill(NAVY); cb.rectangle(0, H - 46, W, 46); cb.fill();
                cb.setColorFill(CYAN); cb.rectangle(0, H - 49, W * 0.72f, 3); cb.fill();
                cb.setColorFill(ORANGE); cb.rectangle(W * 0.72f, H - 49, W * 0.28f, 3); cb.fill();
                if (logoImg != null) w.getDirectContent().addImage(logoImg);
                PdfContentByte c = w.getDirectContent();
                c.beginText(); c.setFontAndSize(mont6, 10.5f); c.setColorFill(Color.WHITE);
                c.setTextMatrix(92, H - 28); c.showText("Evidencia de prueba"); c.endText();
                c.beginText(); c.setFontAndSize(mono6, 9); c.setColorFill(CYAN);
                c.showTextAligned(Element.ALIGN_RIGHT, id, W - 36, H - 28, 0); c.endText();
                c.setColorStroke(LINE); c.setLineWidth(0.6f); c.moveTo(36, 34); c.lineTo(W - 36, 34); c.stroke();
                c.beginText(); c.setFontAndSize(mont5, 7.5f); c.setColorFill(MUTED);
                c.showTextAligned(Element.ALIGN_LEFT, "MoreWater · pruebas automatizadas de qalab", 36, 22, 0);
                String p = "Página " + w.getPageNumber() + " de ";
                c.showTextAligned(Element.ALIGN_RIGHT, p, W - 36 - 10, 22, 0); c.endText();
                c.addTemplate(total, W - 36 - 10, 22);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }

        @Override public void onCloseDocument(PdfWriter w, Document d) {
            total.beginText(); total.setFontAndSize(mont5, 7.5f); total.setColorFill(MUTED);
            total.setTextMatrix(0, 0); total.showText(String.valueOf(w.getPageNumber() - 1)); total.endText();
        }
    }

    private static PdfPCell card(String label, String value, Color valueColor) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(BG); c.setBorderColor(LINE); c.setBorderWidth(0.8f); c.setPadding(8);
        Paragraph l = new Paragraph(label, f(mont5, 7.5f, MUTED));
        Paragraph v = new Paragraph(value, f(mont7, 12, valueColor));
        v.setSpacingBefore(2);
        c.addElement(l); c.addElement(v);
        return c;
    }

    private static String words(String camel) { return camel.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " "); }

    static Path write(Evidence.TestRecord t, boolean passed, long ms, String persona, Instant now) throws Exception {
        load();
        Path file = Path.of("target", "evidence", persona, Evidence.fileName(t.name) + ".pdf");
        Files.createDirectories(file.getParent());
        Matcher m = NAME.matcher(t.name);
        boolean named = m.matches();
        String id = named ? m.group(2) : Evidence.fileName(t.name), bug = named ? m.group(1) : null, title = named ? words(m.group(5)) : t.name;
        String module = named ? m.group(3) : t.module;
        Document doc = new Document(PageSize.A4, 36, 36, 66, 46);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter w = PdfWriter.getInstance(doc, out);
            w.setCompressionLevel(9);
            Image logoImg = null;
            if (logo != null) { logoImg = Image.getInstance(logo); logoImg.scaleToFit(48, 26); logoImg.setAbsolutePosition(36, PageSize.A4.getHeight() - 36); }
            w.setPageEvent(new Frame(id, logoImg));
            doc.addTitle(id + " · " + title); doc.addAuthor("MoreWater"); doc.addCreator("qalab-selenium-starter");
            doc.open();
            Paragraph pid = new Paragraph(id, f(mono6, 10, CYAN_TXT));
            doc.add(pid);
            Paragraph h = new Paragraph(title, f(mont7, 18, NAVY));
            h.setLeading(22); h.setSpacingAfter(10);
            doc.add(h);
            PdfPTable cards = new PdfPTable(4);
            cards.setWidthPercentage(100); cards.setSpacingAfter(8); cards.setWidths(new float[] {22, 20, 14, 44});
            cards.addCell(card("Resultado", passed ? "Aprobada" : "Fallida", passed ? GREEN : RED));
            cards.addCell(card("Duración", String.format("%.1f s", ms / 1000.0), NAVY));
            cards.addCell(card("Pasos", String.valueOf(t.steps.size()), NAVY));
            cards.addCell(card("Fecha", WHEN.format(now), NAVY));
            doc.add(cards);
            Paragraph meta = new Paragraph();
            meta.setLeading(13);
            meta.add(new Chunk("Módulo " + module + "  ·  Usuario de prueba " + persona + "  ·  ", f(mont5, 8.5f, MUTED)));
            meta.add(new Chunk(Config.baseUrl(), f(mono6, 8, MUTED)));
            if (bug != null) { meta.add(Chunk.NEWLINE); meta.add(new Chunk("Defecto que cubre  ", f(mont5, 8.5f, MUTED))); meta.add(new Chunk(bug, f(mono6, 8.5f, new Color(0xA8, 0x52, 0x00)))); }
            meta.add(Chunk.NEWLINE); meta.add(new Chunk(t.className, f(mono6, 7.5f, MUTED)));
            doc.add(meta);
            if (t.error != null) {
                PdfPTable box = new PdfPTable(1); box.setWidthPercentage(100); box.setSpacingBefore(8);
                PdfPCell c = new PdfPCell(new Phrase(t.error.length() > 420 ? t.error.substring(0, 420) + "…" : t.error, f(mono6, 7.5f, RED)));
                c.setBackgroundColor(new Color(0xFD, 0xEC, 0xEE)); c.setBorder(Rectangle.LEFT); c.setBorderColorLeft(RED); c.setBorderWidthLeft(3); c.setPadding(8);
                box.addCell(c); doc.add(box);
            }
            int i = 0;
            for (Evidence.Step s : t.steps) doc.add(step(++i, s));
            doc.close();
        }
        return file;
    }

    private static PdfPTable step(int n, Evidence.Step s) throws Exception {
        PdfPTable head = new PdfPTable(new float[] {26, 474});
        head.setWidthPercentage(100); head.setSpacingBefore(12); head.setKeepTogether(true);
        PdfPCell num = new PdfPCell(new Phrase(String.valueOf(n), f(mont7, 9, Color.WHITE)));
        num.setBackgroundColor(NAVY); num.setBorder(Rectangle.NO_BORDER); num.setHorizontalAlignment(Element.ALIGN_CENTER); num.setVerticalAlignment(Element.ALIGN_MIDDLE); num.setPadding(5);
        PdfPCell lbl = new PdfPCell(new Phrase(s.label(), f(mont6, 8.5f, NAVY)));
        lbl.setBackgroundColor(BG); lbl.setBorder(Rectangle.NO_BORDER); lbl.setVerticalAlignment(Element.ALIGN_MIDDLE); lbl.setPadding(6); lbl.setPaddingLeft(10);
        head.addCell(num); head.addCell(lbl);
        Image img = Image.getInstance(s.jpeg());
        img.scaleToFit(IMG_W, IMG_H);
        PdfPCell pic = new PdfPCell(img, false);
        pic.setColspan(2); pic.setPadding(4); pic.setBorderColor(LINE2); pic.setBorderWidth(0.6f); pic.setHorizontalAlignment(Element.ALIGN_CENTER);
        head.addCell(pic);
        return head;
    }
}
