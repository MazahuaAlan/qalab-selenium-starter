package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.care.CarePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-CARE-11: cancelación y reembolso. */
class CareCancelTests extends BaseTest {
    private static final long GENERAL_RATE = 60_000;

    private CarePage care() { return new CarePage(driver); }

    /** Agenda una cita de Medicina general hoy+días y deja la página en Mis citas. Devuelve el saldo previo a pagar. */
    private long bookIn(CarePage c, int days) {
        c.completeProfile();
        c.toStep3("general", 0, days);
        c.typeReason(CarePage.REASON);
        long before = c.walletCents();
        c.confirmAndAwaitBooked();
        assertThat(before - c.walletCents()).isEqualTo(GENERAL_RATE);
        return before;
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cancelación gratuita con 24 h o más: reembolso 100 % (CP-CARE-080)")
    void freeCancellationRefundsEverything() {
        CarePage c = care();
        long before = bookIn(c, 3);
        c.openCancelModal();
        assertThat(c.cancelPolicy()).isEqualTo("Cancelación gratuita: recibirás " + CarePage.mxn(GENERAL_RATE) + ".");
        c.confirmCancel();
        assertThat(c.textOf("care-appt-status")).isEqualTo("cancelada");
        assertThat(c.textOf("care-appts-msg")).matches("Cita \\w+ cancelada\\.");
        assertThat(c.walletCents()).isEqualTo(before);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cancelación con menos de 24 h retiene el 50 % (CP-CARE-081)")
    void lateCancellationKeepsHalf() {
        CarePage c = care();
        long before = bookIn(c, 1);
        String date = c.stateString("s.care.appts[0].date"), time = c.stateString("s.care.appts[0].time");
        c.setClockBefore(date, time, 5); // la cita queda a 5 h: menos de 24 h
        c.openCancelModal();
        long half = GENERAL_RATE / 2;
        c.becomesTrue(10, d -> c.cancelPolicy().startsWith("Con menos de 24 horas"));
        assertThat(c.cancelPolicy()).isEqualTo("Con menos de 24 horas de anticipación se retiene el 50 %: recibirás " + CarePage.mxn(half) + ".");
        c.confirmCancel();
        assertThat(c.textOf("care-appt-status")).isEqualTo("cancelada");
        assertThat(c.walletCents()).isEqualTo(before - GENERAL_RATE + half);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.cancel_fee_wrong] Persona descuadre: cancelación con más de 24 h retiene el 50 % (CP-CARE-085)")
    void cancellationWithMoreThan24HoursIsFree() {
        CarePage c = care();
        long before = bookIn(c, 3);
        c.openCancelModal();
        assertThat(c.cancelPolicy()).isEqualTo("Cancelación gratuita: recibirás " + CarePage.mxn(GENERAL_RATE) + ".");
        c.confirmCancel();
        assertThat(c.textOf("care-appt-status")).isEqualTo("cancelada");
        assertThat(c.walletCents()).as("saldo tras cancelar con 3 días de anticipación").isEqualTo(before);
    }
}
