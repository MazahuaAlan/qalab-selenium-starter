package dev.morewater.qalab.pages.eats;

import dev.morewater.qalab.pages.BasePage;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Eats: lista de restaurantes, menú con diálogo de opciones, carrito, pago y seguimiento. */
public class EatsPage extends BasePage {
    public EatsPage(WebDriver driver) { super(driver); }

    // ---------- Lista ----------
    public EatsPage openList() { go("/eats/"); return this; }

    /** Abre la lista y espera a que cargue (útil con la persona intermitente: reintenta si hay error). */
    public EatsPage openListReady() {
        openList();
        return waitListReady();
    }

    public EatsPage waitListReady() {
        for (int i = 0; i < 25; i++) {
            wait.until(d -> present("eats-count") || present("eats-list-error"));
            if (present("eats-count")) return this;
            click("eats-retry");
        }
        return this;
    }

    public void waitListOutcome() { wait.until(d -> present("eats-count") || present("eats-list-error")); }
    public boolean listFailed() { return present("eats-list-error"); }
    public void back() { click("eats-back"); }

    public String count() { return text("eats-count"); }
    public String restName(int i) { return text("eats-rest-name-" + i); }
    public String restStatus(int i) { return text("eats-rest-status-" + i); }
    public String restMeta(int i) { return text("eats-rest-meta-" + i); }
    public String restCard(int i) { return text("eats-rest-" + i); }
    public WebElement openButton(int i) { return visible("eats-open-" + i); }
    public void search(String q) { type("eats-search", q); }
    public void chip(String id) { click("eats-chip-" + id); }
    public boolean chipPressed(String id) { return "true".equals(visible("eats-chip-" + id).getAttribute("aria-pressed")); }

    public List<String> restNames() {
        List<String> names = new ArrayList<>();
        for (WebElement e : driver.findElements(By.cssSelector("[data-test^='eats-rest-name-']"))) names.add(e.getText().trim());
        return names;
    }

    // ---------- Menú ----------
    public EatsPage openMenu(String restId) {
        go("/eats/restaurant/?id=" + restId);
        return waitMenu();
    }

    public EatsPage waitMenu() { visible("eats-tab-0"); return this; }

    public String menuTitle() { return text("eats-menu-title"); }
    public void tab(int i) { click("eats-tab-" + i); }
    public boolean tabSelected(int i) { return "true".equals(visible("eats-tab-" + i).getAttribute("aria-selected")); }
    public String tabLabel(int i) { return text("eats-tab-" + i); }
    public String itemPrice(String itemId) { return text("eats-item-price-" + itemId); }
    public long itemPriceCents(String itemId) { return cents(itemPrice(itemId)); }
    public boolean itemShown(String itemId) { return present("eats-item-" + itemId); }
    public String itemCard(String itemId) { return text("eats-item-" + itemId); }

    /** Ids (r1-0…) de los productos visibles en la pestaña actual. */
    public List<String> visibleItemIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement e : driver.findElements(By.cssSelector("[data-test^='eats-item-r'][data-test*='-']:not([data-test^='eats-item-price'])"))) {
            ids.add(e.getAttribute("data-test").substring("eats-item-".length()));
        }
        return ids;
    }

    // ---------- Diálogo de opciones ----------
    public EatsPage openDialog(String itemId) { click("eats-add-" + itemId); visible("eats-modal"); return this; }
    public boolean dialogOpen() { return present("eats-modal"); }
    public String dialogTitle() { return visible("eats-modal").findElement(By.tagName("h2")).getText().trim(); }
    public void size(String id) { click("eats-size-" + id); }
    public boolean sizeChecked(String id) { return visible("eats-size-" + id).isSelected(); }
    public String sizeLabel(String id) { return visible("eats-size-" + id).findElement(By.xpath("..")).getText().trim(); }
    public void extra(String id) { click("eats-extra-" + id); }
    public boolean extraChecked(String id) { return visible("eats-extra-" + id).isSelected(); }
    public String extraLabel(String id) { return visible("eats-extra-" + id).findElement(By.xpath("..")).getText().trim(); }
    public void plus() { click("eats-opt-plus"); }
    public void minus() { click("eats-opt-minus"); }
    public boolean plusEnabled() { return visible("eats-opt-plus").isEnabled(); }
    public boolean minusEnabled() { return visible("eats-opt-minus").isEnabled(); }
    public String addButtonText() { return text("eats-opt-add"); }
    public void press(String dataTest) { click(dataTest); }
    public String read(String dataTest) { return text(dataTest); }
    public boolean has(String dataTest) { return present(dataTest); }
    public String dialogQty() { return text("eats-opt-qty"); }
    public String dialogTotal() { return text("eats-opt-total"); }
    public long dialogTotalCents() { return cents(dialogTotal()); }
    public void notes(String s) { type("eats-notes", s); }
    public String notesValue() { return visible("eats-notes").getAttribute("value"); }
    public void confirmAdd() { click("eats-opt-add"); wait.until(ExpectedConditions.invisibilityOfElementLocated(t("eats-modal"))); }
    public void cancelDialog() { click("eats-opt-close"); wait.until(ExpectedConditions.invisibilityOfElementLocated(t("eats-modal"))); }
    public void pressEscape() { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
    public void waitDialogClosed() { wait.until(ExpectedConditions.invisibilityOfElementLocated(t("eats-modal"))); }

    /** Agrega un platillo con opciones desde el menú actual. */
    public EatsPage addDish(String itemId, String size, int qty, String... extras) {
        openDialog(itemId);
        if (size != null) size(size);
        for (String e : extras) extra(e);
        for (int i = 1; i < qty; i++) plus();
        confirmAdd();
        return this;
    }

    /** Abre el menú de un restaurante (r1…r7) y agrega un platillo con opciones. */
    public EatsPage addDishFrom(String restId, int item, String size, int qty, String... extras) {
        openMenu(restId);
        return addDish(restId + "-" + item, size, qty, extras);
    }

    // ---------- Carrito (menú) ----------
    public boolean cartEmpty() { return present("eats-cart-empty"); }
    public String lineTotal(String itemId) { return text("eats-line-total-" + itemId); }
    public String lineQty(String itemId) { return text("eats-qty-" + itemId); }
    public String lineText(String itemId) { return text("eats-line-" + itemId); }
    public int lineCount() { return driver.findElements(By.cssSelector("[data-test^='eats-line-r'][data-test$='']:not([data-test^='eats-line-total'])")).size(); }
    public String cartText() { return text("eats-cart"); }
    public String cartSubtotal() { return text("eats-cart-subtotal"); }
    public long cartSubtotalCents() { return cents(cartSubtotal()); }
    public void linePlus(String itemId) { click("eats-plus-" + itemId); }
    public void lineMinus(String itemId) { click("eats-minus-" + itemId); }
    public void lineRemove(String itemId) { click("eats-remove-" + itemId); }
    public String navCart() { return text("nav-eats-checkout"); }
    public List<WebElement> lines(String itemId) { return driver.findElements(t("eats-line-" + itemId)); }
    public boolean cartCleared() { return present("eats-cart-empty"); }
    public void goCheckout() { click("eats-go-checkout"); visibleEither(); }

    private void visibleEither() { wait.until(d -> present("eats-place-order") || present("eats-checkout-empty")); }

    // ---------- Cambio de restaurante ----------
    public boolean swapModalOpen() { return present("eats-swap-modal"); }
    public void swapConfirm() { click("eats-swap-confirm"); }
    public void swapCancel() { click("eats-swap-cancel"); wait.until(ExpectedConditions.invisibilityOfElementLocated(t("eats-swap-modal"))); }
    public void waitSwapGone() { wait.until(ExpectedConditions.invisibilityOfElementLocated(t("eats-swap-modal"))); visible("eats-modal"); }
    public void waitSwapModal() { visible("eats-swap-modal"); }

    /** Adelanta el reloj del navegador (Date.now) para no esperar tiempos reales. */
    public void advanceClock(long ms) { js("const o = Date.now; Date.now = () => o() + arguments[0];", ms); }

    /** true si el elemento (data-test) aparece antes de agotar la espera acotada. */
    public boolean appearsWithin(String dataTest, int seconds) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(seconds)).until(d -> present(dataTest));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) { return false; }
    }
}
