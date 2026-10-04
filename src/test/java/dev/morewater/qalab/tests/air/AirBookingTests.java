package dev.morewater.qalab.tests.air;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.air.AirFlowPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU pago, confirmación, mis viajes, cancelación y check-in (CP-AIR-068 a 104). */
class AirBookingTests extends BaseTest {

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_068_PagoExitosoConBilleteraDescuentaUnaSolaVezYConfirma")
    void A_AIR_068_PagoExitosoConBilleteraDescuentaUnaSolaVezYConfirma() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(1);
        long before = a.walletCents();
        long total = a.total();
        assertThat(a.exists("air-method-wallet")).isTrue();
        assertThat(a.selected("air-method-wallet")).isTrue();
        String code = a.payAndConfirm();
        assertThat(a.textOf("air-confirmed-title")).isEqualTo("¡Reserva confirmada!");
        assertThat(code).matches("^MW[A-Z0-9]{4}$");
        assertThat(a.walletCents()).isEqualTo(before - total);
        assertThat(AirFlowPage.cents(a.walletChip())).isEqualTo(before - total);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_086_ConfirmacionMuestraLosDatosDeLaReserva")
    void A_AIR_086_ConfirmacionMuestraLosDatosDeLaReserva() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(2);
        long total = a.total();
        String seats = a.textOf("air-sum-seats");
        String[] ids = seats.split(",\\s*");
        a.payAndConfirm();
        assertThat(a.textOf("air-confirmed-title")).isEqualTo("¡Reserva confirmada!");
        assertThat(a.textOf("air-conf-passenger-0")).contains("Ana0 Pérez").contains("asiento " + ids[0]);
        assertThat(a.textOf("air-conf-passenger-1")).contains("Ana1 Pérez").contains("asiento " + ids[1]);
        assertThat(AirFlowPage.cents(a.textOf("air-conf-total"))).isEqualTo(total);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_089_MisViajesListaLaReservaConEstadoConfirmada")
    void A_AIR_089_MisViajesListaLaReservaConEstadoConfirmada() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(1);
        long total = a.total();
        String code = a.payAndConfirm();
        a.clickOn("air-go-trips");
        a.waitPath("/air/trips");
        assertThat(a.textOf("air-trip-code")).isEqualTo(code);
        assertThat(a.textOf("air-trip-status")).isEqualTo("confirmada");
        assertThat(a.textOf("air-trip")).contains("Ciudad de México (MEX) → Monterrey (MTY)").contains("1 pasajero(s)");
        assertThat(AirFlowPage.cents(a.textOf("air-trip").replaceAll("(?s).*1 pasajero\\(s\\) · (\\$[\\d,.]+).*", "$1"))).isEqualTo(total);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_091_CancelarReservaReembolsaEl90")
    void A_AIR_091_CancelarReservaReembolsaEl90() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(1);
        long start = a.walletCents();
        long total = a.total();
        String code = a.payAndConfirm();
        long afterPay = a.walletCents();
        assertThat(afterPay).isEqualTo(start - total);
        a.openPath("/air/trips/");
        a.clickOn("air-cancel");
        assertThat(a.textOf("air-cancel-confirm")).isNotEmpty();
        a.clickOn("air-cancel-confirm");
        assertThat(a.textOf("air-trips-msg")).contains(code).contains("cancelada").contains("90 %");
        assertThat(a.textOf("air-trip-status")).isEqualTo("cancelada");
        long refund = Math.round(total * 0.9);
        assertThat(a.walletCents()).isEqualTo(afterPay + refund);
        assertThat(AirFlowPage.cents(a.walletChip())).isEqualTo(afterPay + refund);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_095_CheckInExitosoYPaseDeAbordar")
    void A_AIR_095_CheckInExitosoYPaseDeAbordar() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(1);
        String seat = a.textOf("air-sum-seats");
        String code = a.payAndConfirm();
        a.clickOn("air-go-checkin");
        a.waitPath("/air/checkin");
        a.typeInto("air-checkin-code", code);
        a.typeInto("air-checkin-last", "Pérez");
        a.clickOn("air-checkin-find");
        assertThat(a.textOf("air-checkin-status")).isEqualTo("confirmada");
        a.clickOn("air-checkin-do");
        assertThat(a.textOf("air-checkin-status")).isEqualTo("check-in");
        assertThat(a.textOf("air-pass-0")).contains("Ana0 Pérez").contains("Asiento " + seat);
        assertThat(driver.findElements(org.openqa.selenium.By.cssSelector("[data-test='air-pass-0'] svg[aria-label='Código de barras " + code + "-" + seat + "']"))).hasSize(1);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_102_ElBorradorDeReservaSeConservaAlRecargarEstandar")
    void A_AIR_102_ElBorradorDeReservaSeConservaAlRecargarEstandar() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1).selectFirstFlight();
        a.typeInto("air-first-0", "Ana0");
        String summary = a.flightSummary();
        a.reload();
        assertThat(a.exists("air-no-draft")).isFalse();
        assertThat(a.flightSummary()).isEqualTo(summary);
        assertThat(a.fieldValue("air-first-0")).isEqualTo("Ana0");
    }
}
