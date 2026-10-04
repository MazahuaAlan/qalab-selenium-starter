package dev.morewater.qalab.tests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Estas pruebas no usan BaseTest: gestionan su propio inicio de sesión. */
class LoginTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    @Test
    @DisplayName("A_PLAT_901_UnaContrasenaIncorrectaMuestraUnMensajeDeError")
    void A_PLAT_901_UnaContrasenaIncorrectaMuestraUnMensajeDeError() {
        var login = new LoginPage(DriverFactory.create()).open().loginAs("estandar", "contraseña-mala");
        assertThat(login.errorMessage()).contains("incorrectos");
    }

    @Test
    @DisplayName("A_PLAT_902_ElUsuarioBloqueadoNoPuedeEntrarYVeUnMensajeClaro")
    void A_PLAT_902_ElUsuarioBloqueadoNoPuedeEntrarYVeUnMensajeClaro() {
        var login = new LoginPage(DriverFactory.create()).open().loginAs("bloqueado", "qalab123");
        assertThat(login.errorMessage()).containsIgnoringCase("bloqueado");
    }
}
