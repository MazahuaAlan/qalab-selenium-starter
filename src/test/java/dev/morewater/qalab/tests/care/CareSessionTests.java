package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.care.CarePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-CARE-01 (sesión). */
class CareSessionTests extends BaseTest {

    @Test
    @Tag("recomendado")
    @DisplayName("Cerrar sesión (CP-CARE-006)")
    void logout() {
        CarePage care = new CarePage(driver);
        care.openPath("/care/");
        care.clickOn("logout");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/id/") && d.findElements(org.openqa.selenium.By.cssSelector("[data-test='user-chip']")).isEmpty());
        assertThat(driver.getCurrentUrl()).contains("/id/");
        assertThat(care.has("user-chip")).isFalse();
        // Atrás no debe devolver acceso a /care/ (la entrada de historial de /care/ ya fue reemplazada por el login)
        driver.navigate().back();
        care.el("brand");
        assertThat(care.has("user-chip")).isFalse();
        care.openPath("/care/");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).contains("/id/");
        assertThat(care.has("user-chip")).isFalse();
        new LoginPage(driver).open().loginAsConfiguredUser();
        assertThat(care.has("user-chip")).isTrue();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] Persona con sesión corta: la sesión no debe caducar a los 90 s (CP-CARE-008)")
    void sessionDoesNotExpireAt90Seconds() {
        CarePage care = new CarePage(driver);
        care.completeProfile();
        care.toStep2("general", 0);
        care.skewClock(95_000); // adelanta el reloj de la página 95 s en vez de esperar
        boolean expired = care.becomesTrue(4, d -> d.getCurrentUrl().contains("motivo=expirada"));
        assertThat(expired).as("la sesión no debería expirar a los 95 s ni perder el flujo").isFalse();
        assertThat(care.has("user-chip")).isTrue();
        assertThat(care.has("care-step2")).isTrue();
    }
}
