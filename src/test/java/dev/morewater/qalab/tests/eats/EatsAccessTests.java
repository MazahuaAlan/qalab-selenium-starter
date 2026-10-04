package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.eats.CheckoutPage;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

/** HU-EATS acceso y sesión. Gestionan su propio inicio de sesión (no usan BaseTest). */
class EatsAccessTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_089_SinSesionEatsRedirigeAlLoginYRegresa")
    void A_EATS_089_SinSesionEatsRedirigeAlLoginYRegresa() {
        WebDriver driver = DriverFactory.create();
        EatsPage eats = new EatsPage(driver);
        eats.go("/eats/checkout/");
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(d -> d.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).contains("next=%2Feats%2Fcheckout%2F");
        eats.press("persona-estandar");
        eats.press("login-button");
        assertThat(eats.read("user-chip")).isEqualTo("Usuario estándar");
        assertThat(eats.read("wallet-chip")).startsWith("Saldo");
        assertThat(new CheckoutPage(driver).empty()).isTrue();
        assertThat(driver.getCurrentUrl()).contains("/eats/checkout/");
        assertThat(eats.read("eats-checkout-empty")).contains("Tu carrito está vacío");
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_EATS_094_PersonaExpiraLaSesionCaducaALos90SSinAviso")
    void A_EATS_094_PersonaExpiraLaSesionCaducaALos90SSinAviso() {
        WebDriver driver = DriverFactory.create();
        new LoginPage(driver).open().loginAsConfiguredUser();
        EatsPage eats = new EatsPage(driver).openListReady();
        assertThat(eats.has("user-chip")).isTrue();
        eats.advanceClock(91_000);
        assertThat(eats.appearsWithin("session-expired", 4)).as("la sesión expiró sin aviso").isFalse();
        assertThat(driver.getCurrentUrl()).doesNotContain("motivo=expirada");
        assertThat(eats.has("user-chip")).isTrue();
    }
}
