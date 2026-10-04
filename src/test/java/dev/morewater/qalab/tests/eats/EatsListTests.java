package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.EatsPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-EATS lista de restaurantes: búsqueda, chips, restaurante cerrado y apertura del menú. */
class EatsListTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_001_ListaCompletaDeRestaurantesConSusDatos")
    void A_EATS_001_ListaCompletaDeRestaurantesConSusDatos() {
        EatsPage eats = new EatsPage(driver).openListReady();
        assertThat(eats.count()).isEqualTo("8 restaurantes");
        assertThat(eats.restNames()).containsExactly("Taquería El Cóndor", "Pizza Nimbo", "Sushi Aurora", "Burger Jacaranda",
                "Cocina Marlín", "Verde Vivo", "Dulce Copal", "Cafetería Sirena");
        assertThat(eats.restMeta(0)).isEqualTo("25 min · envío $29.00");
        assertThat(eats.restMeta(3)).isEqualTo("30 min · envío gratis");
        assertThat(eats.restCard(0)).contains("Tacos · ★ 4.7");
        for (int i = 0; i < 7; i++) {
            assertThat(eats.restStatus(i)).isEqualTo("Abierto");
            assertThat(eats.openButton(i).getText()).isEqualTo("Ver menú");
        }
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_002_FiltrarPorUnTipoDeComidaConChip")
    void A_EATS_002_FiltrarPorUnTipoDeComidaConChip() {
        EatsPage eats = new EatsPage(driver).openListReady();
        assertThat(eats.count()).isEqualTo("8 restaurantes");
        eats.chip("pizza");
        assertThat(eats.chipPressed("pizza")).isTrue();
        assertThat(eats.count()).isEqualTo("1 restaurante");
        assertThat(eats.restName(0)).isEqualTo("Pizza Nimbo");
        eats.chip("pizza");
        assertThat(eats.chipPressed("pizza")).isFalse();
        assertThat(eats.count()).isEqualTo("8 restaurantes");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_003_CombinarVariosChipsDeTipoDeComida")
    void A_EATS_003_CombinarVariosChipsDeTipoDeComida() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.chip("tacos");
        eats.chip("sushi");
        assertThat(eats.count()).isEqualTo("2 restaurantes");
        assertThat(eats.restName(0)).isEqualTo("Taquería El Cóndor");
        assertThat(eats.restName(1)).isEqualTo("Sushi Aurora");
        eats.chip("tacos");
        assertThat(eats.count()).isEqualTo("1 restaurante");
        assertThat(eats.restNames()).isEqualTo(List.of("Sushi Aurora"));
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_004_BusquedaPorNombreSinDistinguirMayusculasNiEspacios")
    void A_EATS_004_BusquedaPorNombreSinDistinguirMayusculasNiEspacios() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.search("  NIMBO  ");
        assertThat(eats.count()).isEqualTo("1 restaurante");
        assertThat(eats.restName(0)).isEqualTo("Pizza Nimbo");
        eats.search("pizza");
        assertThat(eats.count()).isEqualTo("1 restaurante");
        assertThat(eats.restName(0)).isEqualTo("Pizza Nimbo");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_006_RestauranteCerradoNoPermitePedir")
    void A_EATS_006_RestauranteCerradoNoPermitePedir() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.search("sirena");
        assertThat(eats.count()).isEqualTo("1 restaurante");
        assertThat(eats.restCard(0)).contains("Cafetería Sirena").contains("Café · ★ 4.3");
        assertThat(eats.restMeta(0)).isEqualTo("15 min · envío $15.00");
        assertThat(eats.restStatus(0)).isEqualTo("Cerrado");
        assertThat(eats.openButton(0).isEnabled()).isFalse();
        assertThat(eats.openButton(0).getText()).isEqualTo("No disponible");
        assertThat(eats.openButton(0).getTagName()).isEqualTo("button");
        String url = driver.getCurrentUrl();
        try { eats.openButton(0).click(); } catch (RuntimeException ignored) { /* deshabilitado: no debe pasar nada */ }
        assertThat(driver.getCurrentUrl()).isEqualTo(url);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_007_AbrirElMenuDesdeLaLista")
    void A_EATS_007_AbrirElMenuDesdeLaLista() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.openButton(1).click();
        eats.waitMenu();
        assertThat(driver.getCurrentUrl()).contains("/eats/restaurant/?id=r2");
        assertThat(eats.menuTitle()).isEqualTo("Pizza Nimbo");
        eats.back();
        eats.waitListReady();
        assertThat(eats.count()).isEqualTo("8 restaurantes");
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.visual_open_badge] A_EATS_009_PersonaVisualRestauranteCerradoEtiquetadoAbierto")
    void A_EATS_009_PersonaVisualRestauranteCerradoEtiquetadoAbierto() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.search("sirena");
        assertThat(eats.restStatus(0)).as("etiqueta del restaurante cerrado").isEqualTo("Cerrado");
        assertThat(eats.openButton(0).isEnabled()).isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[eats.flaky_list] A_EATS_010_PersonaIntermitenteLaListaFallaCon503YReintentar")
    void A_EATS_010_PersonaIntermitenteLaListaFallaCon503YReintentar() {
        EatsPage eats = new EatsPage(driver);
        for (int i = 0; i < 12; i++) {
            eats.openList();
            eats.waitListOutcome();
            assertThat(eats.listFailed()).as("la lista falló en la carga " + (i + 1)).isFalse();
            assertThat(eats.count()).isEqualTo("8 restaurantes");
        }
    }
}
