package dev.morewater.qalab.tests.air;

import dev.morewater.qalab.core.Evidence;
import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.air.AirFlowPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de defectos por persona (CP-AIR-109 a 120). Verifican el comportamiento correcto: con «estandar» pasan.
 * Las de azar usan una semilla de caos fija (window.qalab.setChaos) para que la secuencia sea determinista.
 */
class AirPersonaTests extends BaseTest {
    // Semillas calculadas con la fórmula de lib/net.ts: con ellas la persona intermitente falla justo donde se necesita.
    private static final long SEED_SEARCH_FAILS_FIRST = 5;
    private static final long SEED_PAYMENT_FAILS_FIRST = 7;

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_AIR_109_PersonaExpiraLaSesionCaducaALos90SEnPlenoFlujo")
    void A_AIR_109_PersonaExpiraLaSesionCaducaALos90SEnPlenoFlujo() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1).selectFirstFlight();
        a.shiftClock(95_000);
        assertThat(a.redirectedToLoginWithin(4)).as("redirigido a /id/ por sesión caducada").isFalse();
        assertThat(a.path()).contains("/air/passengers");
        assertThat(a.userChipVisible()).isTrue();
    }

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[air.amnesia_selection] A_AIR_110_PersonaAmnesiaAlRecargarSePierdeLaReservaEnCurso")
    void A_AIR_110_PersonaAmnesiaAlRecargarSePierdeLaReservaEnCurso() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1).selectFirstFlight();
        a.reload();
        assertThat(a.exists("air-no-draft")).as("pasajeros tras recargar").isFalse();
        assertThat(a.exists("air-passengers-form")).isTrue();
        a.fillAllValid(1).submitPassengers().waitPath("/air/seats");
        a.reload();
        assertThat(a.exists("air-no-draft")).as("asientos tras recargar").isFalse();
        a.waitSeatMap().chooseStandardSeats(1).nextToPayment();
        a.reload();
        assertThat(a.exists("air-no-draft")).as("pago tras recargar").isFalse();
        assertThat(a.exists("air-summary")).isTrue();
    }

    @Test @Tag("bug") @Tag("recomendado")
    @DisplayName("[air.visual_overlap] A_AIR_111_PersonaVisualElBotonSeleccionarSeSuperponeAlPrecioA400Px")
    void A_AIR_111_PersonaVisualElBotonSeleccionarSeSuperponeAlPrecioA400Px() {
        AirFlowPage a = new AirFlowPage(driver).resizeTo(400, 800);
        a.openResults("MEX", "MTY", AirFlowPage.iso(7), 1).waitResults();
        assertThat(a.viewportWidth()).as("ancho de ventana efectivo").isLessThanOrEqualTo(500);
        assertThat(a.boxesIntersect("air-price-0", "air-select-0")).as("el botón tapa el precio").isFalse();
    }

    @Test @Tag("bug") @Tag("recomendado")
    @DisplayName("[air.slow_search] A_AIR_117_PersonaLentoLaBusquedaTarda4S")
    void A_AIR_117_PersonaLentoLaBusquedaTarda4S() {
        AirFlowPage a = new AirFlowPage(driver).open();
        a.fillSearch("MEX", "MTY", AirFlowPage.iso(7), 1);
        Evidence.pause();
        long t0 = System.currentTimeMillis();
        a.submitSearch().waitResults();
        Evidence.resume();
        assertThat(System.currentTimeMillis() - t0).as("ms hasta ver resultados").isLessThan(2000);
    }

    @Test @Tag("bug") @Tag("recomendado")
    @DisplayName("[air.slow_seats] A_AIR_118_PersonaLentoElMapaDeAsientosTarda35S")
    void A_AIR_118_PersonaLentoElMapaDeAsientosTarda35S() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1).selectFirstFlight().fillAllValid(1);
        Evidence.pause();
        long t0 = System.currentTimeMillis();
        a.submitPassengers().waitSeatMap();
        Evidence.resume();
        assertThat(System.currentTimeMillis() - t0).as("ms hasta ver el mapa").isLessThan(2000);
    }

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[air.flaky_results] A_AIR_119_PersonaIntermitenteLaBusquedaFallaCon503YReintentarRecupera")
    void A_AIR_119_PersonaIntermitenteLaBusquedaFallaCon503YReintentarRecupera() {
        AirFlowPage a = new AirFlowPage(driver);
        a.open().setChaosSeed(SEED_SEARCH_FAILS_FIRST);
        a.fillSearch("MEX", "MTY", AirFlowPage.iso(7), 1).submitSearch();
        // con la semilla elegida la primera llamada fallaría para la persona intermitente; aquí no debe aparecer error
        assertThat(a.resultsFailed()).as("error 503 en resultados").isFalse();
        assertThat(a.flightCount()).isGreaterThan(0);
    }

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[air.flaky_payment] A_AIR_120_PersonaIntermitenteUnPagoFallidoDescuentaElSaldoYElReintento")
    void A_AIR_120_PersonaIntermitenteUnPagoFallidoDescuentaElSaldoYElReintento() {
        AirFlowPage a = new AirFlowPage(driver);
        a.open().setChaosSeed(SEED_PAYMENT_FAILS_FIRST);
        a.toPayment(1);
        long before = a.walletCents();
        long total = a.total();
        for (int attempt = 1; attempt <= 6; attempt++) {
            a.pay();
            if (!a.paymentFailed()) break;
            assertThat(a.walletCents()).as("saldo tras un pago fallido (intento " + attempt + ")").isEqualTo(before);
            a.waitPayEnabled();
        }
        assertThat(a.path()).contains("/air/confirmation");
        assertThat(a.walletCents()).as("un solo cobro").isEqualTo(before - total);
    }
}
