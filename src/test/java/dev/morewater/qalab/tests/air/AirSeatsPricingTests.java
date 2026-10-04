package dev.morewater.qalab.tests.air;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.air.AirFlowPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU mapa de asientos y desglose de precios (CP-AIR-050 a 067). */
class AirSeatsPricingTests extends BaseTest {
    private static final String PRICE_FORMAT = "\\$\\d{1,3}(,\\d{3})*\\.\\d{2}";
    private static final long TUA = 42_000;

    /** IVA = 16 % del subtotal (tarifa + extras), redondeado al centavo, igual que lib/air.ts. */
    private static long iva(long subtotal) { return Math.round(subtotal * 0.16); }

    private AirFlowPage atSeats(int pax) {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", pax).selectFirstFlight().fillAllValid(pax).submitPassengers();
        return a.waitPath("/air/seats").waitSeatMap();
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_050_MapaDeAsientosEstructuraYEncabezadoABCDEF")
    void A_AIR_050_MapaDeAsientosEstructuraYEncabezadoABCDEF() {
        AirFlowPage a = atSeats(1);
        assertThat(a.headerLetters()).containsExactly("A", "B", "C", "D", "E", "F");
        assertThat(a.exists("seat-1A")).isTrue();
        assertThat(a.exists("seat-24F")).isTrue();
        assertThat(a.exists("seat-25A")).isFalse();
        assertThat(a.seatButtons()).isEqualTo(144);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_057_LosAsientosDeMasEspacioSeCobranEnElResumen")
    void A_AIR_057_LosAsientosDeMasEspacioSeCobranEnElResumen() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1);
        long fare = a.firstFlightPrice();
        a.selectFirstFlight().fillAllValid(1).submitPassengers().waitPath("/air/seats").waitSeatMap();
        String seat = a.freeSeats(true).get(0);
        a.chooseSeat(seat);
        assertThat(a.seatCountText()).startsWith("1 de 1");
        a.nextToPayment();
        long subtotal = fare + 35_000;
        assertThat(a.sum("legroom")).isEqualTo(35_000);
        assertThat(a.sum("iva")).isEqualTo(iva(subtotal));
        assertThat(a.total()).isEqualTo(subtotal + iva(subtotal) + TUA);
    }

    @Test @Tag("bug") @Tag("recomendado")
    @DisplayName("[air.visual_seat_labels] A_AIR_059_PersonaVisualLetrasDYEIntercambiadasEnElEncabezadoDelMapa")
    void A_AIR_059_PersonaVisualLetrasDYEIntercambiadasEnElEncabezadoDelMapa() {
        AirFlowPage a = atSeats(1);
        assertThat(a.headerLetters()).containsExactly("A", "B", "C", "D", "E", "F");
        assertThat(a.seatLabel("5D")).startsWith("Asiento 5D");
        assertThat(a.seatLabel("5E")).startsWith("Asiento 5E");
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_060_DesgloseCorrectoPara1PasajeroSinExtras")
    void A_AIR_060_DesgloseCorrectoPara1PasajeroSinExtras() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1);
        long fare = a.firstFlightPrice();
        a.selectFirstFlight().fillAllValid(1).submitPassengers().waitPath("/air/seats").chooseStandardSeats(1).nextToPayment();
        assertThat(a.textOf("air-sum-fare")).contains("Tarifa × 1");
        assertThat(a.sum("fare")).isEqualTo(fare);
        assertThat(a.sum("iva")).isEqualTo(iva(fare));
        assertThat(a.sum("tua")).isEqualTo(TUA);
        assertThat(a.textOf("air-sum-tua")).contains("× 1");
        long expected = fare + iva(fare) + TUA;
        assertThat(a.total()).isEqualTo(expected);
        assertThat(a.textOf("air-pay")).isEqualTo("Pagar " + a.textOf("air-sum-total"));
        assertThat(a.exists("air-sum-bags")).isFalse();
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_061_DesgloseCorrectoPara2Pasajeros")
    void A_AIR_061_DesgloseCorrectoPara2Pasajeros() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 2);
        long unit = a.firstFlightPrice();
        a.selectFirstFlight().fillAllValid(2).submitPassengers().waitPath("/air/seats").chooseStandardSeats(2).nextToPayment();
        assertThat(a.textOf("air-sum-fare")).contains("Tarifa × 2");
        assertThat(a.sum("fare")).isEqualTo(unit * 2);
        assertThat(a.sum("iva")).isEqualTo(iva(unit * 2));
        assertThat(a.sum("tua")).isEqualTo(TUA * 2);
        assertThat(a.total()).isEqualTo(unit * 2 + iva(unit * 2) + TUA * 2);
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_062_Equipaje1MaletaY1AsientoConMasEspacio")
    void A_AIR_062_Equipaje1MaletaY1AsientoConMasEspacio() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1);
        long fare = a.firstFlightPrice();
        a.selectFirstFlight().fillAllValid(1).setBags(0, 1).submitPassengers().waitPath("/air/seats").waitSeatMap();
        a.chooseSeat(a.freeSeats(true).get(0)).nextToPayment();
        long subtotal = fare + 48_000 + 35_000;
        assertThat(a.sum("bags")).isEqualTo(48_000);
        assertThat(a.sum("legroom")).isEqualTo(35_000);
        assertThat(a.sum("iva")).isEqualTo(iva(subtotal));
        assertThat(a.total()).isEqualTo(subtotal + iva(subtotal) + TUA);
    }

    @Test @Tag("bug") @Tag("opcional")
    @DisplayName("[air.visual_price_format] A_AIR_065_PersonaVisualFormatoDePrecioEuropeoEnResultados")
    void A_AIR_065_PersonaVisualFormatoDePrecioEuropeoEnResultados() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 1);
        assertThat(a.priceText(0)).matches(PRICE_FORMAT);
        a.selectFirstFlight().fillAllValid(1).submitPassengers().waitPath("/air/seats").chooseStandardSeats(1).nextToPayment();
        assertThat(a.sumFareText()).matches(PRICE_FORMAT);
    }

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[air.passenger_count] A_AIR_066_PersonaDescuadreElPrecioNoSeMultiplicaPorElNumeroDePasajeros")
    void A_AIR_066_PersonaDescuadreElPrecioNoSeMultiplicaPorElNumeroDePasajeros() {
        AirFlowPage a = new AirFlowPage(driver).open().search("MEX", "MTY", 2);
        long unit = a.firstFlightPrice();
        a.selectFirstFlight().fillAllValid(2).submitPassengers().waitPath("/air/seats").chooseStandardSeats(2).nextToPayment();
        assertThat(a.textOf("air-sum-fare")).contains("Tarifa × 2");
        assertThat(a.sum("fare")).isEqualTo(unit * 2);
        assertThat(a.total()).isEqualTo(unit * 2 + iva(unit * 2) + TUA * 2);
    }

    @Test @Tag("bug") @Tag("obligatorio")
    @DisplayName("[air.tax_mismatch] A_AIR_067_PersonaDescuadreElTotalOmiteLaTUA")
    void A_AIR_067_PersonaDescuadreElTotalOmiteLaTUA() {
        AirFlowPage a = new AirFlowPage(driver).toPayment(1);
        long expected = a.sum("fare") + a.sumOpt("bags") + a.sumOpt("legroom") + a.sum("iva") + a.sum("tua");
        assertThat(a.total()).as("total = suma del desglose").isEqualTo(expected);
        assertThat(a.textOf("air-pay")).isEqualTo("Pagar " + a.textOf("air-sum-total"));
    }
}
