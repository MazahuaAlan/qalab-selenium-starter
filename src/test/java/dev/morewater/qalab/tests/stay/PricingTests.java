package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayBookPage;
import dev.morewater.qalab.pages.stay.StayHotelPage;
import dev.morewater.qalab.pages.stay.StayStaysPage;
import dev.morewater.qalab.stay.StayModel;
import dev.morewater.qalab.stay.StayModel.Quote;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-07: desglose de precios e impuestos (IVA 16 %, impuesto sobre hospedaje 3 %). */
class PricingTests extends StayTest {

    private static String mxn(long c) { return StayModel.mxn(c); }

    private void assertSummaryMatches(StayBookPage b, Plan p) {
        Quote q = p.quote();
        assertThat(b.sumNightsLabel()).isEqualTo(nightsLabel(p.nights()));
        assertThat(b.sumBaseLabel()).isEqualTo(mxn(p.room().rateCents()) + " × " + p.nights());
        assertThat(b.sumBase()).isEqualTo(q.base());
        assertThat(b.sumIvaLabel()).isEqualTo("IVA (16 %)");
        assertThat(b.sumIva()).isEqualTo(q.iva());
        assertThat(b.sumIshLabel()).isEqualTo("Impuesto sobre hospedaje (3 %)");
        assertThat(b.sumIsh()).isEqualTo(q.ish());
        assertThat(b.sumTotal()).isEqualTo(q.total());
        assertThat(b.confirmText()).isEqualTo("Pagar " + mxn(q.total()) + " con billetera");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_044_ElDesgloseDeImpuestosCuadra")
    void A_STAY_044_ElDesgloseDeImpuestosCuadra() {
        Plan p = defaultPlan(14);
        StayBookPage b = startBooking(p);
        assertSummaryMatches(b, p);
        assertThat(b.sumBase() + b.sumIva() + b.sumIsh()).as("base + IVA + ISH").isEqualTo(b.sumTotal());
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_046_CoherenciaDePreciosEntreHotelResumenYConfirmacion")
    void A_STAY_046_CoherenciaDePreciosEntreHotelResumenYConfirmacion() {
        Plan p = defaultPlan(14);
        StayHotelPage h = openHotel(p);
        Matcher m = Pattern.compile("total con impuestos (\\$[\\d,]+\\.\\d{2})").matcher(h.roomText(p.roomIdx()));
        assertThat(m.find()).isTrue();
        String hotelTotal = m.group(1);
        assertThat(hotelTotal).isEqualTo(mxn(p.quote().total()));

        StayBookPage b = h.reserve(p.roomIdx()).fillValid();
        assertThat(b.sumTotalText()).isEqualTo(hotelTotal);
        assertThat(b.confirmText()).isEqualTo("Pagar " + hotelTotal + " con billetera");

        var c = b.confirmOk();
        assertThat(c.totalText()).isEqualTo(hotelTotal);
        StayStaysPage s = c.goStays();
        assertThat(s.dates()).endsWith(hotelTotal);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_047_LaCantidadDeNochesDelResumenCoincideConLasFechasElegidas")
    void A_STAY_047_LaCantidadDeNochesDelResumenCoincideConLasFechasElegidas() {
        Plan p = plan("Torre Nimbo", 0, 14, 5);
        StayBookPage b = startBooking(p);
        assertSummaryMatches(b, p);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.nights_mismatch] A_STAY_098_ElTotalCuentaUnaNocheMenosDeLasElegidasStayNightsMismatch")
    void A_STAY_098_ElTotalCuentaUnaNocheMenosDeLasElegidasStayNightsMismatch() {
        Plan p = defaultPlan(14);
        StayHotelPage h = openHotel(p);
        String cardTotal = mxn(p.quote().total());
        assertThat(h.roomText(p.roomIdx())).contains(cardTotal);
        StayBookPage b = h.reserve(p.roomIdx());
        assertThat(b.sumNightsLabel()).isEqualTo("3 noches");
        assertThat(b.sumBaseLabel()).isEqualTo(mxn(p.room().rateCents()) + " × 3");
        assertThat(b.sumBase()).isEqualTo(p.room().rateCents() * 3);
        assertThat(b.sumTotalText()).as("total del resumen = total de la tarjeta de habitación").isEqualTo(cardTotal);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.tax_double] A_STAY_099_ElImpuestoSobreHospedajeSeSumaDosVecesStayTaxDouble")
    void A_STAY_099_ElImpuestoSobreHospedajeSeSumaDosVecesStayTaxDouble() {
        Plan p = defaultPlan(14);
        Quote q = p.quote();
        StayBookPage b = startBooking(p);
        assertThat(b.sumBase()).isEqualTo(q.base());
        assertThat(b.sumIva()).isEqualTo(q.iva());
        assertThat(b.sumIsh()).isEqualTo(q.ish());
        assertThat(b.sumTotalText()).as("total = base + IVA + ISH").isEqualTo(mxn(q.base() + q.iva() + q.ish()));
        assertThat(b.confirmText()).isEqualTo("Pagar " + mxn(q.total()) + " con billetera");
    }
}
