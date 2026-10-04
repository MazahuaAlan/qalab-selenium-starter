package dev.morewater.qalab.pages.bank;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/** Utilidades de captura comunes a los page objects de Bank. */
final class Fields {
    private Fields() {}

    /** Reemplaza el contenido de un input controlado por React (clear() solo a veces no dispara el cambio). */
    static void replace(WebElement el, String text) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (!text.isEmpty()) el.sendKeys(text);
    }
}
