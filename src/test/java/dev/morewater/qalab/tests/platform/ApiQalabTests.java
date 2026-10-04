package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.platform.IdPage;
import dev.morewater.qalab.pages.platform.PlatformPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** API window.qalab para pruebas. */
class ApiQalabTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("window.qalab expone la API completa (CP-PLAT-056)")
    void apiCompleta() {
        var p = new PlatformPage(driver).open("/");
        List<String> keys = p.eval("return Object.keys(window.qalab)");
        assertThat(keys).containsExactlyInAnyOrder("version", "reset", "state", "setChaos", "calls", "setWallet", "personas");
        assertThat((String) p.eval("return window.qalab.version")).isEqualTo("1");
        assertThat(((Number) p.eval("return window.qalab.personas.length")).intValue()).isEqualTo(8);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("reset() restablece el estado inicial (CP-PLAT-057)")
    void resetRestableceEstadoInicial() {
        var p = new PlatformPage(driver).open("/");
        assertThat(p.hasUserChip()).isTrue();
        p.setWallet(123_400);
        p.setChaosFail(35);
        p.reset();
        p.until(p::hasLoginLink);
        assertThat(p.hasUserChip()).isFalse();
        var st = p.state();
        assertThat(st.get("session")).isNull();
        assertThat(((Number) st.get("walletCents")).longValue()).isEqualTo(6_600_000L);
        assertThat((List<?>) st.get("movements")).isEmpty();
        var c = p.chaos();
        assertThat(((Number) c.get("latencyMs")).intValue()).isZero();
        assertThat(((Number) c.get("failPct")).intValue()).isZero();
        assertThat(((Number) c.get("corruptPct")).intValue()).isZero();
        assertThat(((Number) c.get("seed")).intValue()).isEqualTo(1);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("calls() cuenta peticiones terminadas (CP-PLAT-061)")
    void callsCuentaPeticiones() {
        var p = new IdPage(driver).openId();
        p.reset();
        driver.navigate().refresh();
        p.waitReady();
        p.visible("login-form");
        assertThat(p.callCount()).isZero();
        p.loginAs("estandar", "qalab12");
        p.waitError();
        p.until(() -> p.callCount() == 1);
        p.loginAs("estandar", IdPage.PASSWORD);
        p.until(() -> p.hasUserChip() && p.callCount() == 2);
        assertThat(p.callCount()).isEqualTo(2);
    }
}
