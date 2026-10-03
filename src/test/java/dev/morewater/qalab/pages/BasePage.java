package dev.morewater.qalab.pages;

import dev.morewater.qalab.config.Config;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Utilidades comunes. Los localizadores usan el atributo data-test (consulta /docs/ o el modo Inspector de qalab). */
public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
    }

    protected static By t(String dataTest) { return By.cssSelector("[data-test='" + dataTest + "']"); }

    protected WebElement visible(String dataTest) { return wait.until(ExpectedConditions.visibilityOfElementLocated(t(dataTest))); }

    protected void click(String dataTest) { wait.until(ExpectedConditions.elementToBeClickable(t(dataTest))).click(); }

    protected void type(String dataTest, String text) {
        WebElement el = visible(dataTest);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Un &lt;input type="date"&gt; depende del idioma del navegador al teclear (mm/dd vs dd/mm). Para no depender de eso se asigna el valor
     * ISO con el setter nativo y se dispara el evento que React escucha.
     */
    protected void setDate(String dataTest, String isoDate) {
        js("const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; s.call(arguments[0], arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", visible(dataTest), isoDate);
    }

    protected void select(String dataTest, String value) { new Select(visible(dataTest)).selectByValue(value); }

    protected String text(String dataTest) { return visible(dataTest).getText().trim(); }

    protected boolean present(String dataTest) { return !driver.findElements(t(dataTest)).isEmpty(); }

    /** Dinero en pesos → centavos ("$1,234.50" → 123450). */
    public static long cents(String money) { return Math.round(Double.parseDouble(money.replaceAll("[^0-9.-]", "")) * 100); }

    /** Espera a que termine la última petición simulada (window.qalab.calls()). */
    protected void waitForCallsAfter(long before) {
        wait.until(d -> ((Number) ((JavascriptExecutor) d).executeScript("return window.qalab.calls()")).longValue() > before);
    }

    protected long calls() { return ((Number) ((JavascriptExecutor) driver).executeScript("return window.qalab.calls()")).longValue(); }

    public void go(String path) { driver.get(Config.baseUrl() + path); }

    @SuppressWarnings("unchecked")
    protected <T> T js(String script, Object... args) { return (T) ((JavascriptExecutor) driver).executeScript(script, args); }
}
