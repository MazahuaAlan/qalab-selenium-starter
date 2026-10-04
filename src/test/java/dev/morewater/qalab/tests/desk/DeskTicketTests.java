package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import dev.morewater.qalab.pages.desk.DeskTicketPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-DESK-08 (ficha y edición) y HU-DESK-12 (eliminación por el administrador). */
class DeskTicketTests extends BaseTest {
    private static final String NEW_TITLE = "Nuevo título muy cuidadosamente redactado";

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_047_FichaDeUnTicketConTodosSusDatos")
    void A_DESK_047_FichaDeUnTicketConTodosSusDatos() {
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-4");
        assertThat(ticket.id()).isEqualTo("DK-4");
        assertThat(ticket.title()).isEqualTo("Error 500 al exportar CSV grande");
        assertThat(ticket.status()).isEqualTo("En progreso");
        assertThat(ticket.assignee()).isEqualTo("Carla");
        assertThat(ticket.priority()).isEqualTo("alta");
        assertThat(ticket.labelChecked("backend")).isTrue();
        assertThat(ticket.labelChecked("urgente")).isTrue();
        assertThat(ticket.labelChecked("frontend")).isFalse();
        assertThat(ticket.dueLine()).contains("2026-09-21");
        assertThat(ticket.description()).isEqualTo("Error 500 al exportar CSV grande. Detalle pendiente de completar.");
        assertThat(ticket.log()).contains("Ticket creado");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_049_EditarElTituloCorrectamenteConEnter")
    void A_DESK_049_EditarElTituloCorrectamenteConEnter() {
        String title = "Filtro por rango de fechas en movimientos";
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-3");
        ticket.startTitleEdit();
        ticket.typeNewTitle(title);
        ticket.pressEnterInTitle();
        ticket.waitTitle(title);
        assertThat(ticket.message()).isEqualTo("Título actualizado.");
        assertThat(ticket.logFirst()).isEqualTo("Título actualizado");
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.title("DK-3")).isEqualTo(title);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_052_EditarUnTituloTardandoMasDe20SSeGuardaComportamientoCorrecto")
    void A_DESK_052_EditarUnTituloTardandoMasDe20SSeGuardaComportamientoCorrecto() {
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-3");
        ticket.startTitleEdit();
        ticket.typeNewTitle(NEW_TITLE);
        ticket.advanceClock(25_000); // equivale a esperar 25 s sin interactuar
        assertThat(ticket.titleEditorOpen()).isTrue();
        ticket.saveTitleButton();
        ticket.waitTitle(NEW_TITLE);
        assertThat(ticket.hasTitleError()).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_053_CambiarEstadoResponsablePrioridadYEtiquetaDesdeLaFicha")
    void A_DESK_053_CambiarEstadoResponsablePrioridadYEtiquetaDesdeLaFicha() {
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-3");
        ticket.changeStatus("revision");
        ticket.waitLogFirst("Estado: En revisión");
        assertThat(ticket.status()).isEqualTo("En revisión");
        ticket.changeAssignee("Carla");
        ticket.waitLogFirst("Asignado a Carla");
        ticket.changePriority("baja");
        ticket.waitLogFirst("Prioridad: baja");
        assertThat(ticket.priorityBadgeText()).isEqualTo("Baja");
        assertThat(ticket.priorityBadgeColor()).isEqualTo("rgb(0, 121, 76)");
        ticket.toggleLabel("pruebas");
        ticket.waitLogFirst("Etiqueta pruebas");

        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.hasCard("revision", "DK-3")).isTrue();
        assertThat(board.detail("DK-3")).isEqualTo("Carla · mejora");
        assertThat(board.badgeText(board.card("DK-3"))).isEqualTo("Baja");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_076_ElAdministradorEliminaUnTicketConConfirmacion")
    void A_DESK_076_ElAdministradorEliminaUnTicketConConfirmacion() {
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-1");
        ticket.setRole("admin");
        ticket.clickDelete();
        assertThat(ticket.confirmText()).contains("¿Eliminar DK-1? No se puede deshacer.");
        ticket.confirmNo();
        assertThat(ticket.confirmShown()).isFalse();
        assertThat(ticket.id()).isEqualTo("DK-1");
        ticket.clickDelete();
        ticket.confirmYes();
        DeskBoardPage board = new DeskBoardPage(driver);
        board.waitForBoard();
        assertThat(board.cardExists("DK-1")).isFalse();
        ticket.open("DK-1");
        ticket.waitMissing();
        assertThat(ticket.missingShown()).isTrue();
    }
}
