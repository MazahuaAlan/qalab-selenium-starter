package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayHotelPage;
import dev.morewater.qalab.pages.stay.StayResultsPage;
import dev.morewater.qalab.pages.stay.StaySearchPage;
import dev.morewater.qalab.stay.StayModel;
import dev.morewater.qalab.stay.StayModel.Hotel;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-01: búsqueda de hoteles. */
class SearchTests extends StayTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_001_BusquedaConValoresPorDefecto")
    void A_STAY_001_BusquedaConValoresPorDefecto() {
        StaySearchPage s = search().open();
        LocalDate in = s.today().plusDays(14), out = s.today().plusDays(17);
        assertThat(s.h1()).isEqualTo("¿Dónde quieres hospedarte?");
        assertThat(s.selectedCity()).isEqualTo("Cancún");
        assertThat(s.adults()).isEqualTo(2);
        assertThat(s.kids()).isEqualTo(0);
        assertThat(s.summary()).isEqualTo("Entrada " + s.pretty(in) + " · salida " + s.pretty(out) + " · 3 noches");
        assertThat(s.nights()).isEqualTo("3 noches");

        StayResultsPage r = s.searchAndWait();
        assertThat(driver.getCurrentUrl()).endsWith("/stay/results/?city=CUN&in=" + in + "&out=" + out + "&adults=2&kids=0");
        assertThat(r.title()).isEqualTo("Hoteles en Cancún");
        assertThat(r.countText()).isEqualTo("8 hoteles");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_002_BusquedaEnCadaUnoDeLosSeisDestinos")
    void A_STAY_002_BusquedaEnCadaUnoDeLosSeisDestinos() {
        StaySearchPage s = search().open();
        assertThat(s.cityOptions()).containsExactlyInAnyOrderElementsOf(CITY_NAMES.values());
        for (String code : StayModel.CITY_CODES) {
            s.open().chooseCity(code);
            StayResultsPage r = s.searchAndWait();
            List<Hotel> expected = StayModel.byPrice(StayModel.hotelsIn(code));
            assertThat(r.title()).isEqualTo("Hoteles en " + CITY_NAMES.get(code));
            assertThat(r.countText()).isEqualTo("8 hoteles");
            assertThat(r.names()).as("hoteles de " + code + " por precio").containsExactlyElementsOf(expected.stream().map(Hotel::name).toList());
            assertThat(r.name(0)).isEqualTo(expected.get(0).name());
            assertThat(r.price(0)).isEqualTo(StayModel.mxn(expected.get(0).baseCents()));
        }
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_003_ValoresFronteraDeHuespedesMinimoYMaximo")
    void A_STAY_003_ValoresFronteraDeHuespedesMinimoYMaximo() {
        StaySearchPage s = search().open();
        s.adultsMinus();
        assertThat(s.adults()).isEqualTo(1);
        assertThat(s.adultsMinusEnabled()).isFalse();
        s.kidsPlus().kidsPlus().kidsPlus();
        assertThat(s.kids()).isEqualTo(3);
        assertThat(s.kidsPlusEnabled()).isFalse();
        assertThat(s.kidsMinusEnabled()).isTrue();
        s.submit();
        StayResultsPage r = new StayResultsPage(driver).waitLoaded();
        assertThat(driver.getCurrentUrl()).contains("adults=1&kids=3");

        s.open();
        s.adultsPlus().adultsPlus(); // la página se reinició con 2 adultos: 2 → 4
        assertThat(s.adults()).isEqualTo(4);
        assertThat(s.adultsPlusEnabled()).isFalse();
        assertThat(s.kids()).isEqualTo(0);
        s.submit();
        r.waitLoaded();
        assertThat(driver.getCurrentUrl()).contains("adults=4&kids=0");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_004_RechazarMasDe4HuespedesPorHabitacion")
    void A_STAY_004_RechazarMasDe4HuespedesPorHabitacion() {
        StaySearchPage s = search().open();
        s.adultsPlus().adultsPlus();
        assertThat(s.adultsPlusEnabled()).isFalse();
        s.kidsPlus();
        assertThat(s.kids()).isEqualTo(1);
        assertThat(s.kidsPlusEnabled()).isTrue();
        s.submit();
        assertThat(s.error()).isEqualTo("Máximo 4 huéspedes por habitación; para más personas reserva varias habitaciones.");
        assertThat(driver.getCurrentUrl()).doesNotContain("/results/").contains("/stay/");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_006_BordeLaBusquedaConservaLosParametrosEnLaURLDeResultadosYDe")
    void A_STAY_006_BordeLaBusquedaConservaLosParametrosEnLaURLDeResultadosYDe() {
        StaySearchPage s = search().open();
        LocalDate in = s.today().plusDays(14), out = s.today().plusDays(17);
        s.adultsPlus().kidsPlus();
        StayResultsPage r = s.searchAndWait();
        assertThat(driver.getCurrentUrl()).contains("adults=3&kids=1");
        assertThat(r.subtitle()).isEqualTo(s.pretty(in) + " → " + s.pretty(out) + " · 3 noches · 4 huéspedes");

        Hotel first = StayModel.byPrice(StayModel.hotelsIn("CUN")).get(0);
        StayHotelPage h = r.view(0);
        assertThat(driver.getCurrentUrl()).endsWith("/stay/hotel/?id=" + first.id() + "&in=" + in + "&out=" + out + "&adults=3&kids=1");
        assertThat(h.subtitle()).contains("(3 noches) · 4 huéspedes");
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[stay.slow_results] A_STAY_090_LaBusquedaDeHotelesTarda4SStaySlowResults")
    void A_STAY_090_LaBusquedaDeHotelesTarda4SStaySlowResults() {
        StaySearchPage s = search().open();
        long t0 = System.nanoTime();
        s.submit();
        StayResultsPage r = new StayResultsPage(driver).waitLoaded();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(ms).as("ms hasta ver los resultados (esperado < 3500)").isLessThan(3500);
        assertThat(r.count()).isEqualTo(8);
    }
}
