package dev.morewater.qalab.pages.platform;

import dev.morewater.qalab.pages.BasePage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Elementos comunes a toda la plataforma: cabecera (Shell), pie, panel de caos y la API window.qalab. */
public class PlatformPage extends BasePage {
    public PlatformPage(WebDriver driver) { super(driver); }

    public PlatformPage open(String path) {
        go(path);
        waitReady();
        return this;
    }

    /** Espera a que Platform haya montado window.qalab (tras la hidratación). */
    public void waitReady() {
        wait.until(d -> Boolean.TRUE.equals(js("return typeof window.qalab !== 'undefined' && !!window.qalab.state")));
    }

    public String path() { return js("return location.pathname + location.search"); }
    public String url() { return driver.getCurrentUrl(); }

    // ---- window.qalab ----
    @SuppressWarnings("unchecked")
    public Map<String, Object> state() { return js("return window.qalab.state()"); }
    @SuppressWarnings("unchecked")
    public Map<String, Object> session() { return (Map<String, Object>) state().get("session"); }
    @SuppressWarnings("unchecked")
    public Map<String, Object> chaos() { return (Map<String, Object>) state().get("chaos"); }
    public long walletCents() { return ((Number) state().get("walletCents")).longValue(); }
    public void setWallet(long cents) { js("window.qalab.setWallet(arguments[0])", cents); }
    public void reset() { js("window.qalab.reset()"); }
    public long callCount() { return calls(); }

    /** 123456 → "$1,234.56" (mismo formato que la UI). */
    public static String mxn(long cents) { return String.format(Locale.US, "$%,.2f", cents / 100.0); }

    // ---- cabecera ----
    public boolean hasUserChip() { return present("user-chip"); }
    public boolean hasLoginLink() { return present("login-link"); }
    public boolean hasWalletChip() { return present("wallet-chip"); }
    public String userChip() { return text("user-chip"); }
    public String walletChipText() { return text("wallet-chip").replaceAll("\\s+", " "); }
    public WebElement walletChip() { return visible("wallet-chip"); }
    public void logout() { click("logout"); }
    public void clickFooter(String which) { click("footer-" + which); }

    // ---- panel de caos ----
    public String chaosToggleText() { return text("chaos-toggle"); }
    public String chaosToggleExpanded() { return visible("chaos-toggle").getDomAttribute("aria-expanded"); }
    public void toggleChaos() { click("chaos-toggle"); }
    public boolean chaosPanelOpen() { return present("chaos-panel"); }
    public WebElement chaosPanel() { return visible("chaos-panel"); }
    public void closeChaosPanel() {
        chaosPanel().findElement(By.xpath(".//button[normalize-space()='Cerrar']")).click();
    }
    public void openChaosPanelIfClosed() { if (!chaosPanelOpen()) toggleChaos(); visible("chaos-panel"); }

    /** Fija un input range con el setter nativo y dispara el evento que React escucha. */
    public void setRange(String dataTest, int value) {
        js("const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; s.call(arguments[0], String(arguments[1]));"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", visible(dataTest), value);
    }
    public void setSeed(int seed) { type("chaos-seed", String.valueOf(seed)); }
    public List<WebElement> chaosLogLines() { return visible("chaos-log").findElements(By.cssSelector("div:not(.hint)")); }
    public WebElement chaosLog() { return visible("chaos-log"); }
    public String chaosOutputFor(String inputDataTest) {
        return visible(inputDataTest).findElement(By.xpath("preceding-sibling::label[1]/output")).getText().trim();
    }

    // ---- accesos públicos para las pruebas ----
    public void until(java.util.function.BooleanSupplier cond) { wait.until(d -> cond.getAsBoolean()); }
    public void until(java.util.function.BooleanSupplier cond, int seconds) {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(seconds)).until(d -> cond.getAsBoolean());
    }
    public <T> T eval(String script, Object... args) { return js(script, args); }
}
