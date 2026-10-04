package dev.morewater.qalab.pages.desk;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Tablero Kanban de /desk/. Columnas: nuevo, progreso, revision, hecho. */
public class DeskBoardPage extends DeskBasePage {
    public static final List<String> COLUMNS = List.of("nuevo", "progreso", "revision", "hecho");

    public DeskBoardPage(WebDriver driver) { super(driver); }

    /** Abre el tablero y espera a que termine el esqueleto de carga. */
    public DeskBoardPage open() { openWithoutWaiting(); waitForBoard(); return this; }

    public DeskBoardPage openWithoutWaiting() { go("/desk/"); return this; }

    public DeskBoardPage waitForBoard() { visible("desk-board"); return this; }

    public boolean loadingShown() { return present("desk-loading"); }

    public DeskBoardPage reload() { driver.navigate().refresh(); waitForBoard(); waitHydrated(); return this; }

    public String count(String col) { return text("desk-count-" + col); }

    public int countValue(String col) { return Integer.parseInt(count(col)); }

    public List<String> cardIds(String col) {
        return driver.findElements(By.cssSelector("[data-test='desk-col-" + col + "'] [data-test^='desk-card-']")).stream()
                .map(e -> e.getAttribute("data-test").substring("desk-card-".length())).toList();
    }

    public int totalCards() { return driver.findElements(By.cssSelector("[data-test^='desk-card-']")).size(); }

    public boolean hasCard(String col, String id) { return cardIds(col).contains(id); }

    public boolean cardExists(String id) { return present("desk-card-" + id); }

    public WebElement card(String id) { return visible("desk-card-" + id); }

    public boolean emptyShown(String col) { return present("desk-empty-" + col); }

    public String emptyText(String col) { return text("desk-empty-" + col); }

    public WebElement openLink(String id) { return visible("desk-open-" + id); }

    public String title(String id) { return text("desk-title-" + id); }

    /** Línea de detalle de la tarjeta, p. ej. «Ana · bug». */
    public String detail(String id) { return card(id).findElement(By.cssSelector(".hint")).getText().trim(); }

    public WebElement moveSelect(String id) { return visible("desk-move-" + id); }

    public List<String> moveOptions(String id) { return new Select(moveSelect(id)).getOptions().stream().map(o -> o.getText().trim()).toList(); }

    public String moveSelected(String id) { return new Select(moveSelect(id)).getFirstSelectedOption().getText().trim(); }

    /** Mueve una tarjeta con el selector «Mover a» y devuelve el aviso (toast) nuevo que aparece. */
    public String move(String id, String toColumn) {
        String previous = currentToast();
        select("desk-move-" + id, toColumn);
        return awaitNewToast(previous);
    }

    public String awaitNewToast(String previous) {
        return wait.until(d -> {
            String t = currentToast();
            return !t.isEmpty() && !t.equals(previous) ? t : null;
        });
    }

    public boolean toastIsError() { return visible("desk-toast").getAttribute("class").contains("bad"); }

    public void search(String q) { type("desk-search", q); }

    public void clearSearch() {
        visible("desk-search").sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.BACK_SPACE);
    }

    public void filterAssignee(String a) { selectText("desk-filter-assignee", a); }

    public void filterPriority(String p) { selectText("desk-filter-priority", p); }

    public void waitCardCount(int n) { wait.until(d -> totalCards() == n); }

    public void waitCount(String col, int n) { wait.until(d -> countValue(col) == n); }

    /** Enfoca el selector «Mover a» de la tarjeta y escribe la inicial de la opción (alternativa por teclado). */
    public boolean focusAndType(String id, String keys) {
        WebElement sel = moveSelect(id);
        js("arguments[0].focus()", sel);
        boolean focused = sel.equals(driver.switchTo().activeElement());
        sel.sendKeys(keys);
        return focused;
    }

    /** Arrastrar y soltar HTML5 simulado con eventos DragEvent y un DataTransfer compartido (Selenium no arrastra HTML5 de forma nativa). */
    public void dragCardOver(String id, String col) {
        js("const card = arguments[0], colEl = arguments[1]; const dt = new DataTransfer(); window.__qaDt = dt;"
                + "card.dispatchEvent(new DragEvent('dragstart', {bubbles: true, cancelable: true, dataTransfer: dt}));"
                + "colEl.dispatchEvent(new DragEvent('dragenter', {bubbles: true, cancelable: true, dataTransfer: dt}));"
                + "colEl.dispatchEvent(new DragEvent('dragover', {bubbles: true, cancelable: true, dataTransfer: dt}));",
                card(id), visible("desk-col-" + col));
    }

    public boolean columnHighlighted(String col) { return visible("desk-col-" + col).getAttribute("class").contains("over"); }

    public String dropOn(String col) {
        String previous = currentToast();
        js("const colEl = arguments[0]; const dt = window.__qaDt;"
                + "colEl.dispatchEvent(new DragEvent('drop', {bubbles: true, cancelable: true, dataTransfer: dt}));"
                + "colEl.dispatchEvent(new DragEvent('dragend', {bubbles: true, dataTransfer: dt}));", visible("desk-col-" + col));
        return awaitNewToast(previous);
    }

    public boolean cardDraggable(String id) { return "true".equals(card(id).getAttribute("draggable")); }

    public void openTicket(String id) { click("desk-open-" + id); }
}
