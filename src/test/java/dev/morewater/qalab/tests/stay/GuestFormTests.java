package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayBookPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-06: formulario del huésped. */
class GuestFormTests extends StayTest {
    private static final String NAME_ERR = "Escribe el nombre completo del huésped.";
    private static final String EMAIL_ERR = "Escribe un correo válido.";
    private static final String PHONE_ERR = "El teléfono debe tener 10 dígitos.";

    private StayBookPage form() { return startBooking(defaultPlan(14)); }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_036_EnviarElFormularioVacioMuestraTodosLosErrores")
    void A_STAY_036_EnviarElFormularioVacioMuestraTodosLosErrores() {
        StayBookPage b = form();
        long wallet = b.walletCents();
        b.clickConfirm();
        b.eventually(() -> {
            assertThat(b.nameError()).isEqualTo(NAME_ERR);
            assertThat(b.emailError()).isEqualTo(EMAIL_ERR);
            assertThat(b.phoneError()).isEqualTo(PHONE_ERR);
            assertThat(b.termsError()).isEqualTo("Debes aceptar las condiciones de cancelación.");
        });
        assertThat(b.walletCents()).isEqualTo(wallet);
        assertThat(b.walletChipCents()).isEqualTo(wallet);
        assertThat(driver.getCurrentUrl()).contains("/stay/book/");
        assertThat(b.bookingsInState()).isZero();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_037_ValidacionDelNombreMinimo3CaracteresTrasRecortar")
    void A_STAY_037_ValidacionDelNombreMinimo3CaracteresTrasRecortar() {
        StayBookPage b = form().fillValid();
        long wallet = b.walletCents();
        for (String invalid : new String[] {"Al", "  A  "}) {
            b.name(invalid);
            // se vuelve a pulsar «Confirmar» mientras no aparezca el error: en una máquina lenta el primer clic puede perderse
            b.eventually(() -> { b.clickConfirm(); assertThat(b.nameError()).as("«" + invalid + "»").isEqualTo(NAME_ERR); });
            assertThat(b.walletCents()).isEqualTo(wallet);
        }
        b.name("Ana");
        var c = b.confirmOk();
        assertThat(c.title()).isEqualTo("¡Reserva confirmada!");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_038_ValidacionDelCorreoElectronico")
    void A_STAY_038_ValidacionDelCorreoElectronico() {
        StayBookPage b = form().fillValid();
        for (String invalid : new String[] {"ana", "ana@example", "ana @example.com", "@example.com"}) {
            b.email(invalid);
            // se vuelve a pulsar «Confirmar» mientras no aparezca el error: en una máquina lenta el primer clic puede perderse
            b.eventually(() -> { b.clickConfirm(); assertThat(b.emailError()).as("«" + invalid + "»").isEqualTo(EMAIL_ERR); });
        }
        b.email("ana@example.com");
        assertThat(b.confirmOk().title()).isEqualTo("¡Reserva confirmada!");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_039_ValidacionDelTelefonoExactamente10Digitos")
    void A_STAY_039_ValidacionDelTelefonoExactamente10Digitos() {
        StayBookPage b = form().fillValid();
        for (String invalid : new String[] {"551234567", "55123456789", "55123abc78"}) {
            b.phone(invalid);
            // se vuelve a pulsar «Confirmar» mientras no aparezca el error: en una máquina lenta el primer clic puede perderse
            b.eventually(() -> { b.clickConfirm(); assertThat(b.phoneError()).as("«" + invalid + "»").isEqualTo(PHONE_ERR); });
        }
        b.phone("5512345678");
        assertThat(b.confirmOk().title()).isEqualTo("¡Reserva confirmada!");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_040_LimiteDePeticionesEspeciales200CaracteresSi201No")
    void A_STAY_040_LimiteDePeticionesEspeciales200CaracteresSi201No() {
        StayBookPage b = form().fillValid();
        long wallet = b.walletCents();
        b.notes("x".repeat(201));
        b.eventually(() -> assertThat(b.notesCount()).isEqualTo("201/200"));
        b.clickConfirm();
        b.eventually(() -> assertThat(b.notesError()).isEqualTo("Las peticiones admiten hasta 200 caracteres."));
        assertThat(b.walletCents()).isEqualTo(wallet);
        b.notesBackspace();
        b.eventually(() -> assertThat(b.notesCount()).isEqualTo("200/200"));
        assertThat(b.confirmOk().title()).isEqualTo("¡Reserva confirmada!");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_043_LosDatosDelHuespedSeConservanAlRecargar")
    void A_STAY_043_LosDatosDelHuespedSeConservanAlRecargar() {
        StayBookPage b = form().name("Ana Pérez López").email("ana@example.com").phone("5512345678").arrival("16:00")
                .notes("Cama extra, por favor").terms(true);
        b.eventually(() -> assertThat(b.notesCount()).isEqualTo("21/200"));
        b.reload();
        b.waitForm();
        assertThat(b.nameValue()).isEqualTo("Ana Pérez López");
        assertThat(b.emailValue()).isEqualTo("ana@example.com");
        assertThat(b.phoneValue()).isEqualTo("5512345678");
        assertThat(b.notesValue()).isEqualTo("Cama extra, por favor");
        assertThat(b.arrivalValue()).isEqualTo("16:00");
        assertThat(b.termsChecked()).isTrue();
    }
}
