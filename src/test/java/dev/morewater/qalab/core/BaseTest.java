package dev.morewater.qalab.core;

import dev.morewater.qalab.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

/** La evidencia (ScreenshotOnFailure) se carga sola para TODAS las pruebas (autodetección de extensiones de JUnit). Cada prueba arranca con un navegador limpio y la sesión iniciada con el usuario de prueba configurado. */
public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = DriverFactory.create();
        // El inicio de sesión es igual en todas las pruebas: se resume en UNA captura en vez de 4 o 5 pasos repetidos en cada PDF.
        Evidence.pause();
        try {
            new LoginPage(driver).open().loginAsConfiguredUser();
        } finally {
            Evidence.resume();
        }
        Evidence.step(DriverFactory.raw(), "Sesión iniciada como " + dev.morewater.qalab.config.Config.user());
    }

    @AfterEach
    void tearDown() {
        DriverFactory.quit();
    }
}
