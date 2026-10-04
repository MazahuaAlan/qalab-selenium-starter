package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.platform.IdPage;
import dev.morewater.qalab.pages.platform.PlatformPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Panel de caos y parámetros por URL (no dependen de la persona: no inician sesión). */
class CaosTests extends NoSessionTest {

    private void assertChaos(PlatformPage p, int lat, int fail, int corrupt, int seed) {
        var c = p.chaos();
        assertThat(((Number) c.get("latencyMs")).intValue()).isEqualTo(lat);
        assertThat(((Number) c.get("failPct")).intValue()).isEqualTo(fail);
        assertThat(((Number) c.get("corruptPct")).intValue()).isEqualTo(corrupt);
        assertThat(((Number) c.get("seed")).intValue()).isEqualTo(seed);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Abrir y cerrar el panel de caos (CP-PLAT-046)")
    void abrirYCerrarPanelDeCaos() {
        var p = new PlatformPage(driver).open("/");
        assertThat(p.chaosToggleText()).startsWith("Caos");
        assertThat(p.chaosToggleExpanded()).isEqualTo("false");
        p.toggleChaos();
        var panel = p.chaosPanel();
        assertThat(p.chaosToggleExpanded()).isEqualTo("true");
        assertThat(panel.getDomAttribute("aria-label")).isEqualTo("Panel de caos");
        p.closeChaosPanel();
        p.until(() -> !p.chaosPanelOpen());
        assertThat(p.chaosToggleExpanded()).isEqualTo("false");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Controles del panel fijan el estado (CP-PLAT-047)")
    void controlesDelPanelFijanElEstado() {
        var p = new PlatformPage(driver).open("/");
        p.openChaosPanelIfClosed();
        p.setRange("chaos-latency", 1500);
        p.setRange("chaos-fail", 20);
        p.setRange("chaos-corrupt", 10);
        p.setSeed(7);
        p.until(() -> p.chaosOutputFor("chaos-seed").equals("7"));
        assertThat(p.chaosOutputFor("chaos-latency")).isEqualTo("1500 ms");
        assertThat(p.chaosOutputFor("chaos-fail")).isEqualTo("20 %");
        assertThat(p.chaosOutputFor("chaos-corrupt")).isEqualTo("10 %");
        assertChaos(p, 1500, 20, 10, 7);
        p.closeChaosPanel();
        p.until(() -> !p.chaosPanelOpen());
        assertThat(p.chaosToggleText()).contains("Caos").contains("activo");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Parámetros por URL aplican caos (CP-PLAT-048)")
    void parametrosPorUrlAplicanCaos() {
        var p = new PlatformPage(driver).open("/?latency=1500&fail=20&corrupt=10&seed=7");
        p.until(() -> p.chaosToggleText().contains("activo"));
        assertChaos(p, 1500, 20, 10, 7);
        p.open("/docs/");
        assertChaos(p, 1500, 20, 10, 7);
        assertThat(p.chaosToggleText()).contains("activo");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Valores no numéricos por URL se ignoran (CP-PLAT-050)")
    void valoresNoNumericosSeIgnoran() {
        var p = new PlatformPage(driver).open("/?seed=abc&fail=xyz&latency=");
        assertChaos(p, 0, 0, 0, 1);
        assertThat(p.chaosToggleText()).doesNotContain("activo");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Fallos fail=100 hacen fallar toda petición (CP-PLAT-051)")
    void fail100HaceFallarTodaPeticion() {
        var id = new IdPage(driver).openId("?fail=100");
        id.clickPersona("estandar");
        for (int i = 1; i <= 3; i++) {
            long antes = id.callCount();
            id.submit();
            id.until(() -> id.callCount() > antes && id.buttonEnabled());
            id.waitError();
            assertThat(id.error()).contains("Servicio no disponible (caos)");
        }
        id.toggleChaos();
        id.until(() -> id.chaosLogLines().size() >= 3);
        var lineas = id.chaosLogLines();
        assertThat(lineas).hasSize(3);
        for (var l : lineas) {
            assertThat(l.getText()).contains("503 POST /id/login");
            assertThat(l.getAttribute("class")).contains("sbad");
        }
        assertThat(id.session()).isNull();
    }
}
