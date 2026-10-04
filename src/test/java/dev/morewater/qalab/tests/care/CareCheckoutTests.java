package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.care.CarePage;
import dev.morewater.qalab.pages.care.CarePage.Prof;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** HU-CARE-09: confirmación de la cita, copago, retención del horario y fallos. */
class CareCheckoutTests extends BaseTest {
    /** Tarifas de lib/care.ts (centavos). */
    private static final long GENERAL_RATE = 60_000, CARDIO_RATE = 120_000;

    private CarePage care() { return new CarePage(driver); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cita presencial sin seguro: copago completo (CP-CARE-057)")
    void uninsuredPaysFullCopay() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0);
        assertThat(c.textOf("care-picked-doctor")).isEqualTo("Dra. Lucía Romero · Medicina general");
        c.pickDay(1);
        c.chooseFirstFreeAndContinue();
        assertThat(c.textOf("care-timer")).matches("05:0[01]|04:5\\d");
        assertThat(c.textOf("care-sum-doctor").replace("\n", " ")).isEqualTo("Dra. Lucía Romero Medicina general");
        assertThat(c.textOf("care-sum-rate")).isEqualTo(CarePage.mxn(GENERAL_RATE));
        assertThat(c.el("care-summary").getText()).contains("Sin seguro").contains("Pagas el 100 %");
        assertThat(c.textOf("care-sum-copay")).isEqualTo(CarePage.mxn(GENERAL_RATE));
        c.typeReason(CarePage.REASON);
        assertThat(driver.findElement(By.cssSelector("#reason + .hint")).getText()).isEqualTo(CarePage.REASON.length() + "/200");
        assertThat(c.textOf("care-confirm")).isEqualTo("Agendar y pagar " + CarePage.mxn(GENERAL_RATE));
        long before = c.walletCents();
        c.confirmAndAwaitBooked();
        assertThat(driver.getCurrentUrl()).contains("/care/appointments/?nueva=");
        assertThat(c.textOf("care-booked")).contains("agendada").contains("pendiente de confirmación");
        assertThat(before - c.walletCents()).isEqualTo(GENERAL_RATE);
        assertThat(c.textOf("care-appt-status")).isEqualTo("pendiente");
        assertThat(c.textOf("care-appt-when")).contains("copago " + CarePage.mxn(GENERAL_RATE));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cita con seguro: copago del 20 % (CP-CARE-058)")
    void insuredPaysTwentyPercent() {
        CarePage c = care();
        c.completeProfile(Prof.valid().insured());
        c.toStep2("cardiologia", 0);
        assertThat(c.textOf("care-picked-doctor")).isEqualTo("Dra. Renata Villaseñor · Cardiología");
        c.pickDay(1);
        c.chooseFirstFreeAndContinue();
        long copay = CARDIO_RATE * 20 / 100;
        assertThat(c.textOf("care-sum-rate")).isEqualTo(CarePage.mxn(CARDIO_RATE));
        assertThat(c.el("care-summary").getText()).contains("Cobertura Seguro Aurora").contains("Pagas el 20 %");
        assertThat(c.textOf("care-sum-copay")).isEqualTo(CarePage.mxn(copay));
        c.typeReason(CarePage.REASON);
        long before = c.walletCents();
        assertThat(c.textOf("care-confirm")).isEqualTo("Agendar y pagar " + CarePage.mxn(copay));
        c.confirmAndAwaitBooked();
        assertThat(c.textOf("care-appt-status")).isEqualTo("pendiente");
        assertThat(before - c.walletCents()).isEqualTo(copay);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.copay_wrong] Persona descuadre: copago con seguro es 80 % en lugar de 20 % (CP-CARE-060)")
    void insuredCopayIsTwentyPercent() {
        CarePage c = care();
        c.completeProfile(Prof.valid().insured());
        c.toStep3("general", 0, 1);
        long copay = GENERAL_RATE * 20 / 100;
        assertThat(c.textOf("care-sum-copay")).isEqualTo(CarePage.mxn(copay));
        assertThat(c.textOf("care-confirm")).isEqualTo("Agendar y pagar " + CarePage.mxn(copay));
        c.typeReason(CarePage.REASON);
        long before = c.walletCents();
        c.confirmAndAwaitBooked();
        assertThat(before - c.walletCents()).as("importe cobrado").isEqualTo(copay);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Retención del horario: temporizador inicia en 05:00 y expira (CP-CARE-065)")
    void holdTimerStartsAtFiveMinutesAndExpires() {
        CarePage c = care();
        c.completeProfile();
        c.toStep3("general", 0, 1);
        assertThat(c.textOf("care-timer")).matches("05:0[01]|04:5\\d");
        assertThat(c.textOf("care-hold")).contains("Tu horario está retenido");
        c.skewClock(301_000);
        c.el("care-hold-expired");
        assertThat(c.textOf("care-hold-expired")).isEqualTo("Se liberó el horario porque terminó el tiempo de retención. Elige uno de nuevo.");
        assertThat(c.has("care-step2")).isTrue();
        assertThat(c.enabled("care-step2-next")).isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.slot_hold_short] Persona sesión corta: la retención dura 15 s en lugar de 5 min (CP-CARE-066)")
    void holdLastsFiveMinutes() {
        CarePage c = care();
        c.completeProfile();
        c.toStep3("general", 0, 1);
        String first = c.textOf("care-timer");
        assertThat(first).matches("05:0[01]|04:5\\d");
        c.skewClock(16_000);
        // espera acotada: el reloj de la página avanza cada segundo; o cambia el contador o se libera el horario
        c.becomesTrue(5, d -> d.findElements(By.cssSelector("[data-test='care-step3']")).isEmpty()
                || !d.findElement(By.cssSelector("[data-test='care-timer']")).getText().trim().equals(first));
        assertThat(c.has("care-hold-expired")).as("el horario no debería liberarse a los 16 s").isFalse();
        assertThat(c.has("care-step3")).isTrue();
        c.typeReason(CarePage.REASON);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.amnesia_wizard] Persona amnesia: recargar el asistente pierde el borrador (CP-CARE-069)")
    void wizardSurvivesReload() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0).pickDay(1);
        String t = c.firstFreeSlot();
        c.pickSlot(t);
        driver.navigate().refresh();
        c.becomesTrue(15, d -> c.has("care-step1") || c.has("care-step2"));
        assertThat(c.has("care-step2")).as("sigue en el paso 2 tras recargar").isTrue();
        assertThat(c.stateString("s.care.draft !== null")).isEqualTo("true");
        c.awaitSlots();
        assertThat(c.el("care-slot-" + t).getAttribute("aria-pressed")).isEqualTo("true");
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.flaky_booking] Persona intermitente: reserva fallida deja cita duplicada (CP-CARE-071)")
    void failedBookingCreatesNoAppointmentAndRetryBooksOnce() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0);
        c.forceNextCall(false); // la carga de horarios no debe fallar
        c.awaitSlots();
        c.forceNextCall(false);
        c.pickDay(1);
        c.chooseFirstFreeAndContinue();
        c.typeReason(CarePage.REASON);
        long start = c.walletCents();
        c.forceNextCall(true); // la reserva cae en el 35 % de fallo cuando el defecto está activo
        c.clickOn("care-confirm");
        c.becomesTrue(20, d -> d.getCurrentUrl().contains("nueva=") || !d.findElements(By.cssSelector("[data-test='care-book-error']")).isEmpty());
        if (c.has("care-book-error")) {
            assertThat(c.textOf("care-book-error")).isEqualTo("No se pudo agendar la cita. Intenta de nuevo.");
            assertThat(c.apptCount()).as("citas tras un fallo").isZero();
            assertThat(c.walletCents()).as("saldo tras un fallo").isEqualTo(start);
            c.forceNextCall(false);
            c.confirmAndAwaitBooked();
        }
        assertThat(c.apptCount()).as("citas creadas").isEqualTo(1);
        assertThat(start - c.walletCents()).as("cobro único").isEqualTo(GENERAL_RATE);
    }
}
