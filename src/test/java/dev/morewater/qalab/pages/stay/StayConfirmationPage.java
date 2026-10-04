package dev.morewater.qalab.pages.stay;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /stay/confirmation/ — comprobante de la reserva. */
public class StayConfirmationPage extends StayBase {
    public StayConfirmationPage(WebDriver driver) { super(driver); }

    public StayConfirmationPage open(String code) { go("/stay/confirmation/" + (code == null ? "" : "?code=" + code)); return this; }

    public StayConfirmationPage waitConfirmed() { visible("stay-confirmed-title"); return this; }

    public String title() { return text("stay-confirmed-title"); }
    public String code() { return text("stay-booking-code"); }
    public long total() { return cents(text("stay-conf-total")); }
    public String totalText() { return text("stay-conf-total"); }
    public String detailText() { return text("stay-confirmation"); }
    public boolean statusRole() { return !driver.findElements(By.cssSelector(".alert.ok[role='status']")).isEmpty(); }
    public boolean hasMovementsLink() { return present("stay-go-movements"); }
    public String movementsLinkText() { return text("stay-go-movements"); }
    public void goMovements() { click("stay-go-movements"); }
    public StayStaysPage goStays() { click("stay-go-stays"); return new StayStaysPage(driver).waitLoaded(); }

    // estado «no encontrada»
    public boolean notFoundLinkVisible() { return present("stay-conf-stays"); }
    public void waitNotFound() { visible("stay-conf-stays"); }
    public void clickNotFoundLink() { click("stay-conf-stays"); }
}
