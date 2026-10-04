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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Evidencia de la prueba en curso. Las capturas viven SOLO en memoria (reducidas a JPEG) y se incrustan en el PDF del módulo;
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
    private static final Map<String, EvidencePdf> PDFS = new LinkedHashMap<>();
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
        try {
            byte[] png = shot.getScreenshotAs(OutputType.BYTES);
            String hash = hash(png);
            if (!force && hash.equals(t.lastHash)) return;
            t.lastHash = hash;
            byte[] jpeg = toJpeg(png);
            synchronized (LOCK) { t.steps.add(new Step(label, jpeg)); }
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

    /** Cierra el bloque de la prueba actual y lo escribe al PDF de su módulo. */
    static void end(boolean passed) {
        TestRecord t = current;
        current = null;
        if (t == null) return;
        long ms = (System.nanoTime() - t.startNanos) / 1_000_000;
        synchronized (LOCK) {
            try {
                PDFS.computeIfAbsent(t.module, m -> EvidencePdf.open(m, Config.user(), Instant.now())).addTest(t, passed, ms);
            } catch (Exception e) {
                System.err.println("Evidence: no se pudo escribir el PDF (" + e + ")");
            }
        }
    }

    static void closeAll() {
        synchronized (LOCK) {
            PDFS.values().forEach(EvidencePdf::close);
            PDFS.clear();
        }
    }

    static { Runtime.getRuntime().addShutdownHook(new Thread(Evidence::closeAll)); }

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
}
