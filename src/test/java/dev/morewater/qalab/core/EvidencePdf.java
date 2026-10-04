package dev.morewater.qalab.core;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dev.morewater.qalab.config.Config;
import java.awt.Color;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** PDF de evidencia de UN caso de prueba: portada breve y pasos con captura (2 por página). */
final class EvidencePdf {
    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font TXT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font STEP = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f);
    private static final float IMG_WIDTH = 420;

    private EvidencePdf() {}

    static Path write(Evidence.TestRecord t, boolean passed, long ms, String persona, Instant now) throws Exception {
        Path file = Path.of("target", "evidence", persona, Evidence.fileName(t.name) + ".pdf");
        Files.createDirectories(file.getParent());
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter w = PdfWriter.getInstance(doc, out);
            w.setCompressionLevel(9);
            doc.addTitle(t.name);
            doc.addCreator("qalab-selenium-starter");
            doc.open();
            doc.add(new Paragraph(t.name, H1));
            Paragraph meta = new Paragraph(t.className + "\nMódulo " + t.module + "  ·  Persona: " + persona
                    + "\nFecha (UTC): " + now + "  ·  qalab: " + Config.baseUrl(), TXT);
            meta.setLeading(13);
            doc.add(meta);
            Font rf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, passed ? new Color(0x15, 0x80, 0x3d) : new Color(0xb9, 0x1c, 0x1c));
            Paragraph res = new Paragraph();
            res.setSpacingBefore(4);
            res.add(new Chunk(passed ? "APROBADA" : "FALLIDA", rf));
            res.add(new Chunk("   ·   " + String.format("%.1f s", ms / 1000.0) + "   ·   " + t.steps.size() + " pasos", TXT));
            doc.add(res);
            if (t.error != null) {
                Font ef = FontFactory.getFont(FontFactory.COURIER, 8, new Color(0xb9, 0x1c, 0x1c));
                doc.add(new Paragraph(t.error.length() > 350 ? t.error.substring(0, 350) + "…" : t.error, ef));
            }
            int i = 0;
            for (Evidence.Step s : t.steps) {
                // Etiqueta + captura en una sola celda que no se parte entre páginas
                PdfPCell cell = new PdfPCell();
                cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                cell.setPaddingTop(10);
                Paragraph lbl = new Paragraph("Paso " + (++i) + " · " + s.label(), STEP);
                lbl.setSpacingAfter(4);
                cell.addElement(lbl);
                Image img = Image.getInstance(s.jpeg());
                img.scaleToFit(IMG_WIDTH, 330);
                img.setBorder(com.lowagie.text.Rectangle.BOX);
                img.setBorderWidth(0.5f);
                img.setBorderColor(Color.LIGHT_GRAY);
                cell.addElement(img);
                PdfPTable table = new PdfPTable(1);
                table.setWidthPercentage(100);
                table.setSplitLate(true);
                table.setSplitRows(false);
                table.addCell(cell);
                doc.add(table);
            }
            doc.close();
        }
        return file;
    }
}
