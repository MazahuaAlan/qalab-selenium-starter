package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayStaysPage;
import dev.morewater.qalab.pages.stay.WalletPage;
import dev.morewater.qalab.stay.StayModel;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-11: cancelación y política de reembolso (gratis con 3 o más días; si no, se pierde la primera noche). */
class CancelTests extends StayTest {

    private static String mxn(long c) { return StayModel.mxn(c); }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_067_CancelacionGratuitaCon14DiasDeAnticipacion")
    void A_STAY_067_CancelacionGratuitaCon14DiasDeAnticipacion() {
        Plan p = defaultPlan(14);
        long total = p.quote().total();
        long start = search().walletCents();
        String code = completeBooking(p);
        StayStaysPage s = stays().open().openCancel();
        assertThat(s.modalTitle()).isEqualTo("¿Cancelar la reserva " + code + "?");
        assertThat(s.cancelPolicy()).isEqualTo("Cancelación gratuita: recibirás " + mxn(total) + " en tu billetera.");
        s.confirmCancel().waitModalClosed();
        assertThat(s.message()).isEqualTo("Reserva " + code + " cancelada.");
        assertThat(s.status()).isEqualTo("cancelada");
        assertThat(s.hasModify()).isFalse();
        assertThat(s.hasCancel()).isFalse();
        assertThat(s.walletCents()).isEqualTo(start);
        s.eventually(() -> assertThat(s.walletChipCents()).isEqualTo(start));

        WalletPage w = wallet().open();
        w.waitRows();
        assertThat(w.label(0)).isEqualTo("Reembolso estancia " + code);
        assertThat(w.amount(0)).isEqualTo(mxn(total));
    }

    /** Reserva con entrada mañana (3 noches), cancela y devuelve {saldo inicial, saldo tras reservar, reembolso anunciado}. */
    private long[] lateCancellation(Plan p, String expectedPolicy) {
        long start = search().walletCents();
        String code = completeBooking(p);
        StayStaysPage s = stays().open();
        long afterBooking = s.walletCents();
        s.openCancel();
        String policy = s.cancelPolicy();
        if (expectedPolicy != null) assertThat(policy).isEqualTo(expectedPolicy);
        Matcher m = Pattern.compile("recibirás (\\$[\\d,]+\\.\\d{2})").matcher(policy);
        assertThat(m.find()).as("monto anunciado en: " + policy).isTrue();
        long announced = dev.morewater.qalab.pages.BasePage.cents(m.group(1));
        s.confirmCancel().waitModalClosed();
        assertThat(s.message()).isEqualTo("Reserva " + code + " cancelada.");
        assertThat(s.status()).isEqualTo("cancelada");
        return new long[] {start, afterBooking, announced};
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_069_CancelacionTardiaPierdeLaPrimeraNoche")
    void A_STAY_069_CancelacionTardiaPierdeLaPrimeraNoche() {
        LocalDate tomorrow = today().plusDays(1);
        Plan p = planOn(tomorrow, 3);
        long total = p.quote().total();
        long refund = StayModel.refund(total, 3, tomorrow, today());
        long[] r = lateCancellation(p, "Cancelación tardía: se cobra la primera noche y recibirás " + mxn(refund) + ".");
        assertThat(r[2]).isEqualTo(refund);
        assertThat(stays().walletCents()).as("saldo final").isEqualTo(r[0] - total + refund);
        stays().eventually(() -> assertThat(stays().walletChipCents()).isEqualTo(r[0] - total + refund));
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.refund_wrong] A_STAY_100_ElReembolsoIgnoraLaPoliticaStayRefundWrong")
    void A_STAY_100_ElReembolsoIgnoraLaPoliticaStayRefundWrong() {
        LocalDate tomorrow = today().plusDays(1);
        Plan p = planOn(tomorrow, 3);
        long[] r = lateCancellation(p, null);
        assertThat(stays().walletCents() - r[1]).as("monto acreditado = anunciado").isEqualTo(r[2]);
    }
}
