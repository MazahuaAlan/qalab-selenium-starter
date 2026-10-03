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
    @DisplayName("Una contraseña incorrecta muestra un mensaje de error")
    void wrongPasswordShowsError() {
        var login = new LoginPage(DriverFactory.create()).open().loginAs("estandar", "contraseña-mala");
        assertThat(login.errorMessage()).contains("incorrectos");
    }

    @Test
    @DisplayName("El usuario bloqueado no puede entrar y ve un mensaje claro")
    void lockedUserSeesClearMessage() {
        var login = new LoginPage(DriverFactory.create()).open().loginAs("bloqueado", "qalab123");
        assertThat(login.errorMessage()).containsIgnoringCase("bloqueado");
    }
}
