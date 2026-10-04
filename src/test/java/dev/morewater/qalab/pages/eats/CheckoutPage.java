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
}
