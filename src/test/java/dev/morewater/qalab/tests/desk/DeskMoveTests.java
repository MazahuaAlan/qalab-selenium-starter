package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import dev.morewater.qalab.pages.desk.DeskTicketPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-DESK-03 y HU-DESK-04: mover tarjetas (selector, arrastre, teclado) y límite WIP de «En progreso». */
class DeskMoveTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_011_MoverUnaTarjetaConElSelectorMoverA")
    void A_DESK_011_MoverUnaTarjetaConElSelectorMoverA() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        int nuevoBefore = board.countValue("nuevo");
        int progresoBefore = board.countValue("progreso");
        assertThat(board.move("DK-1", "progreso")).isEqualTo("DK-1 movido a En progreso");
        assertThat(board.toastIsError()).isFalse();
        assertThat(board.hasCard("progreso", "DK-1")).isTrue();
        assertThat(board.countValue("nuevo")).isEqualTo(nuevoBefore - 1);
        assertThat(board.countValue("progreso")).isEqualTo(progresoBefore + 1);
        board.openTicket("DK-1");
        DeskTicketPage ticket = new DeskTicketPage(driver);
        assertThat(ticket.status()).isEqualTo("En progreso");
        assertThat(ticket.logFirst()).isEqualTo("Movido a En progreso");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_012_MoverUnaTarjetaConArrastrarYSoltarHTML5")
    void A_DESK_012_MoverUnaTarjetaConArrastrarYSoltarHTML5() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.cardDraggable("DK-2")).isTrue();
        int revisionBefore = board.countValue("revision");
        int nuevoBefore = board.countValue("nuevo");
        board.dragCardOver("DK-2", "revision");
        assertThat(board.columnHighlighted("revision")).as("la columna se resalta al sobrevolar").isTrue();
        assertThat(board.dropOn("revision")).isEqualTo("DK-2 movido a En revisión");
        assertThat(board.hasCard("revision", "DK-2")).isTrue();
        assertThat(board.countValue("revision")).isEqualTo(revisionBefore + 1);
        assertThat(board.countValue("nuevo")).isEqualTo(nuevoBefore - 1);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_013_AlternativaPorTecladoAlArrastre")
    void A_DESK_013_AlternativaPorTecladoAlArrastre() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        int hechoBefore = board.countValue("hecho");
        String previous = board.currentToast();
        assertThat(board.focusAndType("DK-5", "H")).as("el selector recibe el foco").isTrue();
        assertThat(board.awaitNewToast(previous)).isEqualTo("DK-5 movido a Hecho");
        assertThat(board.hasCard("hecho", "DK-5")).isTrue();
        assertThat(board.countValue("hecho")).isEqualTo(hechoBefore + 1);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_015_MoverTarjetaDeHechoANuevoCualquierSaltoDeEstado")
    void A_DESK_015_MoverTarjetaDeHechoANuevoCualquierSaltoDeEstado() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        int nuevoBefore = board.countValue("nuevo");
        int hechoBefore = board.countValue("hecho");
        assertThat(board.move("DK-7", "nuevo")).isEqualTo("DK-7 movido a Nuevo");
        assertThat(board.hasCard("nuevo", "DK-7")).isTrue();
        assertThat(board.countValue("hecho")).isEqualTo(hechoBefore - 1);
        assertThat(board.countValue("nuevo")).isEqualTo(nuevoBefore + 1);
        assertThat(board.move("DK-7", "hecho")).isEqualTo("DK-7 movido a Hecho");
        assertThat(board.hasCard("hecho", "DK-7")).isTrue();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_018_MoverAEnProgresoCon2TicketsEsPermitidoYLlegaA3")
    void A_DESK_018_MoverAEnProgresoCon2TicketsEsPermitidoYLlegaA3() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.count("progreso")).isEqualTo("2");
        assertThat(board.move("DK-1", "progreso")).isEqualTo("DK-1 movido a En progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_019_CuartoTicketEnEnProgresoEsRechazadoPorLimiteWIPSelector")
    void A_DESK_019_CuartoTicketEnEnProgresoEsRechazadoPorLimiteWIPSelector() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.move("DK-1", "progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
        String toast = board.move("DK-2", "progreso");
        assertThat(toast).isEqualTo("Límite WIP de «En progreso» alcanzado (3).");
        assertThat(board.toastIsError()).isTrue();
        assertThat(board.hasCard("nuevo", "DK-2")).isTrue();
        assertThat(board.count("progreso")).isEqualTo("3");
        board.reload();
        assertThat(board.hasCard("nuevo", "DK-2")).isTrue();
        assertThat(board.cardIds("progreso")).containsExactlyInAnyOrder("DK-1", "DK-3", "DK-4");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_DESK_021_LiberarEspacioEnEnProgresoPermiteVolverAMover")
    void A_DESK_021_LiberarEspacioEnEnProgresoPermiteVolverAMover() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.move("DK-1", "progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
        assertThat(board.move("DK-3", "revision")).isEqualTo("DK-3 movido a En revisión");
        assertThat(board.count("progreso")).isEqualTo("2");
        assertThat(board.move("DK-2", "progreso")).isEqualTo("DK-2 movido a En progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
    }
}
