package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayBookPage;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-05: retención de la habitación (10 minutos). El paso del tiempo se simula retrasando draft.heldAt en el estado local y recargando. */
class HoldTests extends StayTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Reservar una habitación inicia la retención de 10 minutos (CP-STAY-031)")
    void reservingStartsTenMinuteHold() {
        Plan p = defaultPlan(14);
        StayBookPage b = startBooking(p);
        assertThat(b.holdText()).contains("Tu habitación está retenida");
        assertThat(b.timer()).matches("^(10:00|09:5\\d)$");
        int first = b.timerSeconds();
        assertThat(b.becomesTrue(() -> b.timerSeconds() <= first - 2, 8, 200)).as("el temporizador debe bajar un segundo por segundo").isTrue();
        assertThat(b.summaryText()).contains("Casa Cóndor", "Habitación superior", "3 noches", "2 huéspedes");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Expira la retención a los 10 minutos y libera la habitación (CP-STAY-032)")
    void holdExpiresAfterTenMinutes() {
        StayBookPage b = startBooking(defaultPlan(14));
        assertThat(b.timer()).matches("^(10:00|09:5\\d)$");

        b.ageHold(70);
        b.reload();
        b.waitForm();
        assertThat(b.timerSeconds()).as("tras 70 s quedan ≈ 530 s").isBetween(500, 531);

        b.ageHold(610);
        b.reload();
        var hotel = hotelPage();
        hotel.waitExpired();
        assertThat(driver.getCurrentUrl()).contains("expired=1");
        assertThat(hotel.expiredText()).isEqualTo("Se liberó la habitación porque terminó el tiempo de retención. Elige de nuevo.");
        b.open().waitNoDraft();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Recargar conserva el borrador y el temporizador continúa (CP-STAY-033)")
    void reloadKeepsDraftAndTimer() {
        StayBookPage b = startBooking(defaultPlan(14));
        b.name("Ana Pérez López");
        int noted = b.timerSeconds();
        assertThat(b.becomesTrue(() -> b.timerSeconds() <= noted - 3, 10, 200)).isTrue();
        b.reload();
        b.waitForm();
        assertThat(b.noDraft()).isFalse();
        assertThat(b.timerSeconds()).as("el tiempo no se reinició").isLessThan(noted);
        assertThat(b.nameValue()).isEqualTo("Ana Pérez López");
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.amnesia_booking] Al recargar se pierde la reserva en curso (stay.amnesia_booking) (CP-STAY-096)")
    void reloadKeepsBookingInProgress() {
        StayBookPage b = startBooking(defaultPlan(14));
        b.name("Ana Pérez López");
        b.reload();
        b.eventually(() -> assertThat(b.formVisible() || b.noDraft()).isTrue());
        assertThat(b.noDraft()).as("«No hay una reserva en curso» tras recargar").isFalse();
        b.waitForm();
        assertThat(b.nameValue()).isEqualTo("Ana Pérez López");
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.hold_short] La retención dura 20 s en vez de 10 min (stay.hold_short) (CP-STAY-097)")
    void holdLastsTenMinutesNotTwentySeconds() {
        StayBookPage b = startBooking(defaultPlan(14));
        assertThat(b.timer()).as("temporizador inicial").matches("^(10:00|09:5\\d)$");
        b.ageHold(22);
        b.reload();
        b.eventually(() -> assertThat(b.formVisible() || hotelPage().expiredNotice()).isTrue());
        assertThat(hotelPage().expiredNotice()).as("la habitación no debe liberarse a los 22 s").isFalse();
        assertThat(b.timerSeconds()).isBetween(540, 580);
    }
}
