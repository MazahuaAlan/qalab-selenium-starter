package dev.morewater.qalab.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Flujo completo de Air: búsqueda → resultados → pasajeros → asientos → pago. */
public class AirPage extends BasePage {
    public AirPage(WebDriver driver) { super(driver); }

    public AirPage open() { go("/air/"); return this; }

    public AirPage search(String from, String to, int pax) {
        type("air-origin", from);
        type("air-destination", to);
        select("air-pax", String.valueOf(pax));
        click("air-search-submit");
        return this;
    }

    public String searchError(String field) { return text("air-" + field + "-error"); }

    /** Espera los resultados y devuelve el precio por pasajero (centavos) del primer vuelo. */
    public long firstFlightPrice() {
        visible("air-flight-0");
        return cents(text("air-price-0"));
    }

    public String firstFlightPriceText() { visible("air-flight-0"); return text("air-price-0"); }

    public AirPage selectFirstFlight() { click("air-select-0"); return this; }

    public AirPage fillPassengers(int pax) {
        for (int i = 0; i < pax; i++) {
            type("air-first-" + i, "Ana" + i);
            type("air-last-" + i, "Pérez");
            setDate("air-birth-" + i, "1990-05-20");
            type("air-doc-" + i, "ABCD12345" + i);
        }
        type("air-email", "ana@example.com");
        type("air-phone", "5512345678");
        click("air-passengers-next");
        return this;
    }

    public AirPage pickFreeSeats(int n) {
        wait.until(d -> !d.findElements(By.cssSelector("[data-test^='seat-']:not([disabled])")).isEmpty());
        List<WebElement> free = driver.findElements(By.cssSelector("[data-test^='seat-']:not([disabled])"));
        for (int i = 0; i < n; i++) free.get(i).click();
        click("air-seats-next");
        return this;
    }

    public long summaryCents(String key) { return cents(text("air-sum-" + key + "-value")); }

    /** Líneas opcionales del resumen (equipaje, asientos con más espacio): 0 si no aparecen. */
    public long optionalCents(String key) { return present("air-sum-" + key) ? summaryCents(key) : 0; }

    public long totalCents() { return cents(text("air-sum-total")); }

    public String summaryFareText() { return text("air-sum-fare-value"); }

    /** Atajo: llega al resumen de pago con N pasajeros. */
    public AirPage toPayment(int pax) {
        open().search("MEX", "MTY", pax);
        selectFirstFlight().fillPassengers(pax).pickFreeSeats(pax);
        visible("air-summary");
        return this;
    }
}
