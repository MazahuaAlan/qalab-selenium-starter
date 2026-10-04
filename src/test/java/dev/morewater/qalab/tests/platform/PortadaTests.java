package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.platform.HomeDocsPage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Portada pública, enlaces de pie y guía de defectos. */
class PortadaTests extends NoSessionTest {
    @Test
    @Tag("recomendado")
    @DisplayName("Portada muestra héroe, accesos y mapa (CP-PLAT-001)")
    void portadaMuestraHeroeAccesosYMapa() {
        var h = new HomeDocsPage(driver).openHome();
        assertThat(h.heroTitle()).isEqualTo("Una ciudad de apps para romper");
        assertThat(h.homeLoginText()).isEqualTo("Entrar con un usuario de prueba");
        assertThat(h.homeDocsText()).isEqualTo("Guía para pruebas");
        assertThat(h.metroMapVisible()).isTrue();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Lista de seis apps con enlaces a cada app (CP-PLAT-002)")
    void listaDeSeisApps() {
        var h = new HomeDocsPage(driver).openHome();
        assertThat(h.appIds()).containsExactlyInAnyOrder("app-air", "app-bank", "app-stay", "app-eats", "app-care", "app-desk");
        assertThat(h.hrefOf("app-bank")).isEqualTo("/bank/");
        h.clickApp("air");
        // /air/ exige sesión: sin ella la guardia lleva a /id/?next=%2Fair%2F; en ambos casos la tarjeta navegó a /air/.
        h.until(() -> driver.getCurrentUrl().contains("/air/") || driver.getCurrentUrl().contains("next=%2Fair%2F"));
    }

    @Test
    @Tag("opcional")
    @DisplayName("Enlaces del pie y de la guía (CP-PLAT-004)")
    void enlacesDelPie() {
        var h = new HomeDocsPage(driver).openHome();
        h.clickFooter("docs");
        h.until(() -> h.path().equals("/docs/"));
        assertThat(driver.getTitle()).contains("Guía para pruebas").contains("MoreWater qalab");
        driver.navigate().back();
        h.waitReady();
        h.clickFooter("bugs");
        h.until(() -> driver.getCurrentUrl().endsWith("/bugs.json"));
        String body = h.eval("return document.body.innerText");
        assertThat(body.trim()).startsWith("{").endsWith("}");
        Map<String, Object> r = h.fetchJson("/bugs.json");
        assertThat(r.get("json")).as("JSON válido").isNotNull();
    }
}
