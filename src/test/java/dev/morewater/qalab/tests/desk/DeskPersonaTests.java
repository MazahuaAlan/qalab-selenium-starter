package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.desk.DeskBoardPage;
import dev.morewater.qalab.pages.desk.DeskListPage;
import dev.morewater.qalab.pages.desk.DeskNewPage;
import dev.morewater.qalab.pages.desk.DeskTicketPage;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * HU-DESK-14: pruebas de contrato de los defectos de Desk. Verifican el comportamiento CORRECTO: con «estandar» pasan y con la persona
 * que activa el defecto (-Dqalab.user=...) deben fallar.
 */
class DeskPersonaTests extends BaseTest {
    private static final long MAX_FAST_MS = 2500;

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[platform.session_expiry] A_DESK_084_LaSesionDeExpiraCaducaALos90SSinAviso")
    void A_DESK_084_LaSesionDeExpiraCaducaALos90SSinAviso() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.advanceClock(95_000); // equivale a trabajar 95 s en el tablero
        boolean redirected;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).until(d -> d.getCurrentUrl().contains("/id/"));
            redirected = true;
        } catch (TimeoutException e) {
            redirected = false;
        }
        assertThat(redirected).as("la sesión no debe caducar sin aviso (URL: %s)", driver.getCurrentUrl()).isFalse();
        assertThat(board.move("DK-1", "progreso")).isEqualTo("DK-1 movido a En progreso");
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[desk.slow_board] A_DESK_085_PersonaLentoElTableroTarda4SEnCargar")
    void A_DESK_085_PersonaLentoElTableroTarda4SEnCargar() {
        DeskBoardPage board = new DeskBoardPage(driver).openWithoutWaiting();
        long t0 = System.nanoTime();
        board.waitForBoard();
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        assertThat(ms).as("ms hasta ver el tablero").isLessThan(MAX_FAST_MS);
        assertThat(board.totalCards()).isEqualTo(8);
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[desk.slow_save] A_DESK_086_PersonaLentoGuardarUnTicketTarda4S")
    void A_DESK_086_PersonaLentoGuardarUnTicketTarda4S() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.title("Ticket lento de prueba").describe("Descripción suficiente para el ticket");
        long t0 = System.nanoTime();
        form.submit();
        form.awaitCreated();
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        assertThat(ms).as("ms hasta ver la ficha del ticket").isLessThan(MAX_FAST_MS);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.flaky_move] A_DESK_087_PersonaIntermitenteMoverFallaAlAzarYLaInterfazNoRevierte")
    void A_DESK_087_PersonaIntermitenteMoverFallaAlAzarYLaInterfazNoRevierte() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.setChaosFailPct(100); // determinista: toda petición de mover falla
        String toast = board.move("DK-2", "revision");
        assertThat(toast).startsWith("No se pudo mover DK-2");
        assertThat(board.hasCard("nuevo", "DK-2")).as("la tarjeta vuelve a su columna tras el fallo").isTrue();
        assertThat(board.hasCard("revision", "DK-2")).isFalse();
        board.setChaosFailPct(0);
        board.reload();
        assertThat(board.hasCard("nuevo", "DK-2")).as("tras recargar sigue en su columna original").isTrue();
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.flaky_create] A_DESK_088_PersonaIntermitenteCreacionFallidaDuplicaElTicketAl")
    void A_DESK_088_PersonaIntermitenteCreacionFallidaDuplicaElTicketAl() {
        String title = "Ticket intermitente 1";
        DeskNewPage form = new DeskNewPage(driver).open();
        form.title(title).describe("Descripción suficiente para el ticket");
        long before = form.ticketsInState();
        form.setChaosFailPct(100); // determinista: toda petición de guardado falla
        for (int round = 1; round <= 15; round++) {
            form.submitAndWaitCall();
            form.waitFormError();
            assertThat(form.ticketsInState()).as("un fallo no debe crear el ticket (intento %d)", round).isEqualTo(before);
        }
        form.setChaosFailPct(0);
        boolean created = false;
        for (int attempt = 0; attempt < 10 && !created; attempt++) {
            form.submitAndWaitCall();
            created = form.awaitCreatedOrError();
        }
        form.awaitCreated();
        assertThat(form.ticketsWithTitle(title)).as("el reintento no duplica el ticket").isEqualTo(1);
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[desk.visual_priority] A_DESK_089_PersonaVisualColoresDePrioridadInvertidos")
    void A_DESK_089_PersonaVisualColoresDePrioridadInvertidos() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        WebElement high = board.card("DK-1");
        assertThat(board.badgeText(high)).isEqualTo("Alta");
        assertThat(board.badgeColor(high)).as("Alta debe ser roja").isEqualTo("rgb(192, 31, 47)");
        assertThat(board.badgeColor(board.card("DK-7"))).as("DK-7 también es Alta").isEqualTo("rgb(192, 31, 47)");
        assertThat(board.badgeColor(board.card("DK-2"))).as("Baja debe ser verde").isEqualTo("rgb(0, 121, 76)");
        assertThat(board.badgeColor(board.card("DK-3"))).as("Media debe ser amarilla").isEqualTo("rgb(242, 183, 5)");
        new DeskListPage(driver).open();
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-1");
        assertThat(ticket.priorityBadgeColor()).as("la ficha también debe mostrar Alta en rojo").isEqualTo("rgb(192, 31, 47)");
    }

    @Test
    @Tag("bug")
    @Tag("opcional")
    @DisplayName("[desk.visual_counts] A_DESK_090_PersonaVisualElContadorDeColumnaSumaUnaTarjetaDeMas")
    void A_DESK_090_PersonaVisualElContadorDeColumnaSumaUnaTarjetaDeMas() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        for (String col : DeskBoardPage.COLUMNS) {
            assertThat(board.countValue(col)).as("contador de " + col).isEqualTo(board.cardIds(col).size());
        }
        board.filterAssignee("Ana");
        board.waitCardCount(2);
        for (String col : DeskBoardPage.COLUMNS) {
            assertThat(board.countValue(col)).as("contador de %s con el filtro Ana", col).isEqualTo(board.cardIds(col).size());
        }
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.amnesia_draft] A_DESK_091_PersonaAmnesiaElBorradorDelTicketSePierdeAlRecargar")
    void A_DESK_091_PersonaAmnesiaElBorradorDelTicketSePierdeAlRecargar() {
        DeskNewPage form = new DeskNewPage(driver).open();
        form.title("Borrador que se perderá");
        driver.navigate().refresh();
        form.waitHydrated();
        assertThat(form.titleValue()).isEqualTo("Borrador que se perderá");
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.edit_expires] A_DESK_092_PersonaExpiraEditarUnTituloMasDe20SExpiraSinAviso")
    void A_DESK_092_PersonaExpiraEditarUnTituloMasDe20SExpiraSinAviso() {
        String title = "Nuevo título muy cuidadosamente redactado";
        DeskTicketPage ticket = new DeskTicketPage(driver).open("DK-3");
        ticket.startTitleEdit();
        ticket.typeNewTitle(title);
        ticket.advanceClock(25_000); // equivale a tardar 25 s en redactarlo
        ticket.saveTitleButton();
        ticket.waitTitleOrError(title);
        assertThat(ticket.hasTitleError() ? ticket.titleError() : "").as("no debe haber error de expiración").isEmpty();
        assertThat(ticket.title()).isEqualTo(title);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.wip_ignored] A_DESK_093_PersonaDescuadreElLimiteWIPNoSeAplica")
    void A_DESK_093_PersonaDescuadreElLimiteWIPNoSeAplica() {
        DeskBoardPage board = new DeskBoardPage(driver).open();
        board.move("DK-1", "progreso");
        assertThat(board.cardIds("progreso")).hasSize(3);
        String toast = board.move("DK-2", "progreso");
        assertThat(toast).as("el cuarto ticket debe rechazarse").contains("Límite WIP");
        assertThat(board.cardIds("progreso")).as("tarjetas en progreso").hasSize(3).doesNotContain("DK-2");
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[desk.bulk_partial] A_DESK_094_PersonaDescuadreLaAccionEnLoteOmiteElUltimoTicket")
    void A_DESK_094_PersonaDescuadreLaAccionEnLoteOmiteElUltimoTicket() {
        DeskListPage list = new DeskListPage(driver).open();
        list.check("DK-1");
        list.check("DK-2");
        list.check("DK-3");
        list.bulkTo("hecho");
        list.applyBulk();
        assertThat(list.bulkMessage()).isEqualTo("3 tickets seleccionados: estado «Hecho» aplicado.");
        for (String id : new String[] {"DK-1", "DK-2", "DK-3"}) assertThat(list.status(id)).as("estado de " + id).isEqualTo("Hecho");
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[desk.overdue_wrong] A_DESK_095_PersonaDescuadreVencidoSeMarcaElMismoDiaDeLaFechaLimite")
    void A_DESK_095_PersonaDescuadreVencidoSeMarcaElMismoDiaDeLaFechaLimite() {
        DeskNewPage form = new DeskNewPage(driver);
        String today = form.open().today();
        String id = form.createTicket("Ticket que vence hoy mismo", DeskNewPage.DEFAULT_DESC, today);
        DeskListPage list = new DeskListPage(driver).open();
        assertThat(list.due(id)).isEqualTo(today);
        assertThat(list.overdue(id)).as("vence hoy: no debe marcarse «Vencido» hasta mañana").isFalse();
        boolean dk1Expected = list.due("DK-1").compareTo(today) < 0;
        assertThat(list.overdue("DK-1")).as("DK-1 vence %s", list.due("DK-1")).isEqualTo(dk1Expected);
    }
}
