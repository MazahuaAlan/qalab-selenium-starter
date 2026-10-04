package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.CheckoutPage;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-EATS carrito: líneas, cantidades, cambio de restaurante y persistencia. */
class EatsCartTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Agregar el mismo platillo y opciones suma a la misma línea (CP-EATS-024)")
    void sameDishSumsIntoOneLine() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.addDish("r1-0", null, 2);
        assertThat(eats.lineQty("r1-0")).isEqualTo("2");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(base * 2);
        eats.addDish("r1-0", null, 3);
        assertThat(eats.lines("r1-0")).hasSize(1);
        assertThat(eats.lineQty("r1-0")).isEqualTo("5");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(base * 5);
        assertThat(eats.cartSubtotalCents()).isEqualTo(base * 5);
        assertThat(eats.navCart()).contains("Carrito (5)");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Opciones distintas generan líneas separadas (CP-EATS-025)")
    void differentOptionsMakeSeparateLines() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.addDish("r1-0", null, 1);
        assertThat(eats.lines("r1-0")).hasSize(1);
        eats.addDish("r1-0", "gd", 1);
        assertThat(eats.lines("r1-0")).hasSize(2);
        assertThat(eats.lines("r1-0").get(0).getText()).contains("Chico");
        assertThat(eats.lines("r1-0").get(1).getText()).contains("Grande");
        assertThat(eats.cartSubtotalCents()).isEqualTo(base + base + 5000);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Subtotal del carrito con varios productos (CP-EATS-026)")
    void subtotalWithSeveralProducts() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long suadero = eats.itemPriceCents("r1-1");
        eats.addDish("r1-1", null, 2);
        assertThat(EatsPage.cents(eats.lineTotal("r1-1"))).isEqualTo(suadero * 2);
        eats.tab(2);
        long flan = eats.itemPriceCents("r1-7");
        eats.addDish("r1-7", null, 1);
        assertThat(EatsPage.cents(eats.lineTotal("r1-7"))).isEqualTo(flan);
        assertThat(eats.cartSubtotalCents()).isEqualTo(suadero * 2 + flan);
        assertThat(eats.navCart()).contains("Carrito (3)");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Ajustar cantidades con + y −, y eliminar con − (CP-EATS-027)")
    void adjustQuantitiesAndRemoveWithMinus() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.addDish("r1-0", null, 1);
        eats.linePlus("r1-0");
        eats.linePlus("r1-0");
        assertThat(eats.lineQty("r1-0")).isEqualTo("3");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(base * 3);
        assertThat(eats.cartSubtotalCents()).isEqualTo(base * 3);
        eats.lineMinus("r1-0");
        assertThat(eats.lineQty("r1-0")).isEqualTo("2");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(base * 2);
        eats.lineMinus("r1-0");
        eats.lineMinus("r1-0");
        assertThat(eats.cartEmpty()).isTrue();
        assertThat(eats.has("eats-go-checkout")).isFalse();
        assertThat(eats.navCart()).contains("Carrito (0)");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("«Quitar» elimina la línea completa (CP-EATS-028)")
    void removeDeletesWholeLine() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long gringa = eats.itemPriceCents("r1-2");
        eats.addDish("r1-0", null, 4);
        eats.addDish("r1-2", null, 1);
        assertThat(eats.navCart()).contains("Carrito (5)");
        eats.lineRemove("r1-0");
        assertThat(eats.lines("r1-0")).isEmpty();
        assertThat(eats.cartSubtotalCents()).isEqualTo(gringa);
        assertThat(eats.navCart()).contains("Carrito (1)");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cambiar de restaurante pide confirmación para vaciar el carrito (CP-EATS-029)")
    void switchingRestaurantAsksToEmptyCart() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.addDish("r1-0", null, 1);
        eats.openMenu("r2");
        assertThat(eats.cartEmpty()).isTrue();
        eats.press("eats-add-r2-0");
        eats.waitSwapModal();
        assertThat(eats.read("eats-swap-modal")).contains("¿Vaciar el carrito?").contains("Solo puedes pedir de uno a la vez");
        eats.swapCancel();
        assertThat(eats.dialogOpen()).isFalse();
        assertThat(eats.navCart()).contains("Carrito (1)");
        eats.press("eats-add-r2-0");
        eats.swapConfirm();
        eats.waitSwapGone();
        assertThat(eats.dialogTitle()).isEqualTo("Margarita");
        long margarita = eats.dialogTotalCents();
        assertThat(margarita).isEqualTo(eats.itemPriceCents("r2-0"));
        eats.confirmAdd();
        assertThat(eats.lines("r2-0")).hasSize(1);
        assertThat(EatsPage.cents(eats.lineTotal("r2-0"))).isEqualTo(margarita);
        assertThat(eats.navCart()).contains("Carrito (1)");
        eats.openMenu("r1");
        assertThat(eats.cartEmpty()).as("el carrito de r1 se vació").isTrue();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("El carrito se conserva al recargar la página (CP-EATS-030)")
    void cartSurvivesReload() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long unit = eats.itemPriceCents("r1-0") + 2500 + 1500;
        eats.addDish("r1-0", "md", 2, "queso");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(unit * 2);
        assertThat(eats.navCart()).contains("Carrito (2)");
        driver.navigate().refresh();
        eats.waitMenu();
        assertThat(eats.lineQty("r1-0")).isEqualTo("2");
        assertThat(eats.lineText("r1-0")).contains("Mediano").contains("Queso extra");
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(unit * 2);
        eats.goCheckout();
        CheckoutPage co = new CheckoutPage(driver).ready();
        assertThat(co.subtotal()).isEqualTo(unit * 2);
        driver.navigate().refresh();
        assertThat(co.empty()).isFalse();
        assertThat(co.subtotal()).isEqualTo(unit * 2);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.amnesia_cart] Persona amnesia: el carrito se pierde al recargar (CP-EATS-034)")
    void cartKeptOnReloadAtCheckout() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.addDish("r1-0", null, 1);
        eats.goCheckout();
        CheckoutPage co = new CheckoutPage(driver).ready();
        driver.navigate().refresh();
        assertThat(co.empty()).as("el carrito quedó vacío tras recargar").isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.cart_expires] Persona expira: el carrito se vacía solo a los 30 s (CP-EATS-035)")
    void cartNotEmptiedAfter30Seconds() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.addDish("r1-0", null, 1);
        eats.goCheckout();
        CheckoutPage co = new CheckoutPage(driver).ready();
        eats.advanceClock(32_000);
        assertThat(eats.appearsWithin("eats-checkout-empty", 4)).as("el carrito se vació solo").isFalse();
        assertThat(co.empty()).isFalse();
    }
}
