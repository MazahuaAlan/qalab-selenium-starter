package dev.morewater.qalab.core;

import dev.morewater.qalab.config.Config;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Evidencia de la prueba en curso. Las capturas viven SOLO en memoria (PNG a tamaño completo, o JPEG reducido en modo «ligera») y se incrustan en el PDF del módulo;
 * nunca se escriben a disco. API pública para las pruebas: {@link #pause()} / {@link #resume()} (ventanas de medición de tiempo).
 */
public final class Evidence {
    static final int WIDTH = 640;
    static final float QUALITY = 0.5f;

    record Step(String label, byte[] jpeg) {}

    static final class TestRecord {
        final String name, className, module;
        final long startNanos = System.nanoTime();
        final List<Step> steps = new ArrayList<>();
        String lastHash = "";
        String error;
        TestRecord(String name, String className, String module) { this.name = name; this.className = className; this.module = module; }
    }

    private static final Object LOCK = new Object();
    private static volatile TestRecord current;
    private static volatile int paused;

    private Evidence() {}

    public static boolean enabled() { return Config.evidence(); }

    static boolean active() { return enabled() && current != null && paused == 0; }

    /** Suspende la captura de pasos (p. ej. mientras una prueba mide un tiempo). Cada pause() exige su resume(). */
    public static void pause() { synchronized (LOCK) { paused++; } }

    public static void resume() { synchronized (LOCK) { if (paused > 0) paused--; } }

    /** PLAT, AIR, BANK, STAY, EATS, CARE, DESK; GEN si la clase está directamente en el paquete tests. */
    static String moduleOf(Class<?> c) {
        String p = c.getPackageName();
        int i = p.lastIndexOf('.');
        String last = i >= 0 ? p.substring(i + 1) : p;
        return switch (last) {
            case "platform" -> "PLAT";
            case "air", "bank", "stay", "eats", "care", "desk" -> last.toUpperCase();
            default -> "GEN";
        };
    }

    static void begin(String name, Class<?> testClass) {
        if (!enabled()) return;
        synchronized (LOCK) { paused = 0; current = new TestRecord(name, testClass.getName(), moduleOf(testClass)); }
    }

    /** Registra un paso con captura del driver crudo; omite la captura si es idéntica a la anterior. */
    static void step(WebDriver raw, String label) { capture(raw, label, false, false); }

    static void capture(WebDriver raw, String label, boolean force, boolean ignorePause) {
        TestRecord t = current;
        if (!enabled() || t == null || (paused > 0 && !ignorePause) || !(raw instanceof TakesScreenshot shot)) return;
        long c0 = System.nanoTime();
        try {
            byte[] png = shot.getScreenshotAs(OutputType.BYTES);
            String hash = hash(png);
            if (!force && hash.equals(t.lastHash)) return;
            t.lastHash = hash;
            byte[] jpeg = switch (Config.evidenceQuality()) { case "ligera" -> toJpeg(png); case "maxima" -> png; default -> palettePng(png); };
            synchronized (LOCK) { t.steps.add(new Step(label, jpeg)); }
            if (System.getProperty("qalab.debug") != null) System.err.println("EVD " + (System.nanoTime() - c0) / 1_000_000 + " ms " + jpeg.length + " B " + label);
        } catch (Exception | LinkageError ignored) {
            // la evidencia es un extra: nunca debe romper la prueba
        }
    }

    static void fail(WebDriver raw, Throwable error) {
        TestRecord t = current;
        if (t == null) return;
        String msg = String.valueOf(error.getMessage()).strip();
        if (msg.length() > 700) msg = msg.substring(0, 700) + "…";
        t.error = error.getClass().getSimpleName() + ": " + msg;
        capture(raw, "FALLO: " + msg.lines().findFirst().orElse(""), true, true);
    }

    /** Cierra la prueba actual y escribe su PDF: target/evidence/<persona>/<NombreVisible>.pdf */
    static void end(boolean passed) {
        TestRecord t = current;
        current = null;
        if (t == null) return;
        long ms = (System.nanoTime() - t.startNanos) / 1_000_000;
        try {
            EvidencePdf.write(t, passed, ms, Config.user(), Instant.now());
        } catch (Exception e) {
            System.err.println("Evidence: no se pudo escribir el PDF (" + e + ")");
        }
    }

    /** Nombre de archivo: el @DisplayName sin el prefijo [bug.id], saneado a [A-Za-z0-9_.-]. */
    static String fileName(String display) {
        String n = display.replaceFirst("^\\s*\\[[^\\]]*\\]\\s*", "").strip();
        n = java.text.Normalizer.normalize(n, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replaceAll("[^A-Za-z0-9_.-]+", "_").replaceAll("^_+|_+$", "");
        if (n.length() > 120) n = n.substring(0, 120);
        return n.isEmpty() ? "prueba" : n;
    }

    private static String hash(byte[] b) throws Exception {
        return Arrays.toString(MessageDigest.getInstance("SHA-256").digest(b));
    }

    static byte[] toJpeg(byte[] png) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        int w = Math.min(WIDTH, src.getWidth());
        int h = Math.max(1, Math.round(src.getHeight() * (w / (float) src.getWidth())));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src.getScaledInstance(w, h, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = writer.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(QUALITY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(dst, null, null), p);
        } finally { writer.dispose(); }
        return out.toByteArray();
    }

    /** PNG de 256 colores: la interfaz es de colores planos, así que casi no se nota y pesa ~3 veces menos que el PNG completo. */
    static byte[] palettePng(byte[] png) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        int w = src.getWidth(), h = src.getHeight();
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        java.util.Map<Integer, long[]> buckets = new java.util.HashMap<>(); // cubo de 5 bits por canal -> {n, sumR, sumG, sumB}
        for (int p : px) {
            int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            long[] c = buckets.computeIfAbsent(((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3), k -> new long[4]);
            c[0]++; c[1] += r; c[2] += g; c[3] += b;
        }
        java.util.List<long[]> top = new java.util.ArrayList<>(buckets.values());
        top.sort((a, b) -> Long.compare(b[0], a[0]));
        int n = Math.min(256, top.size());
        byte[] rr = new byte[n], gg = new byte[n], bb = new byte[n];
        for (int i = 0; i < n; i++) { long[] c = top.get(i); rr[i] = (byte) (c[1] / c[0]); gg[i] = (byte) (c[2] / c[0]); bb[i] = (byte) (c[3] / c[0]); }
        java.awt.image.IndexColorModel icm = new java.awt.image.IndexColorModel(8, n, rr, gg, bb);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icm);
        java.util.Map<Integer, Integer> cache = new java.util.HashMap<>();
        java.awt.image.WritableRaster ras = dst.getRaster();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = px[y * w + x], r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            int key = (r << 16) | (g << 8) | b;
            Integer idx = cache.get(key);
            if (idx == null) {
                int best = 0, bd = Integer.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    int dr = r - (rr[i] & 255), dg = g - (gg[i] & 255), db = b - (bb[i] & 255), d = dr * dr * 3 + dg * dg * 4 + db * db * 2;
                    if (d < bd) { bd = d; best = i; if (d == 0) break; }
                }
                idx = best; cache.put(key, idx);
            }
            ras.setSample(x, y, 0, idx);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", out);
        return out.size() < png.length ? out.toByteArray() : png;
    }
}
