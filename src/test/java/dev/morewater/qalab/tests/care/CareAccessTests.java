package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.care.CarePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

/** HU-CARE-01 (acceso). Estas pruebas gestionan su propio inicio de sesión (no usan BaseTest). */
class CareAccessTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_001_InicioDeSesionExitosoDelUsuarioEstandar")
    void A_CARE_001_InicioDeSesionExitosoDelUsuarioEstandar() {
        WebDriver driver = DriverFactory.create();
        LoginPage login = new LoginPage(driver).open();
        CarePage care = new CarePage(driver);
        assertThat(care.el("password").getAttribute("type")).isEqualTo("password");
        login.loginAs("estandar", "qalab123");
        assertThat(care.el("user-chip").getText()).isEqualTo("Usuario estándar");
        long fromState = Long.parseLong(care.stateString("s.walletCents"));
        assertThat(care.textOf("wallet-chip").replaceAll("\\s+", " ")).isEqualTo("Saldo " + CarePage.mxn(fromState));
        assertThat(driver.getCurrentUrl()).doesNotContain("/id/");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_CARE_002_ContrasenaIncorrecta")
    void A_CARE_002_ContrasenaIncorrecta() {
        WebDriver driver = DriverFactory.create();
        LoginPage login = new LoginPage(driver).open().loginAs("estandar", "incorrecta1");
        assertThat(login.errorMessage()).isEqualTo("No se pudo entrar. Usuario o contraseña incorrectos.");
        CarePage care = new CarePage(driver);
        care.openPath("/care/");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).contains("/id/").contains("next=%2Fcare%2F");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_005_RedireccionALoginYRetornoALaRutaSolicitada")
    void A_CARE_005_RedireccionALoginYRetornoALaRutaSolicitada() {
        WebDriver driver = DriverFactory.create();
        CarePage care = new CarePage(driver);
        care.openPath("/care/appointments/");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).contains("/id/").contains("next=%2Fcare%2Fappointments");
        new LoginPage(driver).loginAs("estandar", "qalab123");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/care/appointments"));
        assertThat(driver.getCurrentUrl()).contains("/care/appointments");
        assertThat(care.el("care-no-appts").getText()).contains("No hay citas todavía.");
        assertThat(driver.findElement(org.openqa.selenium.By.tagName("h1")).getText()).isEqualTo("Mis citas");
    }
}
