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
    @DisplayName("Búsqueda exitosa de ruta válida MEX→MTY (CP-AIR-001)")
    void validRouteSearch() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        assertThat(a.path()).contains("/air/results");
        assertThat(a.resultsTitle()).contains("Ciudad de México (MEX)").contains("Monterrey (MTY)");
        assertThat(a.flightCount()).isBetween(5, 7);
    }

    @Test @Tag("recomendado")
    @DisplayName("Resolver aeropuerto por nombre de ciudad en minúsculas (CP-AIR-002)")
    void resolvesCityNameLowercase() {
        AirFlowPage a = air().fillSearch("ciudad de méxico", "monterrey", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.currentUrl()).contains("from=MEX").contains("to=MTY");
    }

    @Test @Tag("recomendado")
    @DisplayName("Resolver aeropuerto con formato 'Ciudad (CÓDIGO)' y código en minúsculas (CP-AIR-003)")
    void resolvesListFormatAndLowercaseCode() {
        AirFlowPage a = air().fillSearch("Cancún (CUN)", "gdl", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.currentUrl()).contains("from=CUN").contains("to=GDL");
    }

    @Test @Tag("obligatorio")
    @DisplayName("Origen y destino vacíos (CP-AIR-010)")
    void emptyOriginAndDestination() {
        AirFlowPage a = air();
        a.clearField("air-origin").clearField("air-destination").submitSearch();
        assertThat(a.error("from")).isEqualTo("Escribe el origen.");
        assertThat(a.error("to")).isEqualTo("Escribe el destino.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("Origen inexistente 'XXX' (CP-AIR-011)")
    void unknownOrigin() {
        AirFlowPage a = air().fillSearch("XXX", "MTY", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("from")).isEqualTo(NOT_FOUND);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("Destino inexistente 'Narnia' (CP-AIR-012)")
    void unknownDestination() {
        AirFlowPage a = air().fillSearch("MEX", "Narnia", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("to")).isEqualTo(NOT_FOUND);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("Origen igual al destino (CP-AIR-013)")
    void sameOriginAndDestination() {
        AirFlowPage a = air().fillSearch("MEX", "MEX", AirFlowPage.iso(7), 1).submitSearch();
        assertThat(a.error("to")).isEqualTo("El destino debe ser distinto del origen.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("Fecha pasada 2020-01-01 (CP-AIR-014)")
    void pastDate() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", "2020-01-01", 1).submitSearch();
        assertThat(a.error("date")).isEqualTo(PAST);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("Fecha vacía (CP-AIR-015)")
    void emptyDate() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", "", 1).submitSearch();
        assertThat(a.error("date")).isEqualTo("Elige la fecha de salida.");
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("Fecha de ayer (valor frontera) rechazada (CP-AIR-016)")
    void yesterdayRejected() {
        AirFlowPage a = air().fillSearch("MEX", "MTY", AirFlowPage.iso(-1), 1).submitSearch();
        assertThat(a.error("date")).isEqualTo(PAST);
        assertThat(a.path()).doesNotContain("results");
    }

    @Test @Tag("recomendado")
    @DisplayName("Los errores se corrigen y la búsqueda procede (CP-AIR-017)")
    void errorsCanBeCorrected() {
        AirFlowPage a = air();
        a.clearField("air-origin").clearField("air-destination");
        a.submitSearch();
        assertThat(a.error("from")).isEqualTo("Escribe el origen.");
        a.fillSearch("MEX", "MTY", AirFlowPage.iso(7), 1).submitSearch().waitResults();
        assertThat(a.path()).contains("/air/results");
    }

    @Test @Tag("obligatorio")
    @DisplayName("Ver el detalle de cada vuelo en resultados (CP-AIR-019)")
    void flightDetailsInResults() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        for (int i = 0; i < a.flightCount(); i++) {
            String tx = a.flightText(i);
            assertThat(tx).as("vuelo " + i).containsPattern("\\d{2}:\\d{2} → \\d{2}:\\d{2}")
                    .containsPattern("MW \\d{4}").containsPattern("\\d+ ?h|\\d+ ?min")
                    .containsPattern("Directo|1 escala").contains("por pasajero").containsPattern("\\$[\\d,]+\\.\\d{2}");
        }
    }

    @Test @Tag("recomendado")
    @DisplayName("Resultados ordenados por hora de salida por defecto (CP-AIR-020)")
    void sortedByDepartureByDefault() {
        List<Integer> mins = air().search("MEX", "MTY", 1).departMinutes();
        List<Integer> sorted = new ArrayList<>(mins);
        Collections.sort(sorted);
        assertThat(mins).isEqualTo(sorted);
    }

    @Test @Tag("recomendado")
    @DisplayName("Ordenar resultados por precio (CP-AIR-021)")
    void sortByPrice() {
        AirFlowPage a = air().search("MEX", "MTY", 1);
        List<Long> before = a.prices();
        a.sortBy("precio");
        List<Long> expected = new ArrayList<>(before);
        Collections.sort(expected);
        assertThat(a.prices()).isEqualTo(expected);
        assertThat(a.prices()).hasSameSizeAs(before);
    }

    @Test @Tag("recomendado")
    @DisplayName("Filtrar solo vuelos directos (CP-AIR-022)")
    void directOnlyFilter() {
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
    @DisplayName("Parámetro pax fuera de rango en la URL se limita a 6 (CP-AIR-025)")
    void paxParamIsCappedAtSix() {
        AirFlowPage a = new AirFlowPage(driver).openResults("MEX", "MTY", AirFlowPage.iso(7), 9).waitResults();
        assertThat(a.resultsSubtitle()).contains("6 pasajeros");
        a.selectFirstFlight();
        assertThat(driver.findElements(org.openqa.selenium.By.cssSelector("[data-test^='air-passenger-']"))).hasSize(6);
    }

    @Test @Tag("opcional")
    @DisplayName("Cada ruta y fecha produce resultados distintos y estables (CP-AIR-026)")
    void resultsAreDeterministic() {
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
