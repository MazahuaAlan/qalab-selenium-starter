package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayBookPage;
import dev.morewater.qalab.pages.stay.StayConfirmationPage;
import dev.morewater.qalab.pages.stay.WalletPage;
import dev.morewater.qalab.stay.StayModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-08: pago y confirmación de la reserva. */
class BookingTests extends StayTest {

    private static String mxn(long c) { return StayModel.mxn(c); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Reserva completa y cobro único (CP-STAY-049)")
    void completeBookingChargesOnce() {
        Plan p = defaultPlan(14);
        long total = p.quote().total();
        StayBookPage b = startBooking(p);
        b.setLatency(1500); // hace observable el estado «Confirmando…»
        b.name("Ana Pérez López").email("ana@example.com").phone("5512345678").notes("Cama extra, por favor").terms(true);
        assertThat(b.confirmText()).isEqualTo("Pagar " + mxn(total) + " con billetera");
        long start = b.walletCents();

        b.clickConfirm();
        b.eventually(() -> {
            assertThat(b.confirmText()).isEqualTo("Confirmando…");
            assertThat(b.confirmEnabled()).isFalse();
        });
        StayConfirmationPage c = confirmation().waitConfirmed();
        assertThat(driver.getCurrentUrl()).contains("/stay/confirmation/?code=ST");
        assertThat(c.title()).isEqualTo("¡Reserva confirmada!");
        assertThat(c.code()).matches("^ST[A-Z0-9]{4}$");
        assertThat(c.total()).isEqualTo(total);
        assertThat(c.walletCents()).isEqualTo(start - total);
        c.eventually(() -> assertThat(c.walletChipCents()).isEqualTo(start - total));
        assertThat(c.bookingsInState()).isEqualTo(1);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("El movimiento del cobro aparece en la billetera (CP-STAY-050)")
    void chargeShowsInWallet() {
        Plan p = defaultPlan(14);
        long total = p.quote().total();
        long start = search().walletCents();
        StayConfirmationPage c = startBooking(p).fillValid().confirmOk();
        String code = c.code();
        c.goMovements();
        c.eventually(() -> assertThat(c.path()).startsWith("/bank/movements/"));

        WalletPage w = wallet().openFromChip();
        assertThat(driver.getCurrentUrl()).contains("/wallet/");
        assertThat(w.balance()).isEqualTo(start - total);
        w.waitRows();
        assertThat(w.label(0)).isEqualTo("Estancia " + p.hotel().name() + " · " + code);
        assertThat(w.app(0)).isEqualTo("Stay");
        assertThat(w.amount(0)).isEqualTo(mxn(-total));
        assertThat(w.rows()).as("el cargo se registra una sola vez").hasSize(1);
    }

    @Test
    @Tag("opcional")
    @DisplayName("Confirmación inexistente por URL (CP-STAY-055)")
    void unknownConfirmationCode() {
        StayConfirmationPage c = confirmation().open("ST0000");
        c.waitNotFound();
        assertThat(c.bodyText()).contains("No encontramos esa reserva.");
        c.clickNotFoundLink();
        c.eventually(() -> assertThat(c.path()).startsWith("/stay/stays/"));
        confirmation().open(null).waitNotFound();
        assertThat(c.bodyText()).contains("No encontramos esa reserva.");
    }

    @Test
    @Tag("opcional")
    @DisplayName("Pantalla de confirmación: contenido y enlaces (CP-STAY-056)")
    void confirmationScreenContentAndLinks() {
        Plan p = defaultPlan(14);
        StayConfirmationPage c = startBooking(p).fillValid().confirmOk();
        assertThat(c.statusRole()).isTrue();
        String detail = c.detailText();
        assertThat(detail).contains(p.hotel().name() + " · Cancún · " + p.room().name());
        assertThat(detail).contains(c.pretty(p.in()) + " → " + c.pretty(p.out()) + " (3 noches)");
        assertThat(detail).contains("Total pagado").contains(mxn(p.quote().total()));
        assertThat(c.hasMovementsLink()).isTrue();
        assertThat(c.movementsLinkText()).isEqualTo("Ver el cargo en Bank");
        assertThat(c.goStays().count()).isEqualTo(1);
        driver.navigate().back();
        confirmation().waitConfirmed();
        assertThat(confirmation().hasMovementsLink()).isTrue();
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.flaky_booking] Si falla la confirmación la reserva se crea y el reintento la duplica (stay.flaky_booking) (CP-STAY-093)")
    void failedConfirmationLeavesNoBookingAndRetryCreatesOne() {
        Plan p = defaultPlan(14);
        long total = p.quote().total();
        StayBookPage b = startBooking(p).fillValid();
        long start = b.walletCents();
        // Semilla elegida para que la primera confirmación caiga en el 35 % de fallos si el defecto está activo.
        b.setSeed(StayModel.seedWhereFirstCallFails());

        StayConfirmationPage c = b.confirmAndWait();
        if (c == null) {
            assertThat(b.payError()).contains("No se pudo confirmar la reserva. Intenta de nuevo.");
            assertThat(b.bookingsInState()).as("reservas tras un fallo de confirmación").isZero();
            assertThat(b.walletCents()).as("saldo tras un fallo de confirmación").isEqualTo(start);
            for (int i = 0; i < 12 && c == null; i++) c = b.confirmAndWait();
            assertThat(c).as("el reintento debe terminar confirmando").isNotNull();
        }
        assertThat(stays().open().count()).as("estancias").isEqualTo(1);
        assertThat(stays().walletCents()).isEqualTo(start - total);
    }
}
