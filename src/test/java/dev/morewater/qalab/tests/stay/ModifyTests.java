package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayStaysPage;
import dev.morewater.qalab.pages.stay.WalletPage;
import dev.morewater.qalab.stay.StayModel;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-10: modificar fechas de una estancia confirmada. */
class ModifyTests extends StayTest {

    private static String mxn(long c) { return StayModel.mxn(c); }

    private long totalFor(Plan p, int nights) { return StayModel.quote(p.room().rateCents(), nights).total(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Modificar fechas a más noches cobra la diferencia (CP-STAY-060)")
    void moreNightsChargesDifference() {
        Plan p = defaultPlan(14);
        String code = completeBooking(p);
        long afterBooking = search().walletCents();
        StayStaysPage s = stays().open();
        s.openModify();
        assertThat(s.modalRole()).isEqualTo("dialog");
        assertThat(s.modalTitle()).isEqualTo("Modificar fechas de " + code);
        assertThat(s.calendar.dayPressed(p.in())).as("entrada precargada").isTrue();
        assertThat(s.calendar.dayPressed(p.out())).as("salida precargada").isTrue();

        LocalDate newIn = p.in().minusDays(3), newOut = p.in().plusDays(2); // 5 noches
        s.calendar.selectRange(newIn, newOut);
        long diff = totalFor(p, 5) - totalFor(p, 3);
        s.eventually(() -> assertThat(s.modifyDiff()).isEqualTo("5 noches · se cobrarán " + mxn(diff) + " más"));

        s.saveModify().waitModalClosed();
        assertThat(s.message()).isEqualTo("Fechas de " + code + " actualizadas.");
        assertThat(s.dates()).endsWith("5 noches · " + mxn(totalFor(p, 5)));
        assertThat(s.walletCents()).isEqualTo(afterBooking - diff);
        s.eventually(() -> assertThat(s.walletChipCents()).isEqualTo(afterBooking - diff));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Modificar a menos noches reembolsa la diferencia (CP-STAY-061)")
    void fewerNightsRefundsDifference() {
        Plan p = defaultPlan(14);
        String code = completeBooking(p);
        long afterBooking = search().walletCents();
        StayStaysPage s = stays().open();
        s.openModify();
        s.calendar.selectRange(p.in(), p.in().plusDays(1));
        long refund = totalFor(p, 3) - totalFor(p, 1);
        s.eventually(() -> assertThat(s.modifyDiff()).isEqualTo("1 noche · se reembolsarán " + mxn(refund)));

        s.saveModify().waitModalClosed();
        assertThat(s.message()).isEqualTo("Fechas de " + code + " actualizadas.");
        assertThat(s.dates()).endsWith("1 noche · " + mxn(totalFor(p, 1)));
        assertThat(s.walletCents()).isEqualTo(afterBooking + refund);
        s.eventually(() -> assertThat(s.walletChipCents()).isEqualTo(afterBooking + refund));

        WalletPage w = wallet().open();
        w.waitRows();
        assertThat(w.label(0)).isEqualTo("Reembolso por cambio de fechas " + code);
        assertThat(w.amount(0)).isEqualTo(mxn(refund));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Validar la selección de fechas al modificar (CP-STAY-063)")
    void modifyValidatesDateSelection() {
        Plan p = defaultPlan(14);
        completeBooking(p);
        StayStaysPage s = stays().open();
        String originalDates = s.dates();
        s.openModify();

        s.calendar.pick(p.in().plusDays(3)); // solo entrada
        s.eventually(() -> assertThat(s.modifyDiff()).isEqualTo("Elige las nuevas fechas"));
        s.saveModify();
        assertThat(s.modifyError()).isEqualTo("Elige la nueva entrada y salida.");
        assertThat(s.dates()).isEqualTo(originalDates);

        s.calendar.selectRange(p.in(), p.in().plusDays(15)); // 15 noches
        s.saveModify();
        s.eventually(() -> assertThat(s.modifyError()).isEqualTo("La estancia debe ser de 1 a 14 noches."));
        assertThat(s.dates()).isEqualTo(originalDates);

        s.calendar.selectRange(p.in(), p.in().plusDays(14)); // 14 noches: válido
        long diff = totalFor(p, 14) - totalFor(p, 3);
        s.eventually(() -> assertThat(s.modifyDiff()).isEqualTo("14 noches · se cobrarán " + mxn(diff) + " más"));
        s.setWallet(diff - 100); // el saldo no alcanza para pagar la diferencia
        s.eventually(() -> assertThat(s.walletChipCents()).isEqualTo(diff - 100));
        s.saveModify();
        s.eventually(() -> assertThat(s.modifyError()).isEqualTo("Saldo insuficiente para pagar la diferencia."));
        assertThat(s.dates()).isEqualTo(originalDates);
    }
}
