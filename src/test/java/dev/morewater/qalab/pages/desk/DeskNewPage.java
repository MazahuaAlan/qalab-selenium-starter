package dev.morewater.qalab.pages.desk;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Formulario de nuevo ticket en /desk/new/ (incluye el editor de texto enriquecido contenteditable). */
public class DeskNewPage extends DeskBasePage {
    public static final String DEFAULT_DESC = "Descripción suficiente para el ticket de prueba";

    public DeskNewPage(WebDriver driver) { super(driver); }

    public DeskNewPage open() {
        go("/desk/new/");
        visible("desk-new-form");
        waitHydrated();
        return this;
    }

    public DeskNewPage title(String v) { type("desk-new-title", v); return this; }

    public String titleValue() { return visible("desk-new-title").getAttribute("value"); }

    public DeskNewPage kind(String id) { click("desk-type-" + id); return this; }

    public DeskNewPage priority(String v) { selectText("desk-new-priority", v); return this; }

    public DeskNewPage assignee(String v) { selectText("desk-new-assignee", v); return this; }

    public DeskNewPage due(String iso) { setDate("desk-new-due", iso); return this; }

    public DeskNewPage label(String l) { click("desk-label-" + l); return this; }

    public WebElement editor() { return visible("desk-editor"); }

    /** Teclea la descripción en el editor. */
    public DeskNewPage describe(String text) {
        WebElement ed = editor();
        ed.click();
        ed.sendKeys(text);
        return this;
    }

    /** Asigna una descripción larga de golpe (evita teclear cientos de caracteres) y avisa al editor con el evento input. */
    public DeskNewPage describeInstantly(String text) {
        js("const e = arguments[0]; e.textContent = arguments[1]; e.dispatchEvent(new Event('input', {bubbles: true}));", editor(), text);
        wait.until(d -> editorCount().startsWith(text.length() + "/"));
        return this;
    }

    public void toggleBold() { click("desk-fmt-bold"); }

    public String editorCount() { return text("desk-editor-count"); }

    public boolean editorCountIsError() { return visible("desk-editor-count").getAttribute("class").contains("error-text"); }

    public String editorHtml() { return editor().getAttribute("innerHTML"); }

    public DeskNewPage submit() { click("desk-new-submit"); return this; }

    public String titleError() { return text("desk-new-title-error"); }

    public String descError() { return text("desk-new-desc-error"); }

    public String dueError() { return text("desk-new-due-error"); }

    public boolean hasTitleError() { return present("desk-new-title-error"); }

    public boolean hasDescError() { return present("desk-new-desc-error"); }

    public boolean hasDueError() { return present("desk-new-due-error"); }

    public boolean hasFormError() { return present("desk-new-error"); }

    /** Espera a que la ficha del ticket creado esté abierta y devuelve su id. */
    public String awaitCreated() { return text("desk-ticket-id"); }

    public String createTicket(String title, String desc) {
        open().title(title).describe(desc).submit();
        return awaitCreated();
    }

    public String createTicket(String title, String desc, String dueIso) {
        open().title(title).describe(desc).due(dueIso).submit();
        return awaitCreated();
    }

    /** Espera a que termine una llamada simulada iniciada por la acción dada. */
    public void submitAndWaitCall() {
        long before = calls();
        submit();
        waitForCallsAfter(before);
    }

    public void waitFormError() { visible("desk-new-error"); }

    /** Espera a que ocurra el resultado de un envío; devuelve true si se creó el ticket y false si hubo error. */
    public boolean awaitCreatedOrError() {
        return wait.until(d -> present("desk-ticket-id") ? Boolean.TRUE : present("desk-new-error") ? Boolean.FALSE : null);
    }
}
