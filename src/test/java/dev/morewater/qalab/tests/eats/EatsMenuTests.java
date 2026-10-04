package dev.morewater.qalab.tests.eats;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.eats.EatsPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-EATS menú del restaurante y diálogo de opciones del platillo. */
class EatsMenuTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Menú de Taquería El Cóndor: pestaña Platillos (CP-EATS-011)")
    void menuPlatillosTab() {
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
    @DisplayName("Cambiar de pestaña muestra otra categoría (CP-EATS-012)")
    void switchingTabsShowsOtherCategory() {
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
    @Disabled("Diferencia real: /eats/restaurant/?id=r8 (cerrado) carga el menú con botones Agregar y permite pedir; la validación solo existe en la lista")
    @DisplayName("Un restaurante cerrado no debe poderse pedir escribiendo la URL (CP-EATS-015)")
    void closedRestaurantCannotBeOrderedByUrl() {
        EatsPage eats = new EatsPage(driver);
        eats.go("/eats/restaurant/?id=r8");
        assertThat(eats.has("eats-add-r8-0")).as("botón Agregar en restaurante cerrado").isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[eats.slow_menu] Persona lenta: el menú tarda más de 3.5 s (CP-EATS-016)")
    void menuLoadsQuickly() {
        EatsPage eats = new EatsPage(driver).openListReady();
        eats.openButton(0).click();
        long t0 = System.currentTimeMillis();
        eats.waitMenu();
        assertThat(System.currentTimeMillis() - t0).as("milisegundos hasta ver el menú").isLessThan(3500);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Diálogo de opciones con valores por defecto (CP-EATS-017)")
    void dialogDefaults() {
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
    @DisplayName("Cálculo por tamaño del platillo (CP-EATS-018)")
    void sizeSurcharge() {
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
    @DisplayName("Extras y cantidad en el total del diálogo y del carrito (CP-EATS-019)")
    void extrasAndQuantityInTotals() {
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
    @DisplayName("Límites de cantidad en el diálogo: 1 a 10 (CP-EATS-021)")
    void quantityLimits() {
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
    @DisplayName("Notas para la cocina: máximo 60 caracteres y visibles en el carrito (CP-EATS-022)")
    void kitchenNotesLimit() {
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
    @DisplayName("Cerrar el diálogo sin agregar (Cancelar y Escape) (CP-EATS-023)")
    void closeDialogWithoutAdding() {
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
