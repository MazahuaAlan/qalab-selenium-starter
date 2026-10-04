package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

/** HU-DESK-01: tablero Kanban (estructura, tarjetas, insignia en Shadow DOM, persistencia). */
class DeskBoardTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_001_EstructuraInicialDelTableroConCuatroColumnasYContadores")
    void A_DESK_001_EstructuraInicialDelTableroConCuatroColumnasYContadores() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        for (String col : DeskBoardPage.COLUMNS) assertThat(board.count(col)).as("contador de " + col).isEqualTo("2");
        assertThat(board.cardIds("nuevo")).containsExactlyInAnyOrder("DK-1", "DK-2");
        assertThat(board.cardIds("progreso")).containsExactlyInAnyOrder("DK-3", "DK-4");
        assertThat(board.cardIds("revision")).containsExactlyInAnyOrder("DK-5", "DK-6");
        assertThat(board.cardIds("hecho")).containsExactlyInAnyOrder("DK-7", "DK-8");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_002_ContenidoDeUnaTarjetaDelTablero")
    void A_DESK_002_ContenidoDeUnaTarjetaDelTablero() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.openLink("DK-1").getText().trim()).isEqualTo("DK-1");
        assertThat(board.openLink("DK-1").getAttribute("href")).contains("/desk/ticket/?id=DK-1");
        assertThat(board.title("DK-1")).isEqualTo("El botón de pago no responde en móvil");
        assertThat(board.detail("DK-1")).isEqualTo("Ana · bug");
        assertThat(board.badgeText(board.card("DK-1"))).isEqualTo("Alta");
        assertThat(board.moveSelected("DK-1")).isEqualTo("Nuevo");
        assertThat(board.moveOptions("DK-1")).containsExactly("Nuevo", "En progreso", "En revisión", "Hecho");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_003_InsigniaDePrioridadDentroDeShadowDOMConColoresCorrectos")
    void A_DESK_003_InsigniaDePrioridadDentroDeShadowDOMConColoresCorrectos() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        WebElement high = board.card("DK-1");
        assertThat(board.badgeText(high)).isEqualTo("Alta");
        assertThat(board.badgeColor(high)).isEqualTo("rgb(192, 31, 47)");
        assertThat(board.badgeIn(high).getAttribute("aria-label")).isEqualTo("Prioridad alta");
        assertThat(board.badgeIn(high).getAttribute("role")).isEqualTo("img");
        WebElement medium = board.card("DK-3");
        assertThat(board.badgeText(medium)).isEqualTo("Media");
        assertThat(board.badgeColor(medium)).isEqualTo("rgb(242, 183, 5)");
        WebElement low = board.card("DK-2");
        assertThat(board.badgeText(low)).isEqualTo("Baja");
        assertThat(board.badgeColor(low)).isEqualTo("rgb(0, 121, 76)");
        assertThat(board.badgeVisibleWithoutShadow(high)).as("sin atravesar el shadow no se encuentra").isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_005_PersistenciaDelTableroAlRecargarLaPagina")
    void A_DESK_005_PersistenciaDelTableroAlRecargarLaPagina() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.move("DK-1", "progreso")).isEqualTo("DK-1 movido a En progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
        board.reload();
        assertThat(board.hasCard("progreso", "DK-1")).isTrue();
        assertThat(board.count("progreso")).isEqualTo("3");
        assertThat(List.of("nuevo", "revision", "hecho")).noneMatch(c -> board.hasCard(c, "DK-1"));
    }
}
