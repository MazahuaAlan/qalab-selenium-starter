package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.config.Config;
import dev.morewater.qalab.pages.platform.IdPage;
import dev.morewater.qalab.pages.platform.WalletPage;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Guardia de rutas, parámetro next y ciclo de vida de la sesión (sin expiración, que vive en ExpiraTests). */
class SesionTests extends NoSessionTest {
    private static final String HOST = URI.create(Config.baseUrl()).getHost();

    private String hostNow() { return URI.create(driver.getCurrentUrl()).getHost(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Guardia: /wallet/ sin sesión redirige a login con next (CP-PLAT-020)")
    void guardiaRedirigeALogin() {
        var w = new WalletPage(driver);
        w.go("/wallet/");
        w.until(() -> driver.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).endsWith("/id/?next=%2Fwallet%2F");
        assertThat(w.hasBalance()).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Login con next vuelve a la ruta original (CP-PLAT-021)")
    void loginConNextVuelveALaRuta() {
        var id = new IdPage(driver);
        id.go("/wallet/");
        id.until(() -> driver.getCurrentUrl().contains("/id/?next="));
        id.clickPersona("estandar").submit();
        id.until(() -> id.path().equals("/wallet/"));
        var w = new WalletPage(driver);
        assertThat(w.balanceText()).isEqualTo(WalletPage.mxn(w.walletCents()));
    }

    @Test
    @Tag("recomendado")
    @DisplayName("next ajeno sin «/» inicial se ignora (CP-PLAT-022)")
    void nextAjenoSeIgnora() {
        var id = new IdPage(driver).openId("?next=https%3A%2F%2Fevil.com");
        id.clickPersona("estandar").submit();
        id.until(() -> !driver.getCurrentUrl().contains("/id/"));
        assertThat(hostNow()).isEqualTo(HOST);
        assertThat(URI.create(driver.getCurrentUrl()).getPath()).isEqualTo("/");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("next protocolo-relativo //evil.com (open redirect) (CP-PLAT-023)")
    void nextProtocoloRelativoNoRedirigeAFuera() {
        var id = new IdPage(driver).openId("?next=//evil.com");
        id.clickPersona("estandar").submit();
        id.until(() -> !driver.getCurrentUrl().contains("/id/"));
        // Correcto: el usuario se queda en el dominio de qalab (p. ej. «/»), nunca sale a evil.com.
        assertThat(hostNow()).as("host tras el login con next=//evil.com").isEqualTo(HOST);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("La sesión sobrevive a una recarga (CP-PLAT-024)")
    void sesionSobreviveARecarga() {
        var id = new IdPage(driver).openId().loginOk("estandar");
        var w = new WalletPage(driver).openWallet();
        String antes = w.balanceText();
        driver.navigate().refresh();
        w.waitReady();
        assertThat(w.balanceText()).isEqualTo(antes);
        assertThat(w.path()).isEqualTo("/wallet/");
        assertThat(w.userChip()).isEqualTo("Usuario estándar");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Cerrar sesión (CP-PLAT-025)")
    void cerrarSesion() {
        var id = new IdPage(driver).openId().loginOk("estandar");
        id.logout();
        id.until(() -> id.path().equals("/id/") && id.hasLoginLink());
        assertThat(id.hasUserChip()).isFalse();
        assertThat(id.session()).isNull();
        id.go("/wallet/");
        id.until(() -> driver.getCurrentUrl().contains("/id/"));
        assertThat(driver.getCurrentUrl()).endsWith("/id/?next=%2Fwallet%2F");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Mensaje de sesión expirada accesible por URL directa (CP-PLAT-030)")
    void mensajeExpiradaPorUrl() {
        var id = new IdPage(driver).openId("?motivo=expirada");
        var aviso = id.expiredNotice();
        assertThat(aviso.getDomAttribute("role")).isEqualTo("alert");
        assertThat(aviso.getAttribute("class")).contains("warn");
        assertThat(aviso.getText()).contains("Tu sesión expiró");
        id.openId("?motivo=otra");
        assertThat(id.hasExpiredNotice()).isFalse();
    }
}
