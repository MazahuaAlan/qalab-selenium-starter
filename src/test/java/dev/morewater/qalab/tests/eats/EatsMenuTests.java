package dev.morewater.qalab.tests.eats;

import dev.morewater.qalab.core.Evidence;
import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-EATS menú del restaurante y diálogo de opciones del platillo. */
class EatsMenuTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_011_MenuDeTaqueriaElCondorPestanaPlatillos")
    void A_EATS_011_MenuDeTaqueriaElCondorPestanaPlatillos() {
        EatsPage eats = new EatsPage(driver);
        eats.go("/eats/restaurant/?id=r1");
        eats.waitMenu();
        assertThat(eats.tabLabel(0)).isEqualTo("Platillos");
        assertThat(eats.tabLabel(1)).isEqualTo("Complementos");
        assertThat(eats.tabLabel(2)).isEqualTo("Bebidas y postres");
        assertThat(eats.tabSelected(0)).isTrue();
        assertThat(eats.visibleItemIds()).containsExactly("r1-0", "r1-1", "r1-2");
        assertThat(eats.itemPrice("r1-0")).matches("\\$\\d{1,3}(,\\d{3})*\\.\\d{2}");
        assertThat(eats.itemCard("r1-0")).contains("Tacos al pastor").contains("Con piña y cilantro");
        assertThat(eats.itemCard("r1-1")).contains("Tacos de suadero").contains("Tortilla de maíz");
        assertThat(eats.itemCard("r1-2")).contains("Gringa").contains("Pastor con queso");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_012_CambiarDePestanaMuestraOtraCategoria")
    void A_EATS_012_CambiarDePestanaMuestraOtraCategoria() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.tab(1);
        assertThat(eats.tabSelected(1)).isTrue();
        assertThat(eats.visibleItemIds()).containsExactly("r1-3", "r1-4", "r1-5");
        assertThat(eats.itemShown("r1-0")).isFalse();
        assertThat(eats.itemCard("r1-3")).contains("Quesadilla");
        eats.tab(2);
        assertThat(eats.visibleItemIds()).containsExactly("r1-6", "r1-7", "r1-8");
        assertThat(eats.itemCard("r1-7")).contains("Flan");
        eats.tab(0);
        assertThat(eats.visibleItemIds()).containsExactly("r1-0", "r1-1", "r1-2");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_015_UnRestauranteCerradoNoDebePodersePedirEscribiendoLaURL")
    void A_EATS_015_UnRestauranteCerradoNoDebePodersePedirEscribiendoLaURL() {
        EatsPage eats = new EatsPage(driver);
        eats.go("/eats/restaurant/?id=r8");
        assertThat(eats.has("eats-add-r8-0")).as("botón Agregar en restaurante cerrado").isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.slow_menu] A_EATS_016_PersonaLentaElMenuTardaMasDe35S")
    void A_EATS_016_PersonaLentaElMenuTardaMasDe35S() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.openButton(0).click();
        Evidence.pause();
        long t0 = System.currentTimeMillis();
        eats.waitMenu();
        Evidence.resume();
        assertThat(System.currentTimeMillis() - t0).as("milisegundos hasta ver el menú").isLessThan(3500);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_017_DialogoDeOpcionesConValoresPorDefecto")
    void A_EATS_017_DialogoDeOpcionesConValoresPorDefecto() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.openDialog("r1-0");
        assertThat(eats.dialogTitle()).isEqualTo("Tacos al pastor");
        assertThat(eats.sizeChecked("ch")).isTrue();
        assertThat(eats.sizeChecked("md")).isFalse();
        assertThat(eats.sizeChecked("gd")).isFalse();
        assertThat(eats.sizeLabel("md")).contains("Mediano").contains("+$25.00");
        assertThat(eats.sizeLabel("gd")).contains("Grande").contains("+$50.00");
        assertThat(eats.extraLabel("queso")).contains("Queso extra").contains("+$15.00");
        assertThat(eats.extraLabel("guac")).contains("Guacamole").contains("+$25.00");
        assertThat(eats.extraLabel("salsa")).contains("Salsa de la casa");
        assertThat(eats.extraChecked("queso") || eats.extraChecked("guac") || eats.extraChecked("salsa")).isFalse();
        assertThat(eats.dialogQty()).isEqualTo("1");
        assertThat(eats.dialogTotalCents()).isEqualTo(base);
        assertThat(eats.addButtonText()).contains("Agregar").contains(eats.dialogTotal());
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_018_CalculoPorTamanoDelPlatillo")
    void A_EATS_018_CalculoPorTamanoDelPlatillo() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.openDialog("r1-0");
        assertThat(eats.dialogTotalCents()).isEqualTo(base);
        eats.size("md");
        assertThat(eats.dialogTotalCents()).isEqualTo(base + 2500);
        eats.size("gd");
        assertThat(eats.dialogTotalCents()).isEqualTo(base + 5000);
        eats.size("ch");
        assertThat(eats.dialogTotalCents()).isEqualTo(base);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_EATS_019_ExtrasYCantidadEnElTotalDelDialogoYDelCarrito")
    void A_EATS_019_ExtrasYCantidadEnElTotalDelDialogoYDelCarrito() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long unit = eats.itemPriceCents("r1-0") + 5000 + 1500 + 2500;
        eats.openDialog("r1-0");
        eats.size("gd");
        eats.extra("queso");
        eats.extra("guac");
        assertThat(eats.dialogTotalCents()).isEqualTo(unit);
        eats.plus();
        eats.plus();
        assertThat(eats.dialogQty()).isEqualTo("3");
        assertThat(eats.dialogTotalCents()).isEqualTo(unit * 3);
        eats.confirmAdd();
        assertThat(EatsPage.cents(eats.lineTotal("r1-0"))).isEqualTo(unit * 3);
        assertThat(eats.lineText("r1-0")).contains("Grande").contains("Queso extra").contains("Guacamole");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_021_LimitesDeCantidadEnElDialogo1A10")
    void A_EATS_021_LimitesDeCantidadEnElDialogo1A10() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        long base = eats.itemPriceCents("r1-0");
        eats.openDialog("r1-0");
        assertThat(eats.minusEnabled()).isFalse();
        for (int i = 0; i < 9; i++) eats.plus();
        assertThat(eats.dialogQty()).isEqualTo("10");
        assertThat(eats.dialogTotalCents()).isEqualTo(base * 10);
        assertThat(eats.plusEnabled()).isFalse();
        eats.minus();
        assertThat(eats.dialogQty()).isEqualTo("9");
        assertThat(eats.dialogTotalCents()).isEqualTo(base * 9);
        assertThat(eats.plusEnabled()).isTrue();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_022_NotasParaLaCocinaMaximo60CaracteresYVisiblesEnElCarrito")
    void A_EATS_022_NotasParaLaCocinaMaximo60CaracteresYVisiblesEnElCarrito() {
        String note = "Sin cebolla, con mucha salsa verde y las tortillas bien calientitas por favor";
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.openDialog("r1-0");
        eats.notes(note);
        assertThat(eats.notesValue()).hasSize(60).isEqualTo(note.substring(0, 60));
        eats.confirmAdd();
        assertThat(eats.lineText("r1-0")).contains("Chico").contains("«" + note.substring(0, 60) + "»");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_EATS_023_CerrarElDialogoSinAgregarCancelarYEscape")
    void A_EATS_023_CerrarElDialogoSinAgregarCancelarYEscape() {
        EatsPage eats = new EatsPage(driver).openMenu("r1");
        eats.openDialog("r1-0");
        eats.cancelDialog();
        assertThat(eats.cartEmpty()).isTrue();
        assertThat(eats.navCart()).contains("Carrito (0)");
        eats.openDialog("r1-0");
        eats.size("gd");
        eats.pressEscape();
        eats.waitDialogClosed();
        assertThat(eats.cartEmpty()).isTrue();
        eats.openDialog("r1-0");
        assertThat(eats.sizeChecked("ch")).isTrue();
        assertThat(eats.extraChecked("queso")).isFalse();
        assertThat(eats.dialogQty()).isEqualTo("1");
    }
}
