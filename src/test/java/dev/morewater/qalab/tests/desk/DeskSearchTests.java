package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-DESK-02: búsqueda y filtros del tablero. */
class DeskSearchTests extends BaseTest {

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_006_BuscarPorTextoDelTitulo")
    void A_DESK_006_BuscarPorTextoDelTitulo() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.search("csv");
        board.waitCardCount(1);
        assertThat(board.cardExists("DK-4")).isTrue();
        assertThat(board.cardExists("DK-1")).isFalse();
        assertThat(board.count("progreso")).isEqualTo("1");
        for (String col : new String[] {"nuevo", "revision", "hecho"}) {
            assertThat(board.count(col)).as("contador de " + col).isEqualTo("0");
            assertThat(board.emptyText(col)).isEqualTo("Sin tickets.");
        }
    }

    @Test
    @Tag("opcional")
    @DisplayName("A_DESK_008_BusquedaSinResultados")
    void A_DESK_008_BusquedaSinResultados() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.search("zzzxxx");
        board.waitCardCount(0);
        for (String col : DeskBoardPage.COLUMNS) {
            assertThat(board.emptyText(col)).as("vacío en " + col).isEqualTo("Sin tickets.");
            assertThat(board.count(col)).as("contador de " + col).isEqualTo("0");
        }
        board.clearSearch();
        board.waitCardCount(8);
        for (String col : DeskBoardPage.COLUMNS) assertThat(board.count(col)).isEqualTo("2");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_009_FiltrosCombinadosPorResponsableYPrioridad")
    void A_DESK_009_FiltrosCombinadosPorResponsableYPrioridad() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.filterAssignee("Ana");
        board.waitCardCount(2);
        assertThat(board.cardExists("DK-1")).isTrue();
        assertThat(board.cardExists("DK-6")).isTrue();
        board.filterPriority("alta");
        board.waitCardCount(1);
        assertThat(board.cardExists("DK-1")).isTrue();
        board.search("pago");
        board.waitCardCount(1);
        assertThat(board.cardExists("DK-1")).isTrue();
        board.search("pruebas");
        board.waitCardCount(0);
    }
}
