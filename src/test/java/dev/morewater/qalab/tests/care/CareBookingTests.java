package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.care.CarePage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** HU-CARE-07 (especialidad y médico) y HU-CARE-08 (fecha y horario). */
class CareBookingTests extends BaseTest {

    private CarePage care() { return new CarePage(driver); }

    /** Médicos por especialidad (lib/care.ts): nombre y años de experiencia. */
    private static final Map<String, String[]> DOCTORS = Map.of(
            "general", new String[] {"Dra. Lucía Romero|4", "Dr. Mateo Salazar|11", "Dra. Camila Ortega|18"},
            "pediatria", new String[] {"Dr. Andrés Beltrán|25", "Dra. Valeria Cano|10", "Dr. Emilio Duarte|17"},
            "cardiologia", new String[] {"Dra. Renata Villaseñor|24", "Dr. Samuel Quiroz|9", "Dra. Ximena Paredes|16"},
            "dermatologia", new String[] {"Dr. Joaquín Mena|23", "Dra. Ariadna Lozano|8", "Dr. Tomás Escobar|15"},
            "psicologia", new String[] {"Dra. Fernanda Ibarra|22", "Dr. Bruno Cisneros|7", "Dra. Itzel Montaño|14"});

    @Test
    @Tag("recomendado")
    @DisplayName("A_CARE_043_SinPerfilNoSePuedeAgendar")
    void A_CARE_043_SinPerfilNoSePuedeAgendar() {
        CarePage c = care().openBooking();
        assertThat(c.el("care-need-profile").getText()).contains("Primero completa tu perfil");
        c.clickOn("care-need-profile-link");
        c.becomesTrue(15, d -> d.getCurrentUrl().contains("/care/profile"));
        assertThat(driver.getCurrentUrl()).contains("/care/profile");
        c.completeProfile();
        c.openBooking();
        assertThat(c.el("care-step1")).isNotNull();
        assertThat(c.has("care-need-profile")).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_044_CadaEspecialidadLista3MedicosConAnosDeExperiencia")
    void A_CARE_044_CadaEspecialidadLista3MedicosConAnosDeExperiencia() {
        CarePage c = care();
        c.completeProfile();
        c.openBooking();
        for (var e : DOCTORS.entrySet()) {
            c.chooseSpecialty(e.getKey());
            List<String> labels = new ArrayList<>();
            for (WebElement l : driver.findElements(By.cssSelector("[data-test='care-doctors'] label"))) labels.add(l.getText().replace("\n", " "));
            assertThat(labels).as("médicos de " + e.getKey()).hasSize(3);
            for (int i = 0; i < 3; i++) {
                String[] nameYears = e.getValue()[i].split("\\|");
                assertThat(labels.get(i)).as(e.getKey() + " #" + i).isEqualTo(nameYears[0] + " " + nameYears[1] + " años de experiencia");
            }
        }
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_CARE_045_ContinuarDeshabilitadoSinMedicoYCambioDeEspecialidadLimpiaLa")
    void A_CARE_045_ContinuarDeshabilitadoSinMedicoYCambioDeEspecialidadLimpiaLa() {
        CarePage c = care();
        c.completeProfile();
        c.openBooking();
        assertThat(c.enabled("care-step1-next")).isFalse();
        c.chooseSpecialty("general");
        assertThat(c.enabled("care-step1-next")).isFalse();
        c.clickOn("care-doctor-general-1");
        assertThat(c.enabled("care-step1-next")).isTrue();
        c.chooseSpecialty("pediatria");
        for (WebElement r : driver.findElements(By.cssSelector("[data-test='care-doctors'] input[type='radio']"))) assertThat(r.isSelected()).isFalse();
        assertThat(c.enabled("care-step1-next")).isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[care.slow_doctors] A_CARE_046_PersonaLentaLaListaDeMedicosTarda35S")
    void A_CARE_046_PersonaLentaLaListaDeMedicosTarda35S() {
        CarePage c = care();
        c.completeProfile();
        c.openBooking();
        c.el("care-step1");
        long ms = c.measureUntil(() -> c.choose("care-specialty", "cardiologia"), "care-doctors");
        assertThat(ms).as("ms hasta ver los médicos").isLessThan(2000);
        c.clickOn("care-doctor-cardiologia-0");
        assertThat(c.el("care-doctor-cardiologia-0").isSelected()).isTrue();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_048_Paso2Muestra7DiasY18HorariosDe30Minutos")
    void A_CARE_048_Paso2Muestra7DiasY18HorariosDe30Minutos() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0);
        assertThat(c.textOf("care-picked-doctor")).isEqualTo("Dra. Lucía Romero · Medicina general");
        c.awaitSlots();
        List<WebElement> days = driver.findElements(By.cssSelector("[data-test='care-days'] button"));
        assertThat(days).hasSize(7);
        for (int i = 0; i < 7; i++) {
            String iso = c.isoPlus(i);
            assertThat(days.get(i).getAttribute("data-test")).isEqualTo("care-date-" + iso);
            assertThat(days.get(i).getText()).endsWith(iso.substring(8));
        }
        List<WebElement> slots = c.slotButtons();
        assertThat(slots).hasSize(18);
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 18; i++) expected.add(String.format("%02d:%02d", 9 + (i * 30) / 60, (i * 30) % 60));
        assertThat(slots.stream().map(c::slotTime).toList()).isEqualTo(expected);
        // mañana (sin horarios pasados): ocupados (~35 %) deshabilitados y el resto habilitados
        c.pickDay(1);
        long enabled = c.slotButtons().stream().filter(WebElement::isEnabled).count();
        assertThat(enabled).isBetween(1L, 17L);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_049_HorariosOcupadosYPasadosEstanDeshabilitados")
    void A_CARE_049_HorariosOcupadosYPasadosEstanDeshabilitados() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0);
        c.awaitSlots();
        c.setClockToday(12, 0); // "ahora" = hoy a las 12:00 en la página
        c.becomesTrue(5, d -> d.findElements(By.cssSelector("[data-test='care-slots'] button:disabled")).stream()
                .map(c::slotTime).toList().containsAll(List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30")));
        for (String t : new String[] {"09:00", "09:30", "10:00", "10:30", "11:00", "11:30"}) {
            assertThat(driver.findElement(By.cssSelector("[data-test='care-slot-" + t + "']")).isEnabled()).as("horario pasado " + t).isFalse();
        }
        try { driver.findElement(By.cssSelector("[data-test='care-slot-09:00']")).click(); } catch (RuntimeException ignored) { /* deshabilitado: no debe pasar nada */ }
        assertThat(c.enabled("care-step2-next")).isFalse();
        assertThat(c.slotButtons().stream().anyMatch(b -> "true".equals(b.getAttribute("aria-pressed")))).isFalse();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_CARE_050_ElegirHorarioHabilitaContinuarCambiarDeDiaLoLimpia")
    void A_CARE_050_ElegirHorarioHabilitaContinuarCambiarDeDiaLoLimpia() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0);
        c.pickDay(1);
        assertThat(c.enabled("care-step2-next")).isFalse();
        String t = c.firstFreeSlot();
        c.pickSlot(t);
        assertThat(c.el("care-slot-" + t).getAttribute("aria-pressed")).isEqualTo("true");
        assertThat(c.enabled("care-step2-next")).isTrue();
        c.pickDay(2);
        assertThat(c.slotButtons().stream().anyMatch(b -> "true".equals(b.getAttribute("aria-pressed")))).isFalse();
        assertThat(c.enabled("care-step2-next")).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_CARE_051_HorarioYaReservadoPorMiApareceOcupado")
    void A_CARE_051_HorarioYaReservadoPorMiApareceOcupado() {
        CarePage c = care();
        c.completeProfile();
        String h = c.toStep3("general", 0, 1);
        c.typeReason(CarePage.REASON).confirmAndAwaitBooked();
        c.toStep2("general", 0).pickDay(1);
        assertThat(c.el("care-slot-" + h).isEnabled()).as("el horario " + h + " ya reservado").isFalse();
    }

    @Test
    @Tag("bug")
    @Tag("opcional")
    @DisplayName("[care.visual_slot_time] A_CARE_053_PersonaVisualFormatoDeHoraDistintoEnListaYResumen")
    void A_CARE_053_PersonaVisualFormatoDeHoraDistintoEnListaYResumen() {
        CarePage c = care();
        c.completeProfile();
        c.toStep2("general", 0).pickDay(1);
        assertThat(c.el("care-slot-14:30").getText()).isEqualTo("14:30");
        String h = c.firstFreeSlot();
        String label = c.el("care-slot-" + h).getText();
        c.pickSlot(h);
        c.clickOn("care-step2-next");
        c.el("care-step3");
        assertThat(c.textOf("care-sum-when")).endsWith(label);
    }

    @Test
    @Tag("bug")
    @Tag("recomendado")
    @DisplayName("[care.slow_slots] A_CARE_054_PersonaLentaLosHorariosTardan4S")
    void A_CARE_054_PersonaLentaLosHorariosTardan4S() {
        CarePage c = care();
        c.completeProfile();
        c.openBooking().chooseSpecialty("general");
        c.clickOn("care-doctor-general-0");
        long ms = c.measureUntil(() -> c.clickOn("care-step1-next"), "care-slots");
        assertThat(ms).as("ms hasta ver los horarios").isLessThan(2000);
        long ms2 = c.measureUntil(() -> c.clickOn("care-date-" + c.isoPlus(1)), "care-slots");
        assertThat(ms2).as("ms al cambiar de día").isLessThan(2000);
    }

    @Test
    @Tag("bug")
    @Tag("obligatorio")
    @DisplayName("[care.flaky_slots] A_CARE_055_PersonaIntermitenteError503DeHorariosYReintento")
    void A_CARE_055_PersonaIntermitenteError503DeHorariosYReintento() {
        CarePage c = care();
        c.completeProfile();
        c.openBooking().chooseSpecialty("general");
        c.clickOn("care-doctor-general-0");
        c.forceNextCall(true); // semilla fija: el próximo sorteo caería en el 35 % de fallo si el defecto estuviera activo
        c.clickOn("care-step1-next");
        c.visible2();
        assertThat(c.has("care-slots-error")).as("los horarios no deberían fallar").isFalse();
        assertThat(c.has("care-slots")).isTrue();
        c.forceNextCall(true);
        long before = c.callsNow();
        c.clickOn("care-date-" + c.isoPlus(1));
        c.waitCalls(before);
        c.visible2();
        assertThat(c.has("care-slots-error")).as("los horarios no deberían fallar al cambiar de día").isFalse();
    }
}
