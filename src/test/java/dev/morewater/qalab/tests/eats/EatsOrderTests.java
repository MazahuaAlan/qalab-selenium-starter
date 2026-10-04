package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.CheckoutPage;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-EATS confirmación, seguimiento y cancelación del pedido. */
class EatsOrderTests extends BaseTest {

    private CheckoutPage dishToCheckout(String rest, int item, int qty) {
        EatsPage eats = new EatsPage(driver).openMenu(rest);
        eats.addDish(rest + "-" + item, null, qty);
        eats.goCheckout();
        return new CheckoutPage(driver).ready();
    }

    private CheckoutPage placedOrder() {
        CheckoutPage co = dishToCheckout("r1", 0, 1);
        co.place();
        co.orderTitle();
        return co;
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_060_PedidoExitosoCobroSaldoYMovimiento")
    void A_EATS_060_PedidoExitosoCobroSaldoYMovimiento() {
        CheckoutPage co = dishToCheckout("r1", 0, 2);
        long total = co.total();
        long walletBefore = co.wallet();
        co.place();
        assertThat(driver.getCurrentUrl()).matches(".*/eats/order/\\?id=ED[0-9A-Z]{4}$");
        assertThat(co.orderTitle()).matches("Pedido ED[0-9A-Z]{4}");
        assertThat(co.orderTotal()).isEqualTo(total);
        assertThat(co.wallet()).isEqualTo(walletBefore - total);
        String id = co.orderId();
        co.go("/wallet/");
        String movements = co.read("wallet-balance");
        assertThat(EatsPage.cents(movements)).isEqualTo(walletBefore - total);
        assertThat(driver.getPageSource()).contains("Pedido Taquería El Cóndor · " + id);
        assertThat(new EatsPage(driver).openList().navCart()).contains("Carrito (0)");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_065_ElResumenDelSeguimientoCoincideConElCheckout")
    void A_EATS_065_ElResumenDelSeguimientoCoincideConElCheckout() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long suadero = eats.itemPriceCents("r1-1");
        eats.addDish("r1-1", null, 2);
        eats.tab(2);
        long flan = eats.itemPriceCents("r1-7");
        eats.addDish("r1-7", null, 1);
        eats.goCheckout();
        CheckoutPage co = new CheckoutPage(driver).ready();
        long sub = suadero * 2 + flan;
        assertThat(co.subtotal()).isEqualTo(sub);
        long total = co.total();
        assertThat(total).isEqualTo(sub + Math.round(sub * 0.08) + 2900 + Math.round(sub * 0.10));
        co.place();
        String summary = co.orderSummary();
        assertThat(summary).contains("2 × Tacos de suadero").contains("1 × Flan");
        assertThat(summary).contains(co.read("eats-order-total"));
        assertThat(co.orderTotal()).isEqualTo(total);
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.slow_order] A_EATS_067_PersonaLentaConfirmarElPedidoTardaMasDe35S")
    void A_EATS_067_PersonaLentaConfirmarElPedidoTardaMasDe35S() {
        CheckoutPage co = dishToCheckout("r1", 0, 1);
        long t0 = System.currentTimeMillis();
        co.place();
        co.orderTitle();
        assertThat(System.currentTimeMillis() - t0).as("milisegundos para confirmar").isLessThan(3500);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_068_LosEstadosAvanzanCada6Segundos")
    void A_EATS_068_LosEstadosAvanzanCada6Segundos() {
        CheckoutPage co = placedOrder();
        assertThat(co.status()).isEqualTo("Recibido");
        assertThat(co.stepCurrent(0)).isTrue();
        assertThat(co.stepDone(0)).isFalse();
        co.advanceClock(6500);
        co.waitStatus("Preparando");
        assertThat(co.stepDone(0)).isTrue();
        assertThat(co.stepCurrent(1)).isTrue();
        co.advanceClock(6000);
        co.waitStatus("En camino");
        assertThat(co.stepDone(0) && co.stepDone(1)).isTrue();
        co.advanceClock(6000);
        co.waitStatus("Entregado");
        assertThat(co.stepCurrent(3)).isTrue();
        assertThat(co.eta()).isEqualTo("Tu pedido llegó.");
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.flaky_status] A_EATS_073_PersonaIntermitenteElSeguimientoAVecesRetrocedeUnEstado")
    void A_EATS_073_PersonaIntermitenteElSeguimientoAVecesRetrocedeUnEstado() {
        CheckoutPage co = placedOrder();
        co.advanceClock(7000);
        co.waitStatus("Preparando");
        assertThat(co.statusRegressesWithin(9)).as("el estado retrocedió").isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_074_CancelarATiempoReembolsaElTotal")
    void A_EATS_074_CancelarATiempoReembolsaElTotal() {
        CheckoutPage co = dishToCheckout("r1", 0, 1);
        long walletBefore = co.wallet();
        long total = co.total();
        co.place();
        assertThat(co.wallet()).isEqualTo(walletBefore - total);
        String id = co.orderId();
        assertThat(co.cancelEnabled()).isTrue();
        co.cancel();
        assertThat(co.cancelledText()).contains("Pedido cancelado").contains(co.read("eats-order-total"));
        assertThat(EatsPage.cents(co.cancelledText().substring(co.cancelledText().indexOf('$')).replaceAll("\\.$", ""))).isEqualTo(total);
        assertThat(co.wallet()).isEqualTo(walletBefore);
        co.go("/wallet/");
        co.read("wallet-balance");
        assertThat(driver.getPageSource()).contains("Reembolso pedido " + id).contains("Pedido Taquería El Cóndor · " + id);
        co.go("/eats/orders/");
        assertThat(co.orderState()).isEqualTo("cancelado");
    }
}
