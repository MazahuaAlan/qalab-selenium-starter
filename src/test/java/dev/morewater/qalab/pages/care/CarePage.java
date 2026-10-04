package dev.morewater.qalab.pages.care;

import dev.morewater.qalab.pages.BasePage;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page object de Care: perfil, asistente de citas, Mis citas y utilidades de tiempo/caos para pruebas deterministas. */
public class CarePage extends BasePage {
    public static final String DEMO_CURP = "PEGA900520HDFRRN09";
    public static final String REASON = "Dolor de cabeza persistente desde hace tres días";

    public CarePage(WebDriver driver) { super(driver); }

    /** Datos del formulario de perfil. */
    public static class Prof {
        public String name = "Ana Pérez López", birth = "1990-05-20", sex = "H", curp = DEMO_CURP, blood = "O+";
        public String insurance = "Sin seguro", policy = "", emName = "Luis Pérez", emPhone = "5512345678";
        public boolean consent = true;

        public static Prof valid() { return new Prof(); }
        public Prof birth(String v) { birth = v; return this; }
        public Prof sex(String v) { sex = v; return this; }
        public Prof curp(String v) { curp = v; return this; }
        public Prof phone(String v) { emPhone = v; return this; }
        public Prof consent(boolean v) { consent = v; return this; }
        public Prof insured() { insurance = "Seguro Aurora"; policy = "POL12345678"; return this; }
    }

    // ---------- utilidades generales ----------

    public static String mxn(long cents) { return String.format(Locale.US, "$%,.2f", cents / 100.0); }

    public long walletCents() { return cents(text("wallet-chip").replace("Saldo", "")); }

    /** Fecha local del navegador hoy + n días (AAAA-MM-DD). */
    public String isoPlus(int days) {
        return js("const d=new Date(); d.setDate(d.getDate()+arguments[0]); const p=n=>String(n).padStart(2,'0');"
                + "return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate());", (long) days);
    }

    public long apptCount() { return ((Number) js("return window.qalab.state().care.appts.length")).longValue(); }

    public String stateString(String expr) { return js("const s=window.qalab.state(); return String(" + expr + ");"); }

    public <T> T run(String script, Object... args) { return js(script, args); }

    public void choose(String dataTest, String value) { select(dataTest, value); }

    /** Espera a que el paso 2 termine de consultar horarios (con éxito o con error). */
    public void visible2() { wait.until(d -> present("care-slots") || present("care-slots-error")); }

    public long callsNow() { return calls(); }

    public void waitCalls(long before) { waitForCallsAfter(before); }

    public boolean has(String dataTest) { return present(dataTest); }

    public String textOf(String dataTest) { return text(dataTest); }

    public void clickOn(String dataTest) { click(dataTest); }

    public WebElement el(String dataTest) { return visible(dataTest); }

    public boolean enabled(String dataTest) { return visible(dataTest).isEnabled(); }

    public void openPath(String path) { go(path); }

    /** Mide (ms) lo que tarda una acción hasta que se cumple la condición, sondeando cada 50 ms. */
    public long measureUntil(Runnable action, String visibleDataTest) {
        long t0 = System.nanoTime();
        action.run();
        new WebDriverWait(driver, Duration.ofSeconds(20)).pollingEvery(Duration.ofMillis(50))
                .until(d -> { List<WebElement> l = d.findElements(t(visibleDataTest)); return !l.isEmpty() && l.get(0).isDisplayed(); });
        return (System.nanoTime() - t0) / 1_000_000;
    }

    // ---------- reloj y caos ----------

    /** Desplaza Date.now() de la página (la app lo lee cada segundo). Se pierde al recargar. */
    public void skewClock(long deltaMs) {
        js("if(!window.__realNow){window.__realNow=Date.now.bind(Date);window.__skew=0;Date.now=()=>window.__realNow()+window.__skew;}"
                + "window.__skew+=arguments[0];", deltaMs);
    }

    /** Hace que "ahora" sea hoy a la hora indicada (hora local del navegador). */
    public void setClockToday(int hour, int minute) {
        js("if(!window.__realNow){window.__realNow=Date.now.bind(Date);window.__skew=0;Date.now=()=>window.__realNow()+window.__skew;}"
                + "const t=new Date(); t.setHours(arguments[0],arguments[1],0,0); window.__skew=t.getTime()-window.__realNow();", (long) hour, (long) minute);
    }

    /** Hace que "ahora" sea {@code hoursBefore} horas antes de la fecha/hora dada. */
    public void setClockBefore(String date, String time, int hoursBefore) {
        js("if(!window.__realNow){window.__realNow=Date.now.bind(Date);window.__skew=0;Date.now=()=>window.__realNow()+window.__skew;}"
                + "const t=new Date(arguments[0]+'T'+arguments[1]+':00').getTime()-arguments[2]*3600000; window.__skew=t-window.__realNow();",
                date, time, (long) hoursBefore);
    }

    /**
     * Fija la secuencia pseudoaleatoria de la app (lib/net.ts) para que la PRÓXIMA llamada simulada falle (wantFail) o no.
     * Cada llamada consume dos números: latencia y el sorteo de fallo (p = 0.35).
     */
    public void forceNextCall(boolean wantFail) {
        Number r = js("const want=arguments[0]; const chaos=window.qalab.state().chaos; const seed=chaos.seed;"
                + "const mb=(s)=>{let a=s>>>0; a=(a+0x6d2b79f5)>>>0; let t=a; t=Math.imul(t^(t>>>15),t|1); t^=t+Math.imul(t^(t>>>7),t|61); return ((t^(t>>>14))>>>0)/4294967296;};"
                + "const draw=(k)=>mb(((seed>>>0)+Math.imul(k+1,0x9e3779b1))>>>0);"
                + "let n=0; try{const raw=sessionStorage.getItem('qalab.rng'); if(raw){const st=JSON.parse(raw); if(st.seed===seed) n=st.n;}}catch(e){}"
                + "for(let m=n;m<n+1000;m++){ if((draw(m+1)<0.35)===want){ sessionStorage.setItem('qalab.rng',JSON.stringify({seed:seed,n:m})); return m; } }"
                + "return -1;", wantFail);
        if (r.intValue() < 0) throw new IllegalStateException("No se pudo fijar la secuencia aleatoria");
    }

    // ---------- perfil ----------

    public CarePage openProfile() { go("/care/profile/"); visible("care-name"); return this; }

    public CarePage fillProfile(Prof p) {
        type("care-name", p.name);
        if (p.birth != null) setDate("care-birth", p.birth);
        if ("H".equals(p.sex)) click("care-sex-h");
        else if ("M".equals(p.sex)) click("care-sex-m");
        type("care-curp", p.curp);
        if (p.blood != null) new org.openqa.selenium.support.ui.Select(visible("care-blood")).selectByVisibleText(p.blood);
        new org.openqa.selenium.support.ui.Select(visible("care-insurance")).selectByVisibleText(p.insurance);
        if (!"Sin seguro".equals(p.insurance)) type("care-policy", p.policy);
        type("care-emergency-name", p.emName);
        type("care-emergency-phone", p.emPhone);
        WebElement consent = visible("care-consent");
        if (consent.isSelected() != p.consent) consent.click();
        return this;
    }

    /** Reabre el perfil con navegación del lado del cliente (Resumen → Mi perfil), sin recargar la página. */
    public CarePage reopenProfileViaNav() {
        click("nav-care");
        visible("care-progress-text");
        click("nav-care-profile");
        visible("care-name");
        return this;
    }

    public CarePage save() { click("care-save"); return this; }

    public CarePage saveOk() { save(); visible("care-saved"); return this; }

    /** Abre el perfil, lo llena y lo guarda correctamente. */
    public CarePage completeProfile(Prof p) { openProfile().fillProfile(p).saveOk(); return this; }

    public CarePage completeProfile() { return completeProfile(Prof.valid()); }

    public boolean saved() { return present("care-saved"); }

    public boolean hasError(String key) { return present("care-" + key + "-error"); }

    public String error(String key) { return text("care-" + key + "-error"); }

    /** Espera a que el guardado termine: con "Perfil guardado." o con algún error de campo. */
    public CarePage awaitSaveOutcome() {
        wait.until(d -> present("care-saved") || !d.findElements(By.cssSelector("[data-test^='care-'][data-test$='-error']")).isEmpty());
        return this;
    }

    public String field(String dataTest) { return visible(dataTest).getDomProperty("value"); }

    // ---------- asistente de citas ----------

    public CarePage openBooking() { go("/care/book/"); return this; }

    public CarePage chooseSpecialty(String spec) {
        visible("care-step1");
        select("care-specialty", spec);
        visible("care-doctors");
        wait.until(d -> present("care-doctor-" + spec + "-0"));
        return this;
    }

    /** Pasos 1 → 2 con el médico indicado. No espera los horarios. */
    public CarePage toStep2(String spec, int doctorIdx) {
        openBooking().chooseSpecialty(spec);
        click("care-doctor-" + spec + "-" + doctorIdx);
        click("care-step1-next");
        visible("care-step2");
        return this;
    }

    public CarePage awaitSlots() { visible("care-slots"); return this; }

    /** Elige el día (hoy + offset) y espera los horarios. offset 0 es el día por defecto (no recarga). */
    public CarePage pickDay(int offset) {
        if (offset == 0) return awaitSlots();
        long before = calls();
        click("care-date-" + isoPlus(offset));
        waitForCallsAfter(before);
        return awaitSlots();
    }

    public List<WebElement> slotButtons() { return driver.findElements(By.cssSelector("[data-test='care-slots'] button")); }

    public String slotTime(WebElement b) { return b.getAttribute("data-test").replace("care-slot-", ""); }

    /** Hora (HH:MM) del primer horario habilitado. */
    public String firstFreeSlot() {
        wait.until(d -> d.findElements(By.cssSelector("[data-test='care-slots'] button:not([disabled])")).size() > 0);
        return slotTime(driver.findElement(By.cssSelector("[data-test='care-slots'] button:not([disabled])")));
    }

    public CarePage pickSlot(String hhmm) {
        click("care-slot-" + hhmm);
        wait.until(d -> "true".equals(d.findElement(t("care-slot-" + hhmm)).getAttribute("aria-pressed")));
        return this;
    }

    /** Del paso 2 (con horarios cargados) al paso 3 con el primer horario libre del día elegido; devuelve la hora. */
    public String chooseFirstFreeAndContinue() {
        String hhmm = firstFreeSlot();
        pickSlot(hhmm);
        click("care-step2-next");
        visible("care-step3");
        return hhmm;
    }

    /** Asistente completo hasta el paso 3 (hoy + dayOffset, primer horario libre). */
    public String toStep3(String spec, int doctorIdx, int dayOffset) {
        toStep2(spec, doctorIdx).pickDay(dayOffset);
        return chooseFirstFreeAndContinue();
    }

    public CarePage typeReason(String reason) { type("care-reason", reason); return this; }

    /** Confirma la cita y espera la redirección a Mis citas (camino feliz). */
    public CarePage confirmAndAwaitBooked() {
        click("care-confirm");
        visible("care-booked");
        return this;
    }

    // ---------- Mis citas ----------

    public CarePage openAppointments() { go("/care/appointments/"); return this; }

    public CarePage openCancelModal() { click("care-cancel"); visible("care-modal"); return this; }

    public String cancelPolicy() { return text("care-cancel-policy"); }

    public CarePage confirmCancel() { click("care-cancel-confirm"); visible("care-appts-msg"); return this; }

    /** Espera acotada: devuelve true si la condición se cumple dentro del tiempo (sin lanzar). */
    public boolean becomesTrue(int seconds, java.util.function.Predicate<WebDriver> cond) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(cond::test);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public List<String> texts(String css) {
        return driver.findElements(By.cssSelector(css)).stream().map(e -> e.getText().trim()).collect(Collectors.toList());
    }
}
