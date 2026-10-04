package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.Calendar;
import dev.morewater.qalab.pages.stay.StayResultsPage;
import dev.morewater.qalab.pages.stay.StaySearchPage;
import dev.morewater.qalab.stay.StayModel;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-02: calendario de rango. */
class CalendarTests extends StayTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Selección de entrada y salida en el calendario (CP-STAY-007)")
    void pickCheckInAndCheckOut() {
        StaySearchPage s = search().open();
        LocalDate today = s.today(), in = today.plusDays(20), out = today.plusDays(24);
        YearMonth ym = YearMonth.from(today);
        assertThat(s.calendar.monthTitle(0)).isEqualTo(StayModel.monthTitle(ym));
        assertThat(s.calendar.monthTitle(1)).isEqualTo(StayModel.monthTitle(ym.plusMonths(1)));
        assertThat(s.calendar.hint()).isEqualTo("Elige la fecha de entrada");

        s.calendar.pick(in);
        assertThat(s.calendar.dayPressed(in)).isTrue();
        assertThat(s.calendar.hint()).isEqualTo("Elige la fecha de salida");
        assertThat(s.summary()).isEqualTo("Entrada " + s.pretty(in));
        assertThat(s.hasNights()).isFalse();

        s.calendar.pick(out);
        assertThat(s.calendar.hint()).isEqualTo("Elige la fecha de entrada");
        assertThat(s.nights()).isEqualTo("4 noches");
        assertThat(s.summary()).isEqualTo("Entrada " + s.pretty(in) + " · salida " + s.pretty(out) + " · 4 noches");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Días pasados deshabilitados y hoy seleccionable (CP-STAY-008)")
    void pastDaysDisabledTodaySelectable() {
        StaySearchPage s = search().open();
        LocalDate today = s.today();
        Calendar cal = s.calendar;
        for (LocalDate d = today.withDayOfMonth(1); d.isBefore(today); d = d.plusDays(1))
            assertThat(cal.dayEnabled(d)).as("el día " + d + " (anterior a hoy) debe estar deshabilitado").isFalse();
        if (today.getDayOfMonth() > 1) {
            LocalDate yesterday = today.minusDays(1);
            String before = s.summary();
            cal.forceClick(yesterday);
            assertThat(s.summary()).isEqualTo(before);
            assertThat(cal.dayPressed(yesterday)).isFalse();
        }
        cal.pick(today);
        assertThat(cal.dayPressed(today)).isTrue();
        assertThat(cal.hint()).isEqualTo("Elige la fecha de salida");
        cal.pick(today.plusDays(1));
        assertThat(s.nights()).isEqualTo("1 noche");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Navegación de meses: anterior deshabilitado en el mes actual (CP-STAY-009)")
    void monthNavigation() {
        StaySearchPage s = search().open();
        YearMonth ym = YearMonth.from(s.today());
        Calendar cal = s.calendar;
        assertThat(cal.prevEnabled()).isFalse();
        cal.next();
        assertThat(cal.monthTitle(0)).isEqualTo(StayModel.monthTitle(ym.plusMonths(1)));
        assertThat(cal.monthTitle(1)).isEqualTo(StayModel.monthTitle(ym.plusMonths(2)));
        assertThat(cal.prevEnabled()).isTrue();
        cal.next();
        assertThat(cal.monthTitle(0)).isEqualTo(StayModel.monthTitle(ym.plusMonths(2)));
        assertThat(cal.monthTitle(1)).isEqualTo(StayModel.monthTitle(ym.plusMonths(3)));
        cal.prev();
        cal.prev();
        assertThat(cal.monthTitle(0)).isEqualTo(StayModel.monthTitle(ym));
        assertThat(cal.monthTitle(1)).isEqualTo(StayModel.monthTitle(ym.plusMonths(1)));
        assertThat(cal.prevEnabled()).isFalse();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Sombreado de los días intermedios del rango (CP-STAY-010)")
    void rangeShadingCoversInnerDays() {
        StaySearchPage s = search().open();
        LocalDate t = s.today();
        Calendar cal = s.calendar;
        cal.selectRange(t.plusDays(20), t.plusDays(23));
        assertThat(s.nights()).isEqualTo("3 noches");
        assertThat(cal.dayShaded(t.plusDays(21))).isTrue();
        assertThat(cal.dayShaded(t.plusDays(22))).isTrue();
        for (int off : new int[] {20, 23}) {
            assertThat(cal.daySelected(t.plusDays(off))).as("sel " + off).isTrue();
            assertThat(cal.dayPressed(t.plusDays(off))).isTrue();
            assertThat(cal.dayShaded(t.plusDays(off))).as("in " + off).isFalse();
        }
        for (int off : new int[] {19, 24}) {
            assertThat(cal.dayShaded(t.plusDays(off))).as("in " + off).isFalse();
            assertThat(cal.daySelected(t.plusDays(off))).as("sel " + off).isFalse();
        }
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Buscar sin fecha de salida muestra error (CP-STAY-012)")
    void searchWithoutCheckOutShowsError() {
        StaySearchPage s = search().open();
        LocalDate in = s.today().plusDays(20), out = s.today().plusDays(22);
        s.calendar.pick(in);
        s.submit();
        assertThat(s.error()).isEqualTo("Elige la fecha de entrada y la de salida.");
        assertThat(driver.getCurrentUrl()).doesNotContain("/results/");
        s.calendar.pick(out);
        s.eventually(() -> assertThat(s.hasError()).isFalse());
        assertThat(s.nights()).isEqualTo("2 noches");
        s.searchAndWait();
        assertThat(driver.getCurrentUrl()).contains("in=" + in + "&out=" + out);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Límite superior de noches: 14 válido, 15 rechazado (CP-STAY-013)")
    void maxNightsBoundary() {
        StaySearchPage s = search().open();
        LocalDate in = s.today().plusDays(17);
        s.calendar.selectRange(in, in.plusDays(15));
        assertThat(s.nights()).isEqualTo("15 noches");
        s.submit();
        assertThat(s.error()).isEqualTo("La estancia máxima es de 14 noches.");
        assertThat(driver.getCurrentUrl()).doesNotContain("/results/");

        s.calendar.selectRange(in, in.plusDays(14));
        assertThat(s.nights()).isEqualTo("14 noches");
        s.eventually(() -> assertThat(s.hasError()).isFalse());
        StayResultsPage r = s.searchAndWait();
        assertThat(driver.getCurrentUrl()).contains("in=" + in + "&out=" + in.plusDays(14));
        assertThat(r.subtitle()).contains("14 noches");
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[stay.visual_range_highlight] El sombreado del rango está corrido un día (stay.visual_range_highlight) (CP-STAY-094)")
    void shadingIsNotShifted() {
        StaySearchPage s = search().open();
        LocalDate t = s.today();
        Calendar cal = s.calendar;
        cal.selectRange(t.plusDays(20), t.plusDays(23));
        assertThat(s.nights()).isEqualTo("3 noches");
        assertThat(cal.dayShaded(t.plusDays(21))).as("21 sombreado").isTrue();
        assertThat(cal.dayShaded(t.plusDays(22))).as("22 sombreado").isTrue();
        assertThat(cal.dayShaded(t.plusDays(23))).as("la salida no se sombrea").isFalse();
        assertThat(cal.daySelected(t.plusDays(23))).isTrue();
    }
}
