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

/** HU: caducidad de sesión. */
class BankSessionTests extends BaseTest {
    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[platform.session_expiry] A_BANK_098_PersonaExpiraLaSesionCaducaALos90SSinAviso")
    void A_BANK_098_PersonaExpiraLaSesionCaducaALos90SSinAviso() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba");
        // Se adelanta el inicio de sesión 91 s (en vez de esperar 91 s reales) y se recarga.
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const k='qalab.state.v1'; const s=JSON.parse(localStorage.getItem(k)); s.session.startedAt=Date.now()-91000; localStorage.setItem(k, JSON.stringify(s));");
        p.reload().waitAnyStep();
        // Espera acotada: el vigilante de sesión revisa cada 1 s.
        long t0 = System.currentTimeMillis();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(x -> System.currentTimeMillis() - t0 > 4_000);
        assertThat(driver.getCurrentUrl()).doesNotContain("/id/");
        assertThat(driver.findElements(By.cssSelector("[data-test='session-expired']"))).isEmpty();
        assertThat(p.onConfirm()).isTrue();
    }
}
