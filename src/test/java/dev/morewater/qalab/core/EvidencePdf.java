package dev.morewater.qalab.core;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfWriter;
import dev.morewater.qalab.config.Config;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** PDF de evidencia de un módulo (una ejecución/persona), escrito en streaming. */
final class EvidencePdf {
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font TXT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font STEP = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f);
    private static final float IMG_WIDTH = 420;

    private final Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
    private final PdfWriter writer;
    private final OutputStream out;
    private PdfOutline root;
    private int n;

    private EvidencePdf(String module, String persona, Instant now) throws Exception {
        Path file = Path.of("target", "evidence", "evidencia-" + persona + "-" + module + ".pdf");
        Files.createDirectories(file.getParent());
        out = Files.newOutputStream(file);
        writer = PdfWriter.getInstance(doc, out);
        writer.setCompressionLevel(9);
        doc.addTitle("Evidencia " + module + " · " + persona);
        doc.addCreator("qalab-selenium-starter");
        doc.open();
        root = writer.getDirectContent().getRootOutline();
        doc.add(new Paragraph("Evidencia de pruebas", TITLE));
        doc.add(new Paragraph("Módulo " + module, H2));
        Paragraph p = new Paragraph("\nPersona: " + persona + "\nFecha (UTC): " + now.toString() + "\nqalab: " + Config.baseUrl(), TXT);
        p.setLeading(16);
        doc.add(p);
    }

    static EvidencePdf open(String module, String persona, Instant now) {
        try { return new EvidencePdf(module, persona, now); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    void addTest(Evidence.TestRecord t, boolean passed, long ms) throws Exception {
        doc.newPage();
        String dest = "t" + (++n);
        Chunk title = new Chunk(t.name, H2);
        title.setLocalDestination(dest);
        doc.add(new Paragraph(title));
        doc.add(new Paragraph(t.className, TXT));
        doc.add(new Paragraph("Persona: " + Config.user(), TXT));
        Font rf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, passed ? new Color(0x15, 0x80, 0x3d) : new Color(0xb9, 0x1c, 0x1c));
        Paragraph res = new Paragraph();
        res.add(new Chunk(passed ? "APROBADA" : "FALLIDA", rf));
        res.add(new Chunk("   ·   " + String.format("%.1f s", ms / 1000.0) + "   ·   " + t.steps.size() + " pasos", TXT));
        doc.add(res);
        if (t.error != null) {
            Font ef = FontFactory.getFont(FontFactory.COURIER, 8, new Color(0xb9, 0x1c, 0x1c));
            String e = t.error.length() > 350 ? t.error.substring(0, 350) + "…" : t.error;
            doc.add(new Paragraph(e, ef));
        }
        new PdfOutline(root, PdfAction.gotoLocalPage(dest, false), (passed ? "✔ " : "✘ ") + t.name);
        int i = 0;
        for (Evidence.Step s : t.steps) {
            i++;
            Paragraph l = new Paragraph("Paso " + i + " · " + s.label(), STEP);
            l.setSpacingBefore(10);
            l.setKeepTogether(true);
            doc.add(l);
            Image img = Image.getInstance(s.jpeg());
            img.scaleToFit(IMG_WIDTH, 330);
            img.setAlignment(Element.ALIGN_LEFT);
            img.setBorder(com.lowagie.text.Rectangle.BOX);
            img.setBorderWidth(0.5f);
            img.setBorderColor(Color.LIGHT_GRAY);
            doc.add(img);
        }
        writer.flush();
    }

    void close() {
        try { if (doc.isOpen()) doc.close(); out.close(); } catch (IOException | RuntimeException ignored) { }
    }
}
