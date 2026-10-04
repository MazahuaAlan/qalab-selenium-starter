package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayBookPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-12: la sesión no debe caducar sin aviso en mitad de una reserva. */
class SessionTests extends StayTest {

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[platform.session_expiry] La sesión caduca a los 90 s sin aviso (platform.session_expiry) (CP-STAY-089)")
    void sessionDoesNotExpireSilentlyMidBooking() {
        StayBookPage b = startBooking(defaultPlan(14));
        b.ageSession(95); // simula 95 s desde el inicio de sesión sin esperarlos
        b.reload();
        b.waitForm();
        boolean kickedOut = b.becomesTrue(() -> b.path().contains("/id/"), 5, 250);
        assertThat(kickedOut).as("redirigido sin aviso a /id/?motivo=expirada").isFalse();
        assertThat(b.bodyText()).doesNotContain("Tu sesión expiró");
        assertThat(b.formVisible()).isTrue();
    }
}
