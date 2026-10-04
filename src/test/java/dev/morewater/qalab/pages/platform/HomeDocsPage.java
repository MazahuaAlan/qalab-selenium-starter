package dev.morewater.qalab.pages.platform;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Portada (/), guía (/docs/), tabla de defectos (/docs/bugs/) y el contrato /bugs.json. */
public class HomeDocsPage extends PlatformPage {
    public HomeDocsPage(WebDriver driver) { super(driver); }

    public HomeDocsPage openHome() { open("/"); visible("home-login"); return this; }
    public String heroTitle() { return visible("home-login").findElement(By.xpath("ancestor::div[contains(@class,'hero')]//h1")).getText().trim(); }
    public String homeLoginText() { return text("home-login"); }
    public String homeDocsText() { return text("home-docs"); }
    public boolean metroMapVisible() { return visible("metro-map").isDisplayed(); }

    public List<WebElement> appTiles() { return visible("app-list").findElements(By.cssSelector("[data-test^='app-']")); }
    public List<String> appIds() { return appTiles().stream().map(e -> e.getDomAttribute("data-test")).collect(Collectors.toList()); }
    public String hrefOf(String dataTest) { return visible(dataTest).getDomAttribute("href"); }
    public void clickApp(String id) { click("app-" + id); }

    public HomeDocsPage openBugsTable() { open("/docs/bugs/"); visible("bugs-table"); return this; }
    public List<WebElement> bugRows() { return driver.findElements(t("bug-row")); }
    public String bugsCountText() {
        return driver.findElement(By.xpath("//main//*[contains(normalize-space(.), 'entradas')][not(*[contains(normalize-space(.), 'entradas')])]")).getText();
    }
    /** Celdas de una fila: id, persona, tipo, título, severidad, nivel... */
    public List<String> cells(WebElement row) {
        return row.findElements(By.tagName("td")).stream().map(e -> e.getText().trim()).collect(Collectors.toList());
    }

    /** GET de una ruta del sitio desde el navegador; devuelve {status, body, json} con json ya analizado (o null). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchJson(String path) {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + "fetch(arguments[0]).then(async r => { const body = await r.text(); let json = null; try { json = JSON.parse(body); } catch (e) {}"
                        + "done({status: r.status, body: body, json: json}); }).catch(e => done({status: -1, body: String(e), json: null}));",
                path);
    }
}
