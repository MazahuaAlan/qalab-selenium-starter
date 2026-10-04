package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.platform.IdPage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-PLAT: entrada con usuario de prueba (/id/). */
class IdLoginTests extends NoSessionTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_PLAT_006_LoginExitosoConEstandar")
    void A_PLAT_006_LoginExitosoConEstandar() {
        var id = new IdPage(driver).openId();
        id.fill("estandar", IdPage.PASSWORD);
        assertThat(id.attr("password", "type")).isEqualTo("password");
        assertThat(id.username()).isEqualTo("estandar");
        id.submit();
        id.until(() -> id.hasUserChip() && id.path().equals("/"));
        assertThat(id.path()).isEqualTo("/");
        assertThat(id.userChip()).isEqualTo("Usuario estándar");
        assertThat(id.walletChipText()).isEqualTo("Saldo " + IdPage.mxn(id.walletCents()));
        assertThat(id.hasLoginLink()).isFalse();
        assertThat(id.session()).containsEntry("user", "estandar");
    }

    @Test
    @Tag("opcional")
    @DisplayName("A_PLAT_007_RellenarConBotonDePersona")
    void A_PLAT_007_RellenarConBotonDePersona() {
        var id = new IdPage(driver).openId();
        assertThat(id.personaButtons()).isEqualTo(8);
        id.clickPersona("lento");
        assertThat(id.username()).isEqualTo("lento");
        assertThat(id.password()).isEqualTo(IdPage.PASSWORD);
        id.submit();
        id.until(() -> id.hasUserChip());
        assertThat(id.userChip()).isEqualTo("Usuario lento");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_PLAT_008_LosOchoUsuariosDePruebaPuedenListarseYSalvoBloqueadoEntrar")
    void A_PLAT_008_LosOchoUsuariosDePruebaPuedenListarseYSalvoBloqueadoEntrar() {
        var id = new IdPage(driver).openId();
        assertThat(id.personaButtons()).isEqualTo(IdPage.PERSONAS.size());
        for (String p : IdPage.PERSONAS) {
            assertThat(driver.findElements(org.openqa.selenium.By.cssSelector("[data-test='persona-" + p + "']"))).as("botón de " + p).hasSize(1);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> personas = id.eval("return window.qalab.personas");
        for (String p : IdPage.PERSONAS) {
            if (p.equals("bloqueado")) continue; // cubierto por HU de bloqueo: no crea sesión
            id.openId();
            id.clickPersona(p).submit();
            id.until(() -> id.hasUserChip() || id.hasError());
            String expected = personas.stream().filter(m -> p.equals(m.get("id"))).map(m -> (String) m.get("name")).findFirst().orElseThrow();
            assertThat(id.hasError()).as("error al entrar como " + p).isFalse();
            assertThat(id.userChip()).as("chip de " + p).isEqualTo(expected);
            id.reset();
        }
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_PLAT_009_ContrasenaIncorrecta")
    void A_PLAT_009_ContrasenaIncorrecta() {
        var id = new IdPage(driver).openId().loginAs("estandar", "qalab12");
        id.waitError();
        assertThat(id.error()).contains("No se pudo entrar.").contains("Usuario o contraseña incorrectos.");
        assertThat(id.visibleErrorRole()).isEqualTo("alert");
        assertThat(id.session()).isNull();
        assertThat(id.path()).isEqualTo("/id/");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_PLAT_011_FormularioVacio")
    void A_PLAT_011_FormularioVacio() {
        var id = new IdPage(driver).openId().submit();
        id.waitError();
        assertThat(id.error()).contains("Usuario o contraseña incorrectos.");
        assertThat(id.path()).isEqualTo("/id/");
        assertThat(id.session()).isNull();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_PLAT_015_CampoContrasenaEnmascaradoYAutocompletadoCorrecto")
    void A_PLAT_015_CampoContrasenaEnmascaradoYAutocompletadoCorrecto() {
        var id = new IdPage(driver).openId();
        assertThat(id.attr("password", "type")).isEqualTo("password");
        assertThat(id.attr("password", "autocomplete")).isEqualTo("current-password");
        assertThat(id.attr("username", "autocomplete")).isEqualTo("username");
        assertThat(id.sharedPassword()).isEqualTo(IdPage.PASSWORD);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_PLAT_016_LoginFallaConCaosFail100")
    void A_PLAT_016_LoginFallaConCaosFail100() {
        var id = new IdPage(driver).openId("?fail=100");
        id.clickPersona("estandar").submit();
        id.waitError();
        assertThat(id.error()).contains("Servicio no disponible (caos)");
        id.toggleChaos();
        id.until(() -> !id.chaosLogLines().isEmpty());
        assertThat(id.chaosLog().getText()).contains("503 POST /id/login");
        assertThat(id.session()).isNull();
    }
}
