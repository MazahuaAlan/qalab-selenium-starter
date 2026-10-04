package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskListPage;
import dev.morewater.qalab.pages.desk.DeskNewPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-DESK-10 (vista de lista, vencidos) y HU-DESK-11 (acciones en lote). */
class DeskListTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_059_VistaDeListaConTodasLasFilasYOrdenInicialPorId")
    void A_DESK_059_VistaDeListaConTodasLasFilasYOrdenInicialPorId() {
        DeskListPage list = new DeskListPage(driver).open();
        assertThat(list.rowIds()).containsExactly("DK-1", "DK-2", "DK-3", "DK-4", "DK-5", "DK-6", "DK-7", "DK-8");
        assertThat(list.sortAria("id")).isEqualTo("ascending");
        assertThat(list.sortHeaderText("id")).contains("▲");
        assertThat(list.status("DK-4")).isEqualTo("En progreso");
        assertThat(list.due("DK-4")).isEqualTo("2026-09-21");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_063_MarcaDeVencidoSoloSiLaFechaEsAnteriorAHoyYNoEstaHecho")
    void A_DESK_063_MarcaDeVencidoSoloSiLaFechaEsAnteriorAHoyYNoEstaHecho() {
        DeskNewPage form = new DeskNewPage(driver);
        String today = form.open().today();
        String id = form.createTicket("Ticket que vence hoy mismo", DeskNewPage.DEFAULT_DESC, today);
        DeskListPage list = new DeskListPage(driver).open();
        assertThat(list.due(id)).isEqualTo(today);
        for (String row : list.rowIds()) {
            boolean expected = !"Hecho".equals(list.status(row)) && list.due(row).compareTo(today) < 0;
            assertThat(list.overdue(row)).as("«Vencido» en %s (vence %s, estado %s, hoy %s)", row, list.due(row), list.status(row), today).isEqualTo(expected);
        }
        assertThat(list.overdue(id)).as("vence hoy: aún no está vencido").isFalse();
        assertThat(list.overdue("DK-7")).as("Hecho no se marca vencido").isFalse();
        assertThat(list.overdue("DK-8")).as("Hecho no se marca vencido").isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_066_CambiarTresTicketsAHechoEnLote")
    void A_DESK_066_CambiarTresTicketsAHechoEnLote() {
        DeskListPage list = new DeskListPage(driver).open();
        list.check("DK-1");
        list.check("DK-2");
        list.check("DK-3");
        assertThat(list.selectedText()).isEqualTo("3 seleccionados");
        list.bulkTo("hecho");
        list.applyBulk();
        assertThat(list.bulkMessage()).isEqualTo("3 tickets seleccionados: estado «Hecho» aplicado.");
        for (String id : new String[] {"DK-1", "DK-2", "DK-3"}) assertThat(list.status(id)).as("estado de " + id).isEqualTo("Hecho");
        assertThat(list.selectedText()).isEqualTo("0 seleccionados");
    }
}
