package dev.morewater.qalab.pages.stay;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** /stay/results/ — listado, filtros y orden. */
public class StayResultsPage extends StayBase {
    public StayResultsPage(WebDriver driver) { super(driver); }

    public StayResultsPage open(String city, String in, String out, int adults, int kids) {
        go("/stay/results/?city=" + city + "&in=" + in + "&out=" + out + "&adults=" + adults + "&kids=" + kids);
        return waitLoaded();
    }

    public StayResultsPage waitLoaded() { visible("stay-count"); return this; }

    public String title() { return text("stay-results-title"); }
    public String countText() { return text("stay-count"); }
    public int count() { return Integer.parseInt(countText().split(" ")[0]); }
    public boolean loadingVisible() { return present("stay-loading"); }
    public boolean emptyVisible() { return present("stay-empty"); }
    public String emptyText() { return text("stay-empty"); }

    /** Texto bajo el título: fechas · noches · huéspedes. */
    public String subtitle() { return driver.findElement(By.cssSelector("[data-test='stay-results-title'] ~ span")).getText().trim(); }

    public int shown() { return driver.findElements(By.cssSelector("article[data-hotel-id]")).size(); }
    public List<WebElement> cards() { return driver.findElements(By.cssSelector("article[data-hotel-id]")); }
    public List<String> names() { return texts("stay-hotel-name-"); }
    public List<String> prices() { return texts("stay-hotel-price-"); }
    public List<String> ratings() { return texts("stay-hotel-rating-"); }
    public List<String> starLabels() { return driver.findElements(By.cssSelector("[data-test^='stay-hotel-stars-']")).stream().map(e -> e.getDomAttribute("aria-label")).toList(); }
    public List<String> cardTexts() { return cards().stream().map(e -> e.getText()).toList(); }
    public String name(int i) { return text("stay-hotel-name-" + i); }
    public String price(int i) { return text("stay-hotel-price-" + i); }
    public String rating(int i) { return text("stay-hotel-rating-" + i); }
    public String stars(int i) { return visible("stay-hotel-stars-" + i).getDomAttribute("aria-label"); }

    public StayHotelPage view(int i) {
        click("stay-view-" + i);
        return new StayHotelPage(driver).waitLoaded();
    }

    public String priceValue() { return text("stay-price-value"); }

    /** El control es un &lt;input type="range"&gt;: se asigna el valor con el setter nativo y se dispara «input» (lo que escucha React). */
    public StayResultsPage setMaxPrice(int pesos) {
        js("const s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;s.call(arguments[0],arguments[1]);arguments[0].dispatchEvent(new Event('input',{bubbles:true}));",
                visible("stay-filter-price"), String.valueOf(pesos));
        return this;
    }

    public StayResultsPage toggleStars(int n) { click("stay-filter-stars-" + n); return this; }
    public StayResultsPage toggleAmenity(String id) { click("stay-filter-" + id); return this; }
    public boolean amenityChecked(String id) { return visible("stay-filter-" + id).isSelected(); }
    public StayResultsPage sortBy(String value) { new Select(visible("stay-sort")).selectByValue(value); return this; }
    public String sortValue() { return new Select(visible("stay-sort")).getFirstSelectedOption().getDomAttribute("value"); }
}
