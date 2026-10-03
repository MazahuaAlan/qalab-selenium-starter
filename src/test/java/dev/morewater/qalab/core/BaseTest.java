package dev.morewater.qalab.core;

import dev.morewater.qalab.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/** Cada prueba arranca con un navegador limpio y la sesión iniciada con el usuario de prueba configurado. */
@ExtendWith(ScreenshotOnFailure.class)
public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = DriverFactory.create();
        new LoginPage(driver).open().loginAsConfiguredUser();
    }

    @AfterEach
    void tearDown() {
        DriverFactory.quit();
    }
}
