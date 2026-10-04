package dev.morewater.qalab.pages.desk;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Vista de lista de /desk/list/ con orden, selección y acciones en lote. */
public class DeskListPage extends DeskBasePage {
    public DeskListPage(WebDriver driver) { super(driver); }

    public DeskListPage open() {
        go("/desk/list/");
        visible("desk-table");
        waitHydrated();
        return this;
    }

    public List<String> rowIds() {
        return driver.findElements(By.cssSelector("[data-test^='desk-row-DK-']")).stream()
                .map(e -> e.getAttribute("data-test").substring("desk-row-".length())).toList();
    }

    public String sortAria(String key) { return visible("desk-sort-" + key).findElement(By.xpath("..")).getAttribute("aria-sort"); }

    public String sortHeaderText(String key) { return text("desk-sort-" + key); }

    public String status(String id) { return text("desk-row-status-" + id); }

    public void waitStatus(String id, String status) { wait.until(d -> status.equals(text("desk-row-status-" + id))); }

    private WebElement row(String id) { return visible("desk-row-" + id); }

    /** Fecha de vencimiento (AAAA-MM-DD) sin el texto de la insignia «Vencido». */
    public String due(String id) { return row(id).findElements(By.tagName("td")).get(5).getText().replace("Vencido", "").trim(); }

    public boolean overdue(String id) { return present("desk-overdue-" + id); }

    public void check(String id) { click("desk-select-" + id); }

    public void selectAll() { click("desk-select-all"); }

    public String selectedText() { return text("desk-selected"); }

    public void bulkTo(String status) { select("desk-bulk-status", status); }

    public void applyBulk() { click("desk-bulk-apply"); }

    public String bulkMessage() { return text("desk-bulk-msg"); }
}
