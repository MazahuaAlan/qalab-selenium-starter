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
    @DisplayName("Mover una tarjeta con el selector «Mover a» (CP-DESK-011)")
    void moveWithSelector() {
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
    @DisplayName("Mover una tarjeta con arrastrar y soltar (HTML5) (CP-DESK-012)")
    void moveWithDragAndDrop() {
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
    @DisplayName("Alternativa por teclado al arrastre (CP-DESK-013)")
    void keyboardAlternativeToDrag() {
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
    @DisplayName("Mover tarjeta de Hecho a Nuevo (cualquier salto de estado) (CP-DESK-015)")
    void moveBackwardsAcrossColumns() {
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
    @DisplayName("Mover a «En progreso» con 2 tickets es permitido y llega a 3 (CP-DESK-018)")
    void moveIntoProgressBelowLimitIsAllowed() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.count("progreso")).isEqualTo("2");
        assertThat(board.move("DK-1", "progreso")).isEqualTo("DK-1 movido a En progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Cuarto ticket en «En progreso» es rechazado por límite WIP (selector) (CP-DESK-019)")
    void fourthTicketInProgressIsRejected() {
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
    @DisplayName("Liberar espacio en «En progreso» permite volver a mover (CP-DESK-021)")
    void freeingProgressSpaceAllowsNewMove() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.move("DK-1", "progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
        assertThat(board.move("DK-3", "revision")).isEqualTo("DK-3 movido a En revisión");
        assertThat(board.count("progreso")).isEqualTo("2");
        assertThat(board.move("DK-2", "progreso")).isEqualTo("DK-2 movido a En progreso");
        assertThat(board.count("progreso")).isEqualTo("3");
    }
}
