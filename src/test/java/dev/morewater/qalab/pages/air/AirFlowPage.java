package dev.morewater.qalab.pages.air;

import dev.morewater.qalab.pages.BasePage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Page object del flujo completo de Air (búsqueda, resultados, pasajeros, asientos, pago, viajes y check-in). */
public class AirFlowPage extends BasePage {
    public AirFlowPage(WebDriver driver) { super(driver); }

    public static String iso(int daysFromToday) { return LocalDate.now().plusDays(daysFromToday).toString(); }

    public String textOf(String dataTest) { return text(dataTest); }
    public AirFlowPage clickOn(String dataTest) { click(dataTest); return this; }
    public AirFlowPage typeInto(String dataTest, String value) { type(dataTest, value); return this; }
    public boolean selected(String dataTest) { return visible(dataTest).isSelected(); }
    public boolean exists(String dataTest) { return present(dataTest); }

    // ---------- navegación ----------
    public AirFlowPage open() { go("/air/"); visible("air-search-form"); return this; }
    public AirFlowPage openResults(String from, String to, String date, int pax) {
        go("/air/results/?from=" + from + "&to=" + to + "&date=" + date + "&pax=" + pax);
        return this;
    }
    public AirFlowPage openPath(String path) { go(path); return this; }
    public String path() { return js("return window.location.pathname"); }
    public String currentUrl() { return driver.getCurrentUrl(); }
    public AirFlowPage waitPath(String fragment) { wait.until(d -> path().contains(fragment)); return this; }

    // ---------- búsqueda ----------
    public AirFlowPage fillSearch(String from, String to, String date, int pax) {
        type("air-origin", from);
        type("air-destination", to);
        setDate("air-date", date);
        select("air-pax", String.valueOf(pax));
        return this;
    }
    public AirFlowPage clearField(String dataTest) {
        js("const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; s.call(arguments[0], '');"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", visible(dataTest));
        return this;
    }
    public AirFlowPage submitSearch() { click("air-search-submit"); return this; }
    /** Búsqueda completa hasta ver resultados (fecha hoy+7 salvo que se indique). */
    public AirFlowPage search(String from, String to, int pax) {
        fillSearch(from, to, iso(7), pax).submitSearch();
        waitResults();
        return this;
    }
    public AirFlowPage waitResults() { visible("air-flight-0"); return this; }
    public String error(String field) { return text("air-" + field + "-error"); }
    public boolean hasError(String field) { return present("air-" + field + "-error"); }
    public String fieldValue(String dataTest) { return visible(dataTest).getAttribute("value"); }

    // ---------- resultados ----------
    public String resultsTitle() { return text("air-results-title"); }
    public String resultsSubtitle() { return visible("air-results-title").findElement(By.xpath("following-sibling::span")).getText(); }
    public int flightCount() { return driver.findElements(By.cssSelector("[data-test^='air-flight-'][data-flight-id]")).size(); }
    public String flightText(int i) { return visible("air-flight-" + i).getText(); }
    public List<String> flightIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement e : driver.findElements(By.cssSelector("[data-test^='air-flight-'][data-flight-id]"))) ids.add(e.getAttribute("data-flight-id"));
        return ids;
    }
    public String priceText(int i) { return text("air-price-" + i); }
    public List<Long> prices() {
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < flightCount(); i++) out.add(cents(priceText(i)));
        return out;
    }
    /** Hora de salida de cada vuelo en minutos desde medianoche (lee "HH:MM → HH:MM"). */
    public List<Integer> departMinutes() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < flightCount(); i++) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{2}):(\\d{2})\\s*→").matcher(flightText(i));
            if (!m.find()) throw new AssertionError("Sin hora de salida en: " + flightText(i));
            out.add(Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2)));
        }
        return out;
    }
    public AirFlowPage sortBy(String value) { select("air-sort", value); return this; }
    public AirFlowPage directOnly(boolean on) {
        WebElement c = visible("air-filter-direct");
        if (c.isSelected() != on) c.click();
        return this;
    }
    public AirFlowPage selectFlight(int i) { click("air-select-" + i); return this; }
    public AirFlowPage selectFirstFlight() { return selectFlight(0).waitPath("/air/passengers"); }

    // ---------- pasajeros ----------
    public AirFlowPage fillPassenger(int i, String first, String last, String birth, String doc) {
        type("air-first-" + i, first);
        type("air-last-" + i, last);
        setDate("air-birth-" + i, birth);
        type("air-doc-" + i, doc);
        return this;
    }
    public AirFlowPage fillContact(String email, String phone) {
        type("air-email", email);
        type("air-phone", phone);
        return this;
    }
    public AirFlowPage setBags(int i, int bags) { select("air-bag-" + i, String.valueOf(bags)); return this; }
    public AirFlowPage submitPassengers() { click("air-passengers-next"); return this; }
    public AirFlowPage fillAllValid(int pax) {
        for (int i = 0; i < pax; i++) fillPassenger(i, "Ana" + i, "Pérez", "1990-05-20", "ABCD12345" + i);
        return fillContact("ana@example.com", "5512345678");
    }
    public String flightSummary() { return text("air-flight-summary"); }

    // ---------- asientos ----------
    public AirFlowPage waitSeatMap() { visible("air-seat-header"); return this; }
    public List<String> headerLetters() {
        List<String> out = new ArrayList<>();
        for (WebElement s : visible("air-seat-header").findElements(By.tagName("span"))) {
            String tx = s.getAttribute("textContent").trim();
            if (!tx.isEmpty()) out.add(tx);
        }
        return out;
    }
    public static boolean legroom(String seat) {
        int row = Integer.parseInt(seat.substring(0, seat.length() - 1));
        return row <= 3 || row == 12 || row == 13;
    }
    /** Asientos libres (no deshabilitados) del mapa, filtrando los de más espacio según se pida. */
    public List<String> freeSeats(boolean wantLegroom) {
        List<String> out = new ArrayList<>();
        for (WebElement e : driver.findElements(By.cssSelector("[data-test^='seat-']:not([disabled])"))) {
            String id = e.getAttribute("data-test").substring(5);
            if (legroom(id) == wantLegroom) out.add(id);
        }
        return out;
    }
    public String seatLabel(String id) { return visible("seat-" + id).getAttribute("aria-label"); }
    public long firstFlightPrice() { waitResults(); return cents(text("air-price-0")); }
    public int seatButtons() { return driver.findElements(By.cssSelector("[data-test^='seat-']")).size(); }
    public AirFlowPage chooseSeat(String id) { click("seat-" + id); return this; }
    public AirFlowPage chooseStandardSeats(int n) {
        waitSeatMap();
        List<String> free = freeSeats(false);
        for (int i = 0; i < n; i++) chooseSeat(free.get(i));
        return this;
    }
    public AirFlowPage nextToPayment() { click("air-seats-next"); visible("air-summary"); return this; }
    public String seatCountText() { return text("air-seat-count"); }

    // ---------- pago ----------
    public long sum(String key) { return cents(text("air-sum-" + key + "-value")); }
    public long sumOpt(String key) { return present("air-sum-" + key) ? sum(key) : 0; }
    public long total() { return cents(text("air-sum-total")); }
    public String sumFareText() { return text("air-sum-fare-value"); }
    public AirFlowPage pay() { click("air-pay"); return this; }
    /** Paga y espera la confirmación; devuelve el código de reserva. */
    public String payAndConfirm() {
        pay().waitPath("/air/confirmation");
        return text("air-booking-code");
    }
    public String walletChip() { return text("wallet-chip"); }

    /** Atajo: del inicio al resumen de pago con N pasajeros y asientos estándar (sin sobreprecio). */
    public AirFlowPage toPayment(int pax) {
        open().search("MEX", "MTY", pax);
        selectFirstFlight().fillAllValid(pax).submitPassengers();
        waitPath("/air/seats");
        chooseStandardSeats(pax).nextToPayment();
        return this;
    }

    // ---------- estado de la aplicación (window.qalab) ----------
    public long walletCents() { return ((Number) js("return window.qalab.state().walletCents")).longValue(); }
    public AirFlowPage setWallet(long cents) { js("window.qalab.setWallet(arguments[0])", cents); return this; }
    public Map<String, Object> draft() { return js("return window.qalab.state().air.draft"); }
    public Map<String, Object> state() { return js("return window.qalab.state()"); }
    public AirFlowPage reload() { driver.navigate().refresh(); return this; }
    public AirFlowPage setChaosSeed(long seed) { js("window.qalab.setChaos({seed: arguments[0]})", seed); return this; }

    // ---------- utilidades para pruebas de personas ----------
    /** Adelanta el reloj del navegador (Date.now) para simular el paso del tiempo sin esperar. */
    public AirFlowPage shiftClock(long ms) {
        js("const o = Date.now.bind(Date); Date.now = () => o() + arguments[0];", ms);
        return this;
    }
    /** ¿Aparece la pantalla de login (sesión caducada) en los próximos segundos? */
    public boolean redirectedToLoginWithin(int seconds) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(seconds)).until(d -> path().startsWith("/id"));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }
    public boolean userChipVisible() { return present("user-chip"); }
    public AirFlowPage resizeTo(int w, int h) {
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(w, h));
        return this;
    }
    public long viewportWidth() { return ((Number) js("return window.innerWidth")).longValue(); }
    /** ¿Se intersectan las cajas de dos elementos? */
    public boolean boxesIntersect(String dtA, String dtB) {
        Boolean r = js("const a = arguments[0].getBoundingClientRect(), b = arguments[1].getBoundingClientRect();"
                + "return !(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top);", visible(dtA), visible(dtB));
        return Boolean.TRUE.equals(r);
    }
    /** Espera resultados o error de resultados; devuelve true si hubo error. */
    public boolean resultsFailed() {
        wait.until(d -> present("air-flight-0") || present("air-results-error"));
        return present("air-results-error");
    }
    /** Espera confirmación o error de pago; devuelve true si el pago falló. */
    public boolean paymentFailed() {
        wait.until(d -> path().contains("/air/confirmation") || present("air-payment-error"));
        return !path().contains("/air/confirmation");
    }
    public AirFlowPage waitPayEnabled() {
        wait.until(d -> visible("air-pay").isEnabled());
        return this;
    }
}
