package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import dev.morewater.qalab.pages.desk.DeskListPage;
import dev.morewater.qalab.pages.desk.DeskNewPage;
import dev.morewater.qalab.pages.desk.DeskTicketPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** HU-DESK-05 (crear ticket y validaciones) y HU-DESK-06 (editor de texto enriquecido). */
class DeskCreateTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Crear ticket válido con valores por defecto (CP-DESK-023)")
    void createTicketWithDefaults() {
        DeskNewPage form = new DeskNewPage(driver).open();
        String expectedId = "DK-" + (form.ticketCounter() + 1);
        form.title("Revisar el cálculo de impuestos").describe(DeskNewPage.DEFAULT_DESC);
        assertThat(form.editorCount()).isEqualTo(DeskNewPage.DEFAULT_DESC.length() + "/600");
        form.submit();
        DeskTicketPage ticket = new DeskTicketPage(driver);
        assertThat(form.awaitCreated()).isEqualTo(expectedId);
        assertThat(ticket.status()).isEqualTo("Nuevo");
        assertThat(ticket.priorityBadgeText()).isEqualTo("Media");
        assertThat(ticket.dueLine()).contains("sin fecha");
        assertThat(ticket.log()).contains("Ticket creado");

        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.hasCard("nuevo", expectedId)).isTrue();
        assertThat(board.detail(expectedId)).isEqualTo("Sin asignar · tarea");
        assertThat(board.bellCount()).isEqualTo(1);
        board.openBell();
        assertThat(board.notification(0)).contains(expectedId + " creado");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Crear ticket con todos los campos informados (CP-DESK-024)")
    void createTicketWithAllFields() {
        DeskNewPage form = new DeskNewPage(driver).open();
        String due = form.todayPlus(7);
        form.title("Falla el login con contraseñas largas").kind("bug").priority("alta").assignee("Diego")
                .label("backend").label("urgente").due(due).describe("Pasos para reproducir el error en producción").submit();
        form.awaitCreated();
        DeskTicketPage ticket = new DeskTicketPage(driver);
        assertThat(ticket.title()).isEqualTo("Falla el login con contraseñas largas");
        assertThat(ticket.priorityBadgeText()).isEqualTo("Alta");
        assertThat(ticket.priorityBadgeColor()).isEqualTo("rgb(192, 31, 47)");
        assertThat(ticket.assignee()).isEqualTo("Diego");
        assertThat(ticket.labelChecked("backend")).isTrue();
        assertThat(ticket.labelChecked("urgente")).isTrue();
        assertThat(ticket.labelChecked("frontend")).isFalse();
        assertThat(ticket.dueLine()).contains(due);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Validación: enviar el formulario vacío (CP-DESK-025)")
    void emptyFormShowsValidationErrors() {
        DeskNewPage form = new DeskNewPage(driver).open();
        long before = form.ticketsInState();
        form.submit();
        assertThat(form.titleError()).isEqualTo("El título debe tener al menos 5 caracteres.");
        assertThat(form.descError()).isEqualTo("Describe el ticket (mínimo 10 caracteres).");
        assertThat(form.hasDueError()).isFalse();
        assertThat(driver.getCurrentUrl()).contains("/desk/new/");
        assertThat(form.ticketsInState()).isEqualTo(before);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Límite del título: 4 caracteres falla, 5 es válido (CP-DESK-026)")
    void titleLengthBoundary() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.describe("Descripción de diez o más");
        form.title("abcd").submit();
        assertThat(form.titleError()).contains("al menos 5 caracteres");
        form.title("    ab   ").submit();
        assertThat(form.titleError()).contains("al menos 5 caracteres");
        form.title("abcde").submit();
        form.awaitCreated();
        assertThat(new DeskTicketPage(driver).title()).isEqualTo("abcde");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Límite de descripción: 9, 10, 600 y 601 caracteres (CP-DESK-027)")
    void descriptionLengthBoundaries() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.title("Título válido");
        form.describeInstantly("a".repeat(9)).submit();
        assertThat(form.editorCount()).isEqualTo("9/600");
        assertThat(form.descError()).isEqualTo("Describe el ticket (mínimo 10 caracteres).");

        form.describeInstantly("a".repeat(601)).submit();
        assertThat(form.editorCount()).isEqualTo("601/600");
        assertThat(form.editorCountIsError()).isTrue();
        assertThat(form.descError()).isEqualTo("La descripción admite hasta 600 caracteres.");

        form.describeInstantly("a".repeat(600)).submit();
        form.awaitCreated();
        assertThat(driver.getCurrentUrl()).contains("/desk/ticket/");

        DeskNewPage second = new DeskNewPage(driver).open();
        second.title("Título válido").describeInstantly("a".repeat(10)).submit();
        second.awaitCreated();
        assertThat(driver.getCurrentUrl()).contains("/desk/ticket/");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Fecha límite: pasada falla; hoy y futura son válidas (CP-DESK-028)")
    void dueDateBoundaries() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.title("Ticket con fecha").describe(DeskNewPage.DEFAULT_DESC);
        form.due(form.todayPlus(-1)).submit();
        assertThat(form.dueError()).isEqualTo("La fecha límite no puede ser pasada.");

        form.due(form.today()).submit();
        form.awaitCreated();
        assertThat(new DeskTicketPage(driver).dueLine()).contains(form.today());

        DeskNewPage next = new DeskNewPage(driver).open();
        String tomorrow = next.todayPlus(1);
        next.title("Ticket con fecha").describe(DeskNewPage.DEFAULT_DESC).due(tomorrow).submit();
        next.awaitCreated();
        assertThat(new DeskTicketPage(driver).dueLine()).contains(tomorrow);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("El ticket se crea con el siguiente id consecutivo (CP-DESK-030)")
    void ticketsGetConsecutiveIds() {
        DeskNewPage form = new DeskNewPage(driver);
        form.open();
        long counter = form.ticketCounter();
        assertThat(form.createTicket("Primer ticket nuevo", DeskNewPage.DEFAULT_DESC)).isEqualTo("DK-" + (counter + 1));
        assertThat(form.createTicket("Segundo ticket nuevo", DeskNewPage.DEFAULT_DESC)).isEqualTo("DK-" + (counter + 2));
        DeskListPage list = new DeskListPage(driver).open();
        assertThat(list.rowIds()).contains("DK-" + (counter + 1), "DK-" + (counter + 2));
        assertThat(list.status("DK-" + (counter + 1))).isEqualTo("Nuevo");
        assertThat(list.status("DK-" + (counter + 2))).isEqualTo("Nuevo");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Ticket creado aparece en tablero y lista con estado Nuevo (CP-DESK-032)")
    void createdTicketAppearsInBoardAndList() {
        DeskNewPage form = new DeskNewPage(driver);
        int nuevoBefore = new DeskBoardPage(driver).open().countValue("nuevo");
        String id = form.createTicket("Integración tablero y lista", DeskNewPage.DEFAULT_DESC);
        DeskBoardPage board = new DeskBoardPage(driver).open();
        assertThat(board.hasCard("nuevo", id)).isTrue();
        assertThat(board.countValue("nuevo")).isEqualTo(nuevoBefore + 1);
        assertThat(new DeskListPage(driver).open().status(id)).isEqualTo("Nuevo");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Aplicar negrita en el editor (CP-DESK-033)")
    void boldInEditor() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.describe("Pasos para reproducir ");
        form.toggleBold();
        form.editor().sendKeys("importante");
        form.toggleBold();
        form.editor().sendKeys(" y revisar:");
        assertThat(form.editor().findElement(By.cssSelector("b, strong")).getText()).isEqualTo("importante");
        assertThat(form.editor().findElements(By.cssSelector("b, strong"))).hasSize(1);
        assertThat(form.editorCount()).isEqualTo("Pasos para reproducir importante y revisar:".length() + "/600");
    }
}
