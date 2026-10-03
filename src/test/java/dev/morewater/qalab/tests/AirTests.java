package dev.morewater.qalab.tests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.AirPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de contrato de Air: verifican el comportamiento CORRECTO.
 * Con el usuario «estandar» deben pasar; con otro usuario de prueba fallarán donde haya un defecto.
 * El texto entre corchetes del nombre es el id del defecto que la prueba cubre (así el panel calcula la cobertura).
 */
class AirTests extends BaseTest {

    @Test
    @DisplayName("La búsqueda no acepta el mismo origen y destino")
    void searchRejectsSameOriginAndDestination() {
        AirPage air = new AirPage(driver).open().search("MEX", "MEX", 1);
        assertThat(air.searchError("to")).contains("distinto");
    }

    @Test
    @DisplayName("[air.tax_mismatch] El total incluye tarifa, IVA y TUA")
    void totalIncludesFareVatAndAirportFee() {
        AirPage air = new AirPage(driver).toPayment(1);
        // el resumen puede traer además equipaje o asientos con más espacio, según el asiento elegido
        long expected = air.summaryCents("fare") + air.optionalCents("bags") + air.optionalCents("legroom")
                + air.summaryCents("iva") + air.summaryCents("tua");
        assertThat(air.totalCents()).as("total mostrado").isEqualTo(expected);
    }

    @Test
    @DisplayName("[air.passenger_count] La tarifa se multiplica por el número de pasajeros")
    void fareIsMultipliedByPassengers() {
        AirPage air = new AirPage(driver).open().search("MEX", "MTY", 2);
        long unit = air.firstFlightPrice();
        air.selectFirstFlight().fillPassengers(2).pickFreeSeats(2);
        assertThat(air.summaryCents("fare")).isEqualTo(unit * 2);
    }

    @Test
    @DisplayName("[air.slow_search] La búsqueda de vuelos responde en menos de 3 segundos")
    void searchRespondsQuickly() {
        AirPage air = new AirPage(driver).open();
        long t0 = System.currentTimeMillis();
        air.search("MEX", "MTY", 1).firstFlightPrice();
        assertThat(System.currentTimeMillis() - t0).as("milisegundos").isLessThan(3000);
    }

    @Test
    @DisplayName("[air.visual_price_format] El precio usa el mismo formato en resultados y en el resumen")
    void priceFormatIsConsistent() {
        AirPage air = new AirPage(driver).open().search("MEX", "MTY", 1);
        String inResults = air.firstFlightPriceText();
        air.selectFirstFlight().fillPassengers(1).pickFreeSeats(1);
        String inSummary = air.summaryFareText();
        assertThat(inResults).matches("\\$\\d{1,3}(,\\d{3})*\\.\\d{2}");
        assertThat(inSummary).matches("\\$\\d{1,3}(,\\d{3})*\\.\\d{2}");
    }
}
