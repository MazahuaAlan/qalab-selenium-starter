package dev.morewater.qalab.pages.stay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** /stay/book/ — datos del huésped, resumen de precio y pago. */
public class StayBookPage extends StayBase {
    public StayBookPage(WebDriver driver) { super(driver); }

    public StayBookPage open() { go("/stay/book/"); return this; }

    public StayBookPage waitForm() { visible("stay-book-form"); return this; }
    public boolean noDraft() { return present("stay-no-draft"); }
    public void waitNoDraft() { visible("stay-no-draft"); }
    public boolean formVisible() { return present("stay-book-form"); }

    // ---- temporizador ----
    public String timer() { return text("stay-timer"); }
    public int timerSeconds() { String[] p = timer().split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
    public String holdText() { return text("stay-hold"); }

    // ---- formulario ----
    public StayBookPage name(String v) { type("stay-guest-name", v); return this; }
    public StayBookPage email(String v) { type("stay-guest-email", v); return this; }
    public StayBookPage phone(String v) { type("stay-guest-phone", v); return this; }
    public StayBookPage arrival(String v) { new Select(visible("stay-guest-arrival")).selectByVisibleText(v); return this; }
    public StayBookPage notes(String v) { type("stay-guest-notes", v); return this; }
    public StayBookPage terms(boolean on) { WebElement c = visible("stay-terms"); if (c.isSelected() != on) c.click(); return this; }

    public String nameValue() { return visible("stay-guest-name").getDomProperty("value"); }
    public String emailValue() { return visible("stay-guest-email").getDomProperty("value"); }
    public String phoneValue() { return visible("stay-guest-phone").getDomProperty("value"); }
    public String notesValue() { return visible("stay-guest-notes").getDomProperty("value"); }
    public String arrivalValue() { return new Select(visible("stay-guest-arrival")).getFirstSelectedOption().getText().trim(); }
    public boolean termsChecked() { return visible("stay-terms").isSelected(); }
    public String notesCount() { return text("stay-notes-count"); }

    /** Quita el último carácter de las peticiones especiales. */
    public StayBookPage notesBackspace() { visible("stay-guest-notes").sendKeys(Keys.BACK_SPACE); return this; }

    /** Datos válidos de la guía de casos. */
    public StayBookPage fillValid() { return name("Ana Pérez López").email("ana@example.com").phone("5512345678").terms(true); }

    public String nameError() { return errorOf("stay-guest-name-error"); }
    public String emailError() { return errorOf("stay-guest-email-error"); }
    public String phoneError() { return errorOf("stay-guest-phone-error"); }
    public String notesError() { return errorOf("stay-guest-notes-error"); }
    public String termsError() { return errorOf("stay-terms-error"); }
    private String errorOf(String id) { return present(id) ? text(id) : ""; }

    public StayBookPage clickConfirm() { click("stay-confirm"); return this; }
    public String confirmText() { return text("stay-confirm"); }
    public boolean confirmEnabled() { return enabled("stay-confirm"); }
    public boolean payErrorVisible() { return present("stay-pay-error"); }
    public String payError() { return text("stay-pay-error"); }

    /** Pulsa «Pagar» y espera a que termine la llamada: devuelve la confirmación o null si falló. */
    public StayConfirmationPage confirmAndWait() {
        long before = calls();
        clickConfirm();
        waitForCallsAfter(before);
        wait.until(d -> present("stay-confirmed-title") || present("stay-pay-error"));
        return present("stay-confirmed-title") ? new StayConfirmationPage(driver) : null;
    }

    /** Pulsa «Pagar» y espera a la pantalla de confirmación (debe confirmarse). */
    public StayConfirmationPage confirmOk() {
        clickConfirm();
        return new StayConfirmationPage(driver).waitConfirmed();
    }

    // ---- resumen ----
    public String summaryText() { return text("stay-summary"); }
    public String sumHotel() { return text("stay-sum-hotel"); }
    public String sumNightsLabel() { return text("stay-sum-nights-label"); }
    public String sumBaseLabel() { return driver.findElement(By.cssSelector("[data-test='stay-sum-base'] > span")).getText().trim(); }
    public String sumIshLabel() { return driver.findElement(By.cssSelector("[data-test='stay-sum-ish'] > span")).getText().trim(); }
    public String sumIvaLabel() { return driver.findElement(By.cssSelector("[data-test='stay-sum-iva'] > span")).getText().trim(); }
    public long sumBase() { return cents(text("stay-sum-base-value")); }
    public long sumIva() { return cents(text("stay-sum-iva-value")); }
    public long sumIsh() { return cents(text("stay-sum-ish-value")); }
    public long sumTotal() { return cents(text("stay-sum-total")); }
    public String sumTotalText() { return text("stay-sum-total"); }

    /** Número de noches indicado en la etiqueta «N noche(s)». */
    public int sumNights() { Matcher m = Pattern.compile("\\d+").matcher(sumNightsLabel()); m.find(); return Integer.parseInt(m.group()); }
}
