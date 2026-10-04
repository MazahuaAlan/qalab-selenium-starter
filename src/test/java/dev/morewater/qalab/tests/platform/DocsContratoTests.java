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
class DocsContratoTests extends BaseTest {
    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_PLAT_042_TablaDeDefectosIncluyeLosDosIdsPlatform")
    void A_PLAT_042_TablaDeDefectosIncluyeLosDosIdsPlatform() {
        var h = new HomeDocsPage(driver).openBugsTable();
        int filas = h.bugRows().size();
        assertThat(filas).isPositive();
        assertThat(h.bugsCountText()).contains(filas + " entradas");
        List<List<String>> platform = h.bugRows().stream().map(h::cells).filter(c -> c.get(0).startsWith("platform.")).collect(Collectors.toList());
        assertThat(platform).hasSize(2);
        assertThat(platform).anySatisfy(c -> assertThat(c).startsWith("platform.login_locked", "bloqueado", "diseño"));
        assertThat(platform).anySatisfy(c -> assertThat(c).startsWith("platform.session_expiry", "expira", "bug"));
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[platform.login_locked] A_PLAT_043_ContratoBugsJsonValidoYConsistenteConLaTabla")
    @SuppressWarnings("unchecked")
    void A_PLAT_043_ContratoBugsJsonValidoYConsistenteConLaTabla() {
        var h = new HomeDocsPage(driver).openBugsTable();
        int filas = h.bugRows().size();
        Map<String, Object> r = h.fetchJson("/bugs.json");
        assertThat(((Number) r.get("status")).intValue()).isEqualTo(200);
        Map<String, Object> json = (Map<String, Object>) r.get("json");
        assertThat(json).isNotNull().containsKeys("site", "version", "generatedAt", "login", "personas", "bugs");
        assertThat(json.get("site")).isEqualTo("MoreWater qalab");
        assertThat(json.get("version")).isEqualTo("1");
        Map<String, Object> login = (Map<String, Object>) json.get("login");
        assertThat(login).containsEntry("page", "/id/").containsEntry("password", "qalab123");
        List<Map<String, Object>> bugs = (List<Map<String, Object>>) json.get("bugs");
        assertThat(bugs).hasSize(filas);
        assertThat((List<?>) json.get("personas")).hasSize(8);
        var locked = bugs.stream().filter(b -> "platform.login_locked".equals(b.get("id"))).findFirst().orElseThrow();
        assertThat(locked).containsEntry("persona", "bloqueado").containsEntry("kind", "diseño").containsEntry("severity", "low");
        var expiry = bugs.stream().filter(b -> "platform.session_expiry".equals(b.get("id"))).findFirst().orElseThrow();
        assertThat(expiry).containsEntry("persona", "expira").containsEntry("kind", "bug").containsEntry("severity", "high");
        assertThat(((Number) expiry.get("level")).intValue()).isEqualTo(3);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_PLAT_044_CadaPersonaDeBugsJsonReferenciaIdsExistentes")
    @SuppressWarnings("unchecked")
    void A_PLAT_044_CadaPersonaDeBugsJsonReferenciaIdsExistentes() {
        var h = new HomeDocsPage(driver).openHome();
        Map<String, Object> json = (Map<String, Object>) h.fetchJson("/bugs.json").get("json");
        Set<Object> ids = ((List<Map<String, Object>>) json.get("bugs")).stream().map(b -> b.get("id")).collect(Collectors.toSet());
        List<Map<String, Object>> personas = (List<Map<String, Object>>) json.get("personas");
        for (var p : personas) {
            assertThat(ids).as("ids de " + p.get("id")).containsAll((List<Object>) p.get("bugs"));
        }
        var porId = personas.stream().collect(Collectors.toMap(p -> (String) p.get("id"), p -> (List<Object>) p.get("bugs")));
        assertThat(porId.get("bloqueado")).containsExactly("platform.login_locked");
        assertThat(porId.get("expira")).contains("platform.session_expiry");
    }
}
