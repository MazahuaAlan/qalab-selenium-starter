package dev.morewater.qalab.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;

/** Convierte cada acción de Selenium (abrir, clic, escribir, limpiar) en un paso con captura. */
public final class EvidenceListener implements WebDriverListener {
    private final WebDriver raw;
    private String pendingClick = "";

    EvidenceListener(WebDriver raw) { this.raw = raw; }

    @Override public void afterGet(WebDriver driver, String url) { Evidence.step(raw, "Abrir " + url); }

    @Override public void afterTo(WebDriver.Navigation nav, String url) { Evidence.step(raw, "Abrir " + url); }

    @Override public void beforeClick(WebElement el) {
        if (!Evidence.active()) return;
        pendingClick = describe(el, true);
    }

    @Override public void afterClick(WebElement el) {
        String label = pendingClick.isEmpty() ? describe(el, false) : pendingClick;
        pendingClick = "";
        Evidence.step(raw, "Clic en " + label);
    }

    @Override public void afterSendKeys(WebElement el, CharSequence... keys) {
        if (!Evidence.active()) return;
        String sel = selector(el);
        String text = String.join("", java.util.Arrays.stream(keys).map(CharSequence::toString).toList());
        boolean secret = sel.toLowerCase().contains("pass") || sel.toLowerCase().contains("contra");
        if (secret) text = "••••••";
        Evidence.step(raw, "Escribir \"" + shorten(text, 60) + "\" en " + sel);
    }

    @Override public void afterClear(WebElement el) {
        if (!Evidence.active()) return;
        Evidence.step(raw, "Limpiar " + selector(el));
    }

    private String describe(WebElement el, boolean withText) {
        if (!Evidence.active()) return "";
        String sel = selector(el);
        if (!withText) return sel;
        try {
            String t = el.getText();
            if (t != null && !t.isBlank()) return sel + " \"" + shorten(t.replaceAll("\\s+", " ").trim(), 40) + "\"";
        } catch (RuntimeException ignored) { }
        return sel;
    }

    /** RemoteWebElement.toString() termina en «-> css selector: X]»; se extrae solo el localizador. */
    static String selector(WebElement el) {
        try {
            String s = el.toString();
            int i = s.lastIndexOf("-> ");
            if (i >= 0) {
                String r = s.substring(i + 3);
                int c = r.indexOf(": ");
                if (c >= 0) r = r.substring(c + 2);
                while (r.endsWith("]") && count(r, '[') < count(r, ']')) r = r.substring(0, r.length() - 1);
                return r;
            }
        } catch (RuntimeException ignored) { }
        return "elemento";
    }

    private static long count(String s, char c) { return s.chars().filter(x -> x == c).count(); }

    private static String shorten(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
}
