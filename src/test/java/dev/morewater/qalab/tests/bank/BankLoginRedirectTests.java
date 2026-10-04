package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.config.Config;
import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/** HU: acceso protegido. No usa BaseTest: gestiona su propio inicio de sesión. */
class BankLoginRedirectTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    private void waitUrlContains(WebDriver d, String part) {
        new WebDriverWait(d, Duration.ofSeconds(15)).until(x -> x.getCurrentUrl().contains(part));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Redirección al login sin sesión y retorno a la ruta pedida (CP-BANK-094)")
    void redirectsToLoginAndReturns() {
        WebDriver d = DriverFactory.create();
        d.get(Config.baseUrl() + "/bank/transfer/");
        waitUrlContains(d, "/id/");
        assertThat(d.getCurrentUrl()).contains("next=%2Fbank%2Ftransfer%2F");
        new LoginPage(d).loginAs(Config.user(), Config.password());
        new WebDriverWait(d, Duration.ofSeconds(15)).until(x -> x.getCurrentUrl().contains("/bank/transfer"));
        assertThat(new WebDriverWait(d, Duration.ofSeconds(15))
                .until(x -> x.findElement(By.tagName("h1"))).getText()).contains("Transferencia SPEI");
        // Otras rutas protegidas también conservan next (sesión nueva sin iniciar).
        DriverFactory.quit();
        d = DriverFactory.create();
        for (String route : new String[] {"/bank/movements/", "/bank/cards/"}) {
            d.get(Config.baseUrl() + route);
            waitUrlContains(d, "/id/");
            assertThat(d.getCurrentUrl()).contains("next=" + route.replace("/", "%2F"));
        }
    }
}
