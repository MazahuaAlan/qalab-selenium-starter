package dev.morewater.qalab.tests.air;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.air.AirFlowPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU datos de pasajeros y selección de vuelo (CP-AIR-027 a 038). */
class AirPassengersTests extends BaseTest {

    private AirFlowPage atPassengers(int pax) {
        return new AirFlowPage(driver).open().search("MEX", "MTY", pax).selectFirstFlight();
    }

    private void assertStaysOnPassengersWithError(AirFlowPage a, String errorTest, String expected) {
        assertThat(a.textOf(errorTest)).isEqualTo(expected);
        assertThat(a.path()).contains("/air/passengers");
    }

    @Test @Tag("obligatorio")
    @DisplayName("Elegir un vuelo crea la reserva en curso (CP-AIR-027)")
    void choosingFlightCreatesDraft() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1);
        String id = a.flightIds().get(0);
        a.selectFirstFlight();
        assertThat(a.flightSummary()).contains("Ciudad de México (MEX)").contains("Monterrey (MTY)");
        assertThat(a.draft()).containsEntry("flightId", id);
    }

    @Test @Tag("obligatorio")
    @DisplayName("Capturar datos válidos de un pasajero (CP-AIR-028)")
    void validPassengerData() {
        AirFlowPage a = atPassengers(1).fillAllValid(1).submitPassengers();
        a.waitPath("/air/seats");
        assertThat(a.exists("air-first-error-0")).isFalse();
    }

    @Test @Tag("obligatorio")
    @DisplayName("Formulario de pasajeros vacío muestra todos los errores (CP-AIR-029)")
    void emptyFormShowsAllErrors() {
        AirFlowPage a = atPassengers(1).submitPassengers();
        assertThat(a.textOf("air-first-error-0")).isEqualTo("Escribe el nombre (mínimo 2 letras).");
        assertThat(a.textOf("air-last-error-0")).isEqualTo("Escribe los apellidos (mínimo 2 letras).");
        assertThat(a.textOf("air-birth-error-0")).isEqualTo("Indica la fecha de nacimiento.");
        assertThat(a.textOf("air-doc-error-0")).isEqualTo("El documento debe tener de 8 a 12 letras o números.");
        assertThat(a.textOf("air-email-error")).isEqualTo("Escribe un correo válido.");
        assertThat(a.textOf("air-phone-error")).isEqualTo("El teléfono debe tener 10 dígitos.");
        assertThat(a.textOf("air-adult-error")).contains("mayor de 18");
        assertThat(a.path()).contains("/air/passengers");
    }

    @Test @Tag("recomendado")
    @DisplayName("Documento 8 caracteres (mínimo válido) (CP-AIR-033)")
    void documentWithEightCharsIsValid() {
        AirFlowPage a = atPassengers(1);
        a.fillPassenger(0, "Ana", "Pérez", "1990-05-20", "ABCD1234").fillContact("ana@example.com", "5512345678").submitPassengers();
        a.waitPath("/air/seats");
        assertThat(a.exists("air-doc-error-0")).isFalse();
    }

    @Test @Tag("recomendado")
    @DisplayName("Documento con guion (inválido) (CP-AIR-036)")
    void documentWithHyphenIsInvalid() {
        AirFlowPage a = atPassengers(1);
        a.fillPassenger(0, "Ana", "Pérez", "1990-05-20", "ABCD-1234").fillContact("ana@example.com", "5512345678").submitPassengers();
        assertStaysOnPassengersWithError(a, "air-doc-error-0", "El documento debe tener de 8 a 12 letras o números.");
    }

    @Test @Tag("recomendado")
    @DisplayName("Correo de contacto sin arroba (CP-AIR-037)")
    void emailWithoutAtSign() {
        AirFlowPage a = atPassengers(1);
        a.fillPassenger(0, "Ana", "Pérez", "1990-05-20", "ABCD123450").fillContact("ana.example.com", "5512345678").submitPassengers();
        assertStaysOnPassengersWithError(a, "air-email-error", "Escribe un correo válido.");
    }

    @Test @Tag("recomendado")
    @DisplayName("Correo de contacto sin dominio con punto (CP-AIR-038)")
    void emailWithoutDotDomain() {
        AirFlowPage a = atPassengers(1);
        a.fillPassenger(0, "Ana", "Pérez", "1990-05-20", "ABCD123450").fillContact("ana@example", "5512345678").submitPassengers();
        assertStaysOnPassengersWithError(a, "air-email-error", "Escribe un correo válido.");
    }
}
