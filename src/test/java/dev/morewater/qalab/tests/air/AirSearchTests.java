package dev.morewater.qalab.tests.air;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.air.AirFlowPage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU búsqueda de vuelos y resultados (CP-AIR-001 a 026). */
class AirSearchTests extends BaseTest {
    private static final String NOT_FOUND = "No encontramos ese aeropuerto. Elige uno de la lista.";
    private static final String PAST = "La fecha no puede ser anterior a hoy.";

    private AirFlowPage air() { return new AirFlowPage(driver).open(); }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_001_BusquedaExitosaDeRutaValidaMEXMTY")
    void A_AIR_001_BusquedaExitosaDeRutaValidaMEXMTY() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        assertThat(a.path()).contains("/air/results");
        assertThat(a.resultsTitle()).contains("Ciudad de México (MEX)").contains("Monterrey (MTY)");
        assertThat(a.flightCount()).isBetween(5, 7);
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_002_ResolverAeropuertoPorNombreDeCiudadEnMinusculas")
    void A_AIR_002_ResolverAeropuertoPorNombreDeCiudadEnMinusculas() {
        AirFlowPage a = air().fillSearch("ciudad de méxico", "monterrey", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.currentUrl()).contains("from=MEX").contains("to=MTY");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_003_ResolverAeropuertoConFormatoCiudadCODIGOYCodigoEnMinusculas")
    void A_AIR_003_ResolverAeropuertoConFormatoCiudadCODIGOYCodigoEnMinusculas() {
        AirFlowPage a = air().fillSearch("Cancún (CUN)", "gdl", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.currentUrl()).contains("from=CUN").contains("to=GDL");
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_010_OrigenYDestinoVacios")
    void A_AIR_010_OrigenYDestinoVacios() {
        AirFlowPage a = air();
        a.clearField("air-origin").clearField("air-destination").submitSearch();
        assertThat(a.error("from")).isEqualTo("Escribe el origen.");
        assertThat(a.error("to")).isEqualTo("Escribe el destino.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_011_OrigenInexistenteXXX")
    void A_AIR_011_OrigenInexistenteXXX() {
        AirFlowPage a = air().fillSearch("XXX", "MTY", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("from")).isEqualTo(NOT_FOUND);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_012_DestinoInexistenteNarnia")
    void A_AIR_012_DestinoInexistenteNarnia() {
        AirFlowPage a = air().fillSearch("MEX", "Narnia", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("to")).isEqualTo(NOT_FOUND);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_013_OrigenIgualAlDestino")
    void A_AIR_013_OrigenIgualAlDestino() {
        AirFlowPage a = air().fillSearch("MEX", "MEX", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("to")).isEqualTo("El destino debe ser distinto del origen.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_014_FechaPasada20200101")
    void A_AIR_014_FechaPasada20200101() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", "2020-01-01", 1).submitSearch();
        assertThat(a.error("date")).isEqualTo(PAST);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_015_FechaVacia")
    void A_AIR_015_FechaVacia() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", "", 1).submitSearch();
        assertThat(a.error("date")).isEqualTo("Elige la fecha de salida.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_016_FechaDeAyerValorFronteraRechazada")
    void A_AIR_016_FechaDeAyerValorFronteraRechazada() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", AirFlowPage.iso(-1), 1).submitSearch();
        assertThat(a.error("date")).isEqualTo(PAST);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_017_LosErroresSeCorrigenYLaBusquedaProcede")
    void A_AIR_017_LosErroresSeCorrigenYLaBusquedaProcede() {
        AirFlowPage a = air();
        a.clearField("air-origin").clearField("air-destination");
        a.submitSearch();
        assertThat(a.error("from")).isEqualTo("Escribe el origen.");
        a.fillSearch("MEX", "MTY", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.path()).contains("/air/results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_019_VerElDetalleDeCadaVueloEnResultados")
    void A_AIR_019_VerElDetalleDeCadaVueloEnResultados() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        for (int i = 0; i < a.flightCount(); i++) {
            String tx = a.flightText(i);
            assertThat(tx).as("vuelo " + i).containsPattern("\\d{2}:\\d{2} → \\d{2}:\\d{2}")
                    .containsPattern("MW \\d{4}").containsPattern("\\d+ ?h|\\d+ ?min")
                    .containsPattern("Directo|1 escala").contains("por pasajero").containsPattern("\\$[\\d,]+\\.\\d{2}");
        }
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_020_ResultadosOrdenadosPorHoraDeSalidaPorDefecto")
    void A_AIR_020_ResultadosOrdenadosPorHoraDeSalidaPorDefecto() {
        List<Integer> mins = air().search("MEX", "MTY", 1).departMinutes();
        List<Integer> sorted = new ArrayList<>(mins);
        Collections.sort(sorted);
        assertThat(mins).isEqualTo(sorted);
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_021_OrdenarResultadosPorPrecio")
    void A_AIR_021_OrdenarResultadosPorPrecio() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        List<Long> before = a.prices();
        a.sortBy("precio");
        List<Long> expected = new ArrayList<>(before);
        Collections.sort(expected);
        assertThat(a.prices()).isEqualTo(expected);
        assertThat(a.prices()).hasSameSizeAs(before);
    }

    @Test @Tag("recomendado")
    @DisplayName("A_AIR_022_FiltrarSoloVuelosDirectos")
    void A_AIR_022_FiltrarSoloVuelosDirectos() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        int all = a.flightCount();
        int stops = 0;
        for (int i = 0; i < all; i++) if (a.flightText(i).contains("1 escala")) stops++;
        a.directOnly(true);
        for (int i = 0; i < a.flightCount(); i++) assertThat(a.flightText(i)).contains("Directo").doesNotContain("1 escala");
        assertThat(a.flightCount()).isEqualTo(all - stops);
        a.directOnly(false);
        assertThat(a.flightCount()).isEqualTo(all);
    }

    @Test @Tag("opcional")
    @DisplayName("A_AIR_025_ParametroPaxFueraDeRangoEnLaURLSeLimitaA6")
    void A_AIR_025_ParametroPaxFueraDeRangoEnLaURLSeLimitaA6() {
        AirFlowPage a = new AirFlowPage(driver).openResults("MEX", "MTY", AirFlowPage.iso(7), 9).waitResults();
        assertThat(a.resultsSubtitle()).contains("6 pasajeros");
        a.selectFirstFlight();
        assertThat(driver.findElements(org.openqa.selenium.By.cssSelector("[data-test^='air-passenger-']"))).hasSize(6);
    }

    @Test @Tag("opcional")
    @DisplayName("A_AIR_026_CadaRutaYFechaProduceResultadosDistintosYEstables")
    void A_AIR_026_CadaRutaYFechaProduceResultadosDistintosYEstables() {
        String d1 = AirFlowPage.iso(7), d2 = AirFlowPage.iso(12);
        AirFlowPage a = new AirFlowPage(driver).openResults("MEX", "MTY", d1, 1).waitResults();
        List<String> first = a.flightIds();
        List<Long> firstPrices = a.prices();
        a.openResults("MEX", "CUN", d2, 1).waitResults();
        assertThat(a.flightIds()).isNotEqualTo(first);
        a.openResults("MEX", "MTY", d1, 1).waitResults();
        assertThat(a.flightIds()).isEqualTo(first);
        assertThat(a.prices()).isEqualTo(firstPrices);
    }
}
