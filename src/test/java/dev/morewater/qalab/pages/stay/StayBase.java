package dev.morewater.qalab.pages.stay;

import dev.morewater.qalab.pages.BasePage;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

/** Utilidades comunes de las páginas de Stay: fecha de hoy del navegador, billetera, caos y manipulación del estado local. */
public abstract class StayBase extends BasePage {
    protected StayBase(WebDriver driver) { super(driver); }

    /** «Hoy» según el navegador (la app usa su reloj local, no el de la JVM). */
    public LocalDate today() {
        String s = js("const d=new Date();const p=n=>String(n).padStart(2,'0');return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate());");
        return LocalDate.parse(s);
    }

    /** Fecha con el mismo formato que la app (prettyDate en es-MX). */
    public String pretty(LocalDate d) {
        return js("return new Date(arguments[0]+'T12:00:00').toLocaleDateString('es-MX',{weekday:'short',day:'numeric',month:'short',year:'numeric'});", d.toString());
    }

    public String path() { return js("return location.pathname + location.search;"); }

    /** Saldo de la billetera según el estado de la app. */
    public long walletCents() {
        wait.until(d -> Boolean.TRUE.equals(js("return typeof window.qalab !== 'undefined';")));
        return ((Number) js("return window.qalab.state().walletCents;")).longValue();
    }

    /** Saldo mostrado en el chip del encabezado. */
    public long walletChipCents() { return cents(text("wallet-chip")); }

    public void setWallet(long cents) { js("window.qalab.setWallet(arguments[0]);", cents); }

    public void setLatency(int ms) { js("window.qalab.setChaos({latencyMs: arguments[0]});", ms); }

    public void setSeed(int seed) { js("window.qalab.setChaos({seed: arguments[0]});", seed); }

    public int bookingsInState() { return ((Number) js("return window.qalab.state().stay.bookings.length;")).intValue(); }

    /** Retrasa el inicio de la retención (draft.heldAt) para simular el paso del tiempo sin esperarlo; después hay que recargar. */
    public void ageHold(int seconds) {
        js("const k='qalab.state.v1';const s=JSON.parse(localStorage.getItem(k));s.stay.draft.heldAt=Date.now()-arguments[0]*1000;localStorage.setItem(k,JSON.stringify(s));", seconds);
    }

    /** Retrasa el inicio de la sesión (session.startedAt); después hay que recargar. */
    public void ageSession(int seconds) {
        js("const k='qalab.state.v1';const s=JSON.parse(localStorage.getItem(k));s.session.startedAt=Date.now()-arguments[0]*1000;localStorage.setItem(k,JSON.stringify(s));", seconds);
    }

    public void logout() { click("logout"); }

    public String userChip() { return text("user-chip"); }

    public void reload() { driver.navigate().refresh(); }

    public String bodyText() { return driver.findElement(By.tagName("body")).getText(); }

    public String h1() { return wait.until(d -> d.findElement(By.tagName("h1"))).getText().trim(); }

    protected List<String> texts(String cssPrefix) {
        return driver.findElements(By.cssSelector("[data-test^='" + cssPrefix + "']")).stream().map(e -> e.getText().trim()).toList();
    }

    protected boolean enabled(String dataTest) { return visible(dataTest).isEnabled(); }

    public static boolean hasClass(WebElement e, String cls) {
        String c = e.getDomAttribute("class");
        return c != null && List.of(c.split("\\s+")).contains(cls);
    }

    /** Reintenta una aserción mientras la interfaz se actualiza (hasta 8 s). */
    public void eventually(Runnable assertion) { eventually(assertion, 8); }

    public void eventually(Runnable assertion, int seconds) {
        AtomicReference<Throwable> last = new AtomicReference<>();
        try {
            new FluentWait<>(driver).withTimeout(Duration.ofSeconds(seconds)).pollingEvery(Duration.ofMillis(150)).until(d -> {
                try { assertion.run(); return true; } catch (AssertionError e) { last.set(e); return false; }
            });
        } catch (TimeoutException e) {
            if (last.get() instanceof AssertionError ae) throw ae;
            throw e;
        }
    }

    /** Espera una condición durante un máximo de segundos y dice si llegó a cumplirse (para comprobar que algo NO ocurre o medir tiempos). */
    public boolean becomesTrue(java.util.function.BooleanSupplier cond, int seconds, int pollMs) {
        try {
            new FluentWait<>(driver).withTimeout(Duration.ofSeconds(seconds)).pollingEvery(Duration.ofMillis(pollMs)).ignoring(RuntimeException.class).until(d -> cond.getAsBoolean());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
