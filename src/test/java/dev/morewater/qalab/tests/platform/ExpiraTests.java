package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.config.Config;
import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.platform.IdPage;
import dev.morewater.qalab.pages.platform.WalletPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Caducidad de sesión. Comportamiento correcto: la sesión es estable (o, si caduca, se avisa al usuario). El defecto
 * platform.session_expiry (persona «expira») la cierra a los ~90 s sin aviso previo.
 */
class ExpiraTests extends BaseTest {
    /** El defecto actúa a los 90 s; 97 s deja margen para el temporizador de 1 s y la carga. */
    private static final int ESPERA_S = 97;

    @Test
    @Tag("bug")
    @Tag("slow")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_PLAT_027_LaSesionNoCaducaALos90SEnUnaPantallaPublica")
    void A_PLAT_027_LaSesionNoCaducaALos90SEnUnaPantallaPublica() {
        var p = new IdPage(driver);
        p.open("/docs/");
        assertThat(p.hasUserChip()).isTrue();
        boolean expiro = p.waitSessionLost(ESPERA_S);
        assertThat(expiro).as("la sesión no debería cerrarse sola a los ~90 s (URL: %s)", driver.getCurrentUrl()).isFalse();
        assertThat(p.hasUserChip()).isTrue();
    }

    @Test
    @Tag("bug")
    @Tag("slow")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_PLAT_028_EnWalletLaSesionSeConservaOSiCaducaSeMuestraElAviso")
    void A_PLAT_028_EnWalletLaSesionSeConservaOSiCaducaSeMuestraElAviso() {
        var w = new WalletPage(driver).openWallet();
        assertThat(w.hasBalance()).isTrue();
        w.waitSessionLost(ESPERA_S);
        w.waitReady(); // la página ya hidrató
        boolean sesionViva = w.hasUserChip();
        boolean avisa = w.hasExpiredNotice();
        assertThat(sesionViva || avisa).as("sin sesión y sin aviso de expiración (URL: %s)", driver.getCurrentUrl()).isTrue();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_PLAT_031_ReentrarTrasExpirarYRecuperarSesion")
    void A_PLAT_031_ReentrarTrasExpirarYRecuperarSesion() {
        var p = new IdPage(driver);
        p.open("/");
        p.setWallet(777_700);
        p.until(() -> p.walletCents() == 777_700);
        long inicio = ((Number) p.session().get("startedAt")).longValue();
        // Simula que la sesión ya tiene más de 90 s y recarga: una sesión correcta no se cierra sola.
        p.ageSession(100_000);
        driver.navigate().refresh();
        p.waitReady();
        assertThat(p.waitSessionLost(5)).as("la sesión envejecida no debería cerrarse sola").isFalse();
        // Flujo de reentrada desde el aviso de expiración.
        p.openId("?motivo=expirada");
        assertThat(p.hasExpiredNotice()).isTrue();
        p.clickPersona(Config.user()).submit();
        p.until(() -> p.hasUserChip() && p.path().equals("/"));
        assertThat(((Number) p.session().get("startedAt")).longValue()).isGreaterThan(inicio - 100_000);
        assertThat(p.walletCents()).isEqualTo(777_700);
    }
}
