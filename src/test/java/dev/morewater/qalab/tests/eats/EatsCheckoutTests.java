package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.CheckoutPage;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * HU-EATS pago: desglose de totales (servicio 8 %, envío del restaurante, propina sobre subtotal de comida)
 * y promociones. Los importes esperados se calculan con las fórmulas de lib/eats.ts a partir de los precios leídos del menú.
 */
class EatsCheckoutTests extends BaseTest {
    private long base;

    /** Agrega un platillo y llega al pago; deja en {@code base} el precio base leído del menú. */
    private CheckoutPage toCheckout(String rest, int item, String size, int qty, String... extras) {
        EatsPage eats = new EatsPage(driver).openMenu(rest);
        base = eats.itemPriceCents(rest + "-" + item);
        eats.addDish(rest + "-" + item, size, qty, extras);
        eats.goCheckout();
        return new CheckoutPage(driver).ready();
    }

    private static long pct(long amount, double rate) { return Math.round(amount * rate); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Desglose de totales con tamaño, extra y cantidad (CP-EATS-036)")
    void totalsBreakdown() {
        CheckoutPage co = toCheckout("r1", 0, "md", 2, "queso");
        long sub = (base + 2500 + 1500) * 2;
        assertThat(co.read("eats-summary")).contains("Taquería El Cóndor").contains("2 × Tacos al pastor (Mediano, Queso extra)");
        assertThat(co.subtotal()).isEqualTo(sub);
        assertThat(co.service()).isEqualTo(pct(sub, 0.08));
        assertThat(co.delivery()).isEqualTo(2900);
        assertThat(co.tip()).isEqualTo(pct(sub, 0.10));
        assertThat(co.tipLabel()).contains("Propina (10 %)");
        long total = sub + pct(sub, 0.08) + 2900 + pct(sub, 0.10);
        assertThat(co.total()).isEqualTo(total);
        assertThat(co.placeButtonText()).contains("Pedir").contains(co.read("eats-sum-total"));
        assertThat(co.has("eats-sum-discount")).isFalse();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Restaurante con envío gratis (Burger Jacaranda) (CP-EATS-037)")
    void freeDeliveryRestaurant() {
        CheckoutPage co = toCheckout("r4", 0, null, 1);
        assertThat(co.subtotal()).isEqualTo(base);
        assertThat(co.delivery()).isZero();
        assertThat(co.service()).isEqualTo(pct(base, 0.08));
        assertThat(co.tip()).isEqualTo(pct(base, 0.10));
        assertThat(co.total()).isEqualTo(base + pct(base, 0.08) + pct(base, 0.10));
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.extras_not_charged] Persona descuadre: los extras se muestran pero no se cobran (CP-EATS-042)")
    void extrasAreCharged() {
        CheckoutPage co = toCheckout("r1", 0, "gd", 1, "queso", "guac");
        long unit = base + 5000 + 1500 + 2500;
        co.tip(0);
        assertThat(co.subtotal()).as("subtotal con tamaño y extras").isEqualTo(unit);
        assertThat(co.service()).isEqualTo(pct(unit, 0.08));
        assertThat(co.total()).isEqualTo(unit + pct(unit, 0.08) + 2900);
    }

    @Test
    @Tag("opcional")
    @DisplayName("Límite superior de la propina personalizada: 50 % (CP-EATS-045)")
    void customTipUpperLimit() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        long fixed = base + pct(base, 0.08) + 2900;
        co.customTip("50");
        co.waitText("eats-sum-tip", co.read("eats-sum-tip"));
        assertThat(co.tip()).isEqualTo(pct(base, 0.50));
        assertThat(co.tipLabel()).contains("Propina (50 %)");
        assertThat(co.total()).isEqualTo(fixed + pct(base, 0.50));
        co.customTip("0");
        assertThat(co.tip()).isZero();
        assertThat(co.total()).isEqualTo(fixed);
    }

    @Test
    @Tag("opcional")
    @DisplayName("Propina personalizada inválida se ignora (CP-EATS-046)")
    void invalidCustomTipIsIgnored() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        long fixed = base + pct(base, 0.08) + 2900;
        co.tip(15);
        long tip15 = pct(base, 0.15);
        assertThat(co.tip()).isEqualTo(tip15);
        for (String bad : new String[] {"51", "-5", "abc"}) {
            co.customTipPasted(bad);
            assertThat(co.tip()).as("propina tras escribir " + bad).isEqualTo(tip15);
            assertThat(co.tipLabel()).contains("Propina (15 %)");
        }
        assertThat(co.total()).isEqualTo(fixed + tip15);
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.visual_tip] Persona visual: el botón de propina resaltado no es el elegido (CP-EATS-048)")
    void highlightedTipMatchesChosen() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        assertThat(co.tipPressed(10)).as("propina inicial 10 % resaltada").isTrue();
        co.tip(20);
        assertThat(co.tipPressed(20)).as("20 % resaltado").isTrue();
        assertThat(co.tipPressed(15)).isFalse();
        assertThat(co.tipLabel()).contains("Propina (20 %)");
        assertThat(co.tip()).isEqualTo(pct(base, 0.20));
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.tip_percent_wrong] Persona descuadre: la propina se calcula sobre subtotal + servicio + envío (CP-EATS-049)")
    void tipIsPercentOfFoodSubtotal() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        assertThat(co.subtotal()).isEqualTo(base);
        co.tip(20);
        assertThat(co.tip()).as("propina 20 %").isEqualTo(pct(base, 0.20));
        assertThat(co.total()).isEqualTo(base + pct(base, 0.08) + 2900 + pct(base, 0.20));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("PRIMERA: 20 % de descuento por debajo del tope (CP-EATS-050)")
    void primeraDiscountBelowCap() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        long before = co.total();
        co.applyPromo("PRIMERA");
        assertThat(co.promoMsg()).contains("20 % de descuento").contains("máximo $80.00");
        long discount = Math.min(8000, pct(base, 0.20));
        assertThat(co.discount()).isEqualTo(discount);
        assertThat(co.total()).isEqualTo(before - discount);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("ENVIOGRATIS elimina el costo de envío (CP-EATS-053)")
    void freeShippingPromo() {
        CheckoutPage co = toCheckout("r1", 0, null, 1);
        assertThat(co.delivery()).isEqualTo(2900);
        long before = co.total();
        co.applyPromo("ENVIOGRATIS");
        assertThat(co.promoMsg()).isEqualTo("Envío gratis");
        assertThat(co.delivery()).isZero();
        assertThat(co.has("eats-sum-discount")).isFalse();
        assertThat(co.total()).isEqualTo(before - 2900);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.promo_not_capped] Persona descuadre: PRIMERA ignora el tope de $80.00 (CP-EATS-059)")
    void primeraDiscountIsCapped() {
        CheckoutPage co = toCheckout("r1", 0, null, 10);
        long sub = base * 10;
        assertThat(pct(sub, 0.20)).as("el 20 % debe superar el tope para que la prueba aplique").isGreaterThan(8000);
        co.tip(0);
        co.applyPromo("PRIMERA");
        assertThat(co.promoMsg()).contains("máximo $80.00");
        assertThat(co.discount()).as("descuento aplicado").isEqualTo(8000);
        assertThat(co.total()).isEqualTo(sub + pct(sub, 0.08) + 2900 - 8000);
    }
}
