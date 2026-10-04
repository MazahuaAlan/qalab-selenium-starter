package dev.morewater.qalab.tests.platform;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.core.ScreenshotOnFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/** Base para pruebas que gestionan su propia sesión (o no la necesitan): navegador limpio y sin login previo. */
@ExtendWith(ScreenshotOnFailure.class)
abstract class NoSessionTest {
    protected WebDriver driver;

    @BeforeEach
    void startBrowser() { driver = DriverFactory.create(); }

    @AfterEach
    void stopBrowser() { DriverFactory.quit(); }
}
