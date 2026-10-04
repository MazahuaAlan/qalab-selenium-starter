package dev.morewater.qalab.pages.desk;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Ficha de un ticket: /desk/ticket/?id=DK-n. */
public class DeskTicketPage extends DeskBasePage {
    public DeskTicketPage(WebDriver driver) { super(driver); }

    public DeskTicketPage open(String id) {
        go("/desk/ticket/?id=" + id);
        wait.until(d -> present("desk-ticket-id") || present("desk-ticket-missing"));
        waitHydrated();
        return this;
    }

    public String id() { return text("desk-ticket-id"); }

    public String title() { return text("desk-ticket-title"); }

    public String status() { return text("desk-ticket-status"); }

    public String description() { return text("desk-ticket-desc"); }

    public WebElement descriptionElement() { return visible("desk-ticket-desc"); }

    /** Insignia de prioridad del encabezado (shadow DOM): contenedor = la página completa. */
    public String priorityBadgeText() { return badgeText(driver.findElement(By.tagName("body"))); }

    public String priorityBadgeColor() { return badgeColor(driver.findElement(By.tagName("body"))); }

    public String assignee() { return new Select(visible("desk-assignee")).getFirstSelectedOption().getText().trim(); }

    public String priority() { return new Select(visible("desk-priority")).getFirstSelectedOption().getAttribute("value"); }

    public String statusSelected() { return new Select(visible("desk-status")).getFirstSelectedOption().getAttribute("value"); }

    public boolean labelChecked(String l) { return visible("desk-ticket-label-" + l).isSelected(); }

    public void toggleLabel(String l) { click("desk-ticket-label-" + l); }

    /** Texto «Vence: ...» del panel lateral. */
    public String dueLine() {
        return visible("desk-ticket-side").findElement(By.xpath(".//div[contains(@class,'hint') and contains(.,'Vence')]")).getText().trim();
    }

    public String log() { return text("desk-log"); }

    public String logFirst() { return text("desk-log-0"); }

    public void waitLogFirst(String expected) { wait.until(d -> present("desk-log-0") && expected.equals(text("desk-log-0"))); }

    public void changeStatus(String v) { select("desk-status", v); }

    public void changeAssignee(String v) { selectText("desk-assignee", v); }

    public void changePriority(String v) { selectText("desk-priority", v); }

    public void startTitleEdit() {
        click("desk-title-edit");
        visible("desk-title-input");
    }

    public void typeNewTitle(String v) { type("desk-title-input", v); }

    public void pressEnterInTitle() { visible("desk-title-input").sendKeys(Keys.ENTER); }

    public void saveTitleButton() { click("desk-title-save"); }

    public String titleInputValue() { return visible("desk-title-input").getAttribute("value"); }

    public String message() { return text("desk-ticket-msg"); }

    public boolean hasTitleError() { return present("desk-title-error"); }

    public String titleError() { return text("desk-title-error"); }

    public boolean titleEditorOpen() { return present("desk-title-editor"); }

    /** Espera a que el título mostrado sea el indicado. */
    public void waitTitle(String expected) { wait.until(d -> present("desk-ticket-title") && expected.equals(text("desk-ticket-title"))); }

    // Eliminación (solo administrador)
    public void clickDelete() { click("desk-delete"); }

    public boolean confirmShown() { return present("desk-delete-confirm"); }

    public String confirmText() { return text("desk-delete-confirm"); }

    public void confirmNo() { click("desk-delete-no"); }

    public void confirmYes() { click("desk-delete-yes"); }

    public boolean missingShown() { return present("desk-ticket-missing"); }

    public void waitMissing() { visible("desk-ticket-missing"); }

    /** Espera a que el título mostrado sea el indicado o aparezca un error de edición. */
    public void waitTitleOrError(String expected) {
        wait.until(d -> present("desk-title-error") || (present("desk-ticket-title") && expected.equals(text("desk-ticket-title"))));
    }
}
