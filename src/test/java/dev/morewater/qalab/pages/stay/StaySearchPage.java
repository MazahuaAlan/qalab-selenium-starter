package dev.morewater.qalab.pages.stay;

import java.time.LocalDate;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** /stay/ — buscador de hoteles. */
public class StaySearchPage extends StayBase {
    public final Calendar calendar;

    public StaySearchPage(WebDriver driver) { super(driver); this.calendar = new Calendar(driver); }

    public StaySearchPage open() {
        go("/stay/");
        visible("stay-search-form");
        return this;
    }

    public List<String> cityOptions() { return new Select(visible("stay-city")).getOptions().stream().map(o -> o.getText().trim()).toList(); }
    public String selectedCity() { return new Select(visible("stay-city")).getFirstSelectedOption().getText().trim(); }
    public StaySearchPage chooseCity(String code) { select("stay-city", code); return this; }

    public int adults() { return Integer.parseInt(text("stay-adults-value")); }
    public int kids() { return Integer.parseInt(text("stay-kids-value")); }
    public StaySearchPage adultsPlus() { int n = adults(); click("stay-adults-plus"); wait.until(d -> adults() == n + 1); return this; }
    public StaySearchPage adultsMinus() { int n = adults(); click("stay-adults-minus"); wait.until(d -> adults() == n - 1); return this; }
    public StaySearchPage kidsPlus() { int n = kids(); click("stay-kids-plus"); wait.until(d -> kids() == n + 1); return this; }
    public StaySearchPage kidsMinus() { int n = kids(); click("stay-kids-minus"); wait.until(d -> kids() == n - 1); return this; }
    public boolean adultsPlusEnabled() { return enabled("stay-adults-plus"); }
    public boolean adultsMinusEnabled() { return enabled("stay-adults-minus"); }
    public boolean kidsPlusEnabled() { return enabled("stay-kids-plus"); }
    public boolean kidsMinusEnabled() { return enabled("stay-kids-minus"); }

    public StaySearchPage pickRange(LocalDate from, LocalDate to) { calendar.selectRange(from, to); return this; }

    public String summary() { return text("stay-range-summary"); }
    public String nights() { return text("stay-nights"); }
    public boolean hasNights() { return present("stay-nights"); }

    public StaySearchPage submit() { click("stay-search-submit"); return this; }
    public String error() { return text("stay-search-error"); }
    public boolean hasError() { return present("stay-search-error"); }

    /** Pulsa Buscar y espera a que carguen los resultados. */
    public StayResultsPage searchAndWait() {
        submit();
        StayResultsPage r = new StayResultsPage(driver);
        r.waitLoaded();
        return r;
    }

    public WebElement dayButton(LocalDate d) { return driver.findElement(By.cssSelector("[data-test='cal-day-" + d + "']")); }
}
