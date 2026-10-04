package dev.morewater.qalab.pages.eats;

import dev.morewater.qalab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** Pago, seguimiento y mis pedidos de Eats. */
public class CheckoutPage extends BasePage {
    public CheckoutPage(WebDriver driver) { super(driver); }

    public CheckoutPage open() { go("/eats/checkout/"); return ready(); }
    public CheckoutPage ready() { visible("eats-place-order"); return this; }
    public boolean empty() { wait.until(d -> present("eats-place-order") || present("eats-checkout-empty")); return present("eats-checkout-empty"); }

    public long subtotal() { return cents(text("eats-sum-subtotal")); }
    public long service() { return cents(text("eats-sum-service")); }
    public long delivery() { return cents(text("eats-sum-delivery")); }
    public long tip() { return cents(text("eats-sum-tip")); }
    public long total() { return cents(text("eats-sum-total")); }
    public boolean hasDiscount() { return present("eats-sum-discount"); }
    public long discount() { return cents(text("eats-sum-discount")); }
    public String tipLabel() { return driver.findElement(t("eats-sum-tip")).findElement(org.openqa.selenium.By.xpath("../span")).getText().trim(); }
    public String lineText(String itemId) { return text("eats-sum-line-" + itemId); }
    public String summaryText() { return text("eats-summary"); }
    public String placeButtonText() { return text("eats-place-order"); }

    public void tip(int pct) { click("eats-tip-" + pct); }
    public boolean tipPressed(int pct) { return "true".equals(visible("eats-tip-" + pct).getAttribute("aria-pressed")); }
    public void customTip(String s) { type("eats-tip-custom", s); }
    /** Asigna el valor completo de una sola vez (como pegar el texto), sin pasos intermedios por tecla. */
    public void customTipPasted(String s) { setDate("eats-tip-custom", s); }
    public void applyPromo(String code) { type("eats-promo", code); click("eats-promo-apply"); }
    public String promoMsg() { return text("eats-promo-msg"); }

    public String orderError() { return text("eats-order-error"); }

    /** Confirma el pedido y espera el seguimiento. */
    public CheckoutPage place() {
        click("eats-place-order");
        wait.until(d -> present("eats-order-title") || present("eats-order-error"));
        return this;
    }

    // ---------- Seguimiento ----------
    public String orderTitle() { return text("eats-order-title"); }
    public String orderId() { return orderTitle().replace("Pedido", "").trim(); }
    public String status() { return text("eats-status-text"); }
    public long orderTotal() { return cents(text("eats-order-total")); }
    public String orderSummary() { return text("eats-order-summary"); }
    public boolean cancelEnabled() { return visible("eats-cancel").isEnabled(); }
    public void cancel() { click("eats-cancel"); visible("eats-order-cancelled"); }
    public String cancelledText() { return text("eats-order-cancelled"); }
    public String eta() { return text("eats-eta"); }
    public void rate(int stars) { click("eats-star-" + stars); click("eats-rate-submit"); }
    public void goOrders() { click("eats-go-orders"); }
    public String orderState() { return text("eats-order-state"); }
    public void reorder() { click("eats-reorder"); }

    /** Saldo de la billetera en centavos (chip de la cabecera). */
    public long wallet() { return cents(text("wallet-chip")); }

    public void press(String dataTest) { click(dataTest); }
    public String read(String dataTest) { return text(dataTest); }
    public boolean has(String dataTest) { return present(dataTest); }
    public void waitText(String dataTest, String expected) {
        wait.until(d -> present(dataTest) && driver.findElement(t(dataTest)).getText().trim().equals(expected));
    }

    /** Adelanta el reloj del navegador (Date.now); el seguimiento lo lee cada segundo. */
    public void advanceClock(long ms) { js("const o = Date.now; Date.now = () => o() + arguments[0];", ms); }

    public static int statusIndex(String status) { return java.util.List.of("Recibido", "Preparando", "En camino", "Entregado").indexOf(status); }

    /** Observa el estado hasta {@code seconds} y devuelve true si en algún momento muestra un paso anterior al máximo ya visto. */
    public boolean statusRegressesWithin(int seconds) {
        int[] max = {-1};
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(seconds))
                    .pollingEvery(java.time.Duration.ofMillis(150))
                    .until(d -> {
                        int cur = statusIndex(text("eats-status-text"));
                        if (cur < max[0]) return true;
                        max[0] = Math.max(max[0], cur);
                        return false;
                    });
            return true;
        } catch (org.openqa.selenium.TimeoutException e) { return false; }
    }

    public boolean stepDone(int i) { return visible("eats-step-" + i).getAttribute("class").contains("done"); }
    public boolean stepCurrent(int i) { return "step".equals(visible("eats-step-" + i).getAttribute("aria-current")); }
    public void waitStatus(String expected) { waitText("eats-status-text", expected); }
}
