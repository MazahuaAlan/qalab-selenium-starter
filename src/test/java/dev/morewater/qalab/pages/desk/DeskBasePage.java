package dev.morewater.qalab.pages.desk;

import dev.morewater.qalab.pages.BasePage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Utilidades comunes de Desk: rol, notificaciones, insignia de prioridad (Shadow DOM), fecha y reloj del navegador. */
public abstract class DeskBasePage extends BasePage {
    protected static final By BADGE = By.cssSelector("[data-test='desk-priority-badge']");

    protected DeskBasePage(WebDriver driver) { super(driver); }

    /** Espera a que React haya hidratado y el estado real de localStorage esté cargado (window.qalab existe). */
    public void waitHydrated() { wait.until(d -> Boolean.TRUE.equals(js("return !!window.qalab"))); }

    /** Los option de Desk no tienen atributo value (el valor es el texto), así que se elige por texto visible. */
    protected void selectText(String dataTest, String text) { new org.openqa.selenium.support.ui.Select(visible(dataTest)).selectByVisibleText(text); }

    public void setRole(String role) { select("desk-role", role); }

    public int bellCount() { return Integer.parseInt(text("desk-bell-count")); }

    public void openBell() { click("desk-bell"); visible("desk-notifs"); }

    public String notification(int i) { return text("desk-notif-" + i); }

    /** Insignia de prioridad que vive dentro del shadowRoot abierto del elemento qa-priority situado bajo el ámbito dado. */
    public WebElement badgeIn(WebElement scope) {
        WebElement host = scope.findElement(By.cssSelector("qa-priority"));
        return host.getShadowRoot().findElement(BADGE);
    }

    /** Texto de la insignia de prioridad. */
    public String badgeText(WebElement scope) {
        return wait.ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(d -> badgeIn(scope).getText().trim());
    }

    /** Color de fondo de la insignia normalizado a "rgb(r, g, b)". */
    public String badgeColor(WebElement scope) {
        return wait.ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(d -> rgb(badgeIn(scope).getCssValue("background-color")));
    }

    /** ¿Se ve la insignia usando el DOM normal (sin atravesar el shadowRoot)? */
    public boolean badgeVisibleWithoutShadow(WebElement scope) { return !scope.findElements(BADGE).isEmpty(); }

    public static String rgb(String css) {
        Matcher m = Pattern.compile("(\\d+)\\D+(\\d+)\\D+(\\d+)").matcher(css);
        if (!m.find()) throw new IllegalArgumentException("Color no reconocido: " + css);
        return "rgb(" + m.group(1) + ", " + m.group(2) + ", " + m.group(3) + ")";
    }

    /** Fecha de hoy según el navegador (AAAA-MM-DD, hora local), la misma que usa la aplicación. */
    public String today() {
        return js("const d=new Date(),p=n=>String(n).padStart(2,'0');return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate())");
    }

    public String todayPlus(int days) { return java.time.LocalDate.parse(today()).plusDays(days).toString(); }

    /** Cantidad de tickets guardados según window.qalab.state(). */
    public long ticketsInState() { return ((Number) js("return window.qalab.state().desk.tickets.length")).longValue(); }

    /** Contador de ids de tickets (el siguiente ticket será contador + 1). */
    public long ticketCounter() { return ((Number) js("return window.qalab.state().desk.counter")).longValue(); }

    public long ticketsWithTitle(String title) {
        return ((Number) js("return window.qalab.state().desk.tickets.filter(t => t.title === arguments[0]).length", title)).longValue();
    }

    /** Provoca que toda llamada simulada falle (caos) para disparar los errores de red de forma determinista. */
    public void setChaosFailPct(int pct) { js("window.qalab.setChaos({failPct: arguments[0]})", pct); }

    /** Adelanta el reloj de Date.now() del navegador para no esperar tiempo real en las pruebas de caducidad. */
    public void advanceClock(long ms) {
        js("if (window.__qaShift === undefined) { window.__qaShift = 0; const o = Date.now.bind(Date); Date.now = () => o() + window.__qaShift; }"
                + "window.__qaShift += arguments[0];", ms);
    }

    public String currentToast() {
        var els = driver.findElements(t("desk-toast"));
        return els.isEmpty() ? "" : els.get(0).getText().trim();
    }
}
