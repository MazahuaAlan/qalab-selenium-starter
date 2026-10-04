package dev.morewater.qalab.pages.stay;

import dev.morewater.qalab.stay.StayModel;
import java.time.LocalDate;
import java.time.YearMonth;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Calendario de rango propio (RangeCalendar): dos meses visibles, aparece en /stay/ y en el diálogo de modificar. */
public class Calendar extends StayBase {
    public Calendar(WebDriver driver) { super(driver); }

    /** El CSS pone la inicial en mayúscula; se normaliza a minúsculas como en el DOM. */
    public String monthTitle(int offset) { return text("cal-title-" + offset).toLowerCase(java.util.Locale.ROOT); }
    public String hint() { return text("cal-hint"); }
    public boolean prevEnabled() { return enabled("cal-prev"); }
    public void next() { String before = monthTitle(0); click("cal-next"); wait.until(d -> !monthTitle(0).equals(before)); }
    public void prev() { String before = monthTitle(0); click("cal-prev"); wait.until(d -> !monthTitle(0).equals(before)); }

    public boolean hasDay(LocalDate d) { return !driver.findElements(t("cal-day-" + d)).isEmpty(); }
    public WebElement day(LocalDate d) { return visible("cal-day-" + d); }
    public boolean dayEnabled(LocalDate d) { return day(d).isEnabled(); }
    public boolean dayPressed(LocalDate d) { return "true".equals(day(d).getDomAttribute("aria-pressed")); }
    public boolean dayShaded(LocalDate d) { return hasClass(day(d), "in"); }
    public boolean daySelected(LocalDate d) { return hasClass(day(d), "sel"); }

    /** Mes del primer calendario visible. */
    public YearMonth firstMonth() {
        String[] p = monthTitle(0).split(" ");
        for (int i = 0; i < StayModel.MONTHS.length; i++) if (StayModel.MONTHS[i].equals(p[0])) return YearMonth.of(Integer.parseInt(p[1]), i + 1);
        throw new IllegalStateException("Título de mes desconocido: " + monthTitle(0));
    }

    /** Navega con ‹ › hasta que el día sea visible. */
    public void navigateTo(LocalDate d) {
        for (int i = 0; i < 24 && !hasDay(d); i++) {
            if (YearMonth.from(d).isBefore(firstMonth())) prev(); else next();
        }
        wait.until(x -> hasDay(d));
    }

    /** Clic por JavaScript: demuestra que un día deshabilitado no reacciona aunque se fuerce el evento. */
    public void forceClick(LocalDate d) { js("arguments[0].click();", day(d)); }

    public Calendar pick(LocalDate d) {
        navigateTo(d);
        wait.until(x -> day(d).isEnabled());
        day(d).click();
        return this;
    }

    public Calendar selectRange(LocalDate from, LocalDate to) { pick(from); return pick(to); }
}
