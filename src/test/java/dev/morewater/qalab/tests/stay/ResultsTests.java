package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayResultsPage;
import dev.morewater.qalab.pages.stay.StaySearchPage;
import dev.morewater.qalab.stay.StayModel;
import dev.morewater.qalab.stay.StayModel.Hotel;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-03: listado, filtros y orden de resultados (Cancún, búsqueda por defecto). */
class ResultsTests extends StayTest {
    private final List<Hotel> all = StayModel.hotelsIn("CUN");

    private StayResultsPage openCancun() {
        LocalDate t = today();
        return results().open("CUN", t.plusDays(14).toString(), t.plusDays(17).toString(), 2, 0);
    }

    private static List<String> names(List<Hotel> l) { return l.stream().map(Hotel::name).toList(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Listado inicial de Cancún ordenado por precio (CP-STAY-016)")
    void initialListSortedByPrice() {
        StaySearchPage s = search().open();
        s.setLatency(1500); // hace observable el estado de carga
        s.submit();
        StayResultsPage r = results();
        assertThat(r.becomesTrue(r::loadingVisible, 6, 50)).as("indicador «Buscando hoteles…»").isTrue();
        r.waitLoaded();
        List<Hotel> expected = StayModel.byPrice(all);
        assertThat(r.countText()).isEqualTo("8 hoteles");
        assertThat(r.shown()).isEqualTo(8);
        assertThat(r.names()).containsExactlyElementsOf(names(expected));
        assertThat(r.prices()).containsExactlyElementsOf(expected.stream().map(h -> StayModel.mxn(h.baseCents())).toList());
        List<Long> cents = r.prices().stream().map(p -> cents(p)).toList();
        assertThat(cents).isSorted();
    }

    private static long cents(String money) { return dev.morewater.qalab.pages.BasePage.cents(money); }

    @Test
    @Tag("recomendado")
    @DisplayName("Filtro de precio máximo: valor frontera inclusivo (CP-STAY-017)")
    void maxPriceFilterIsInclusive() {
        StayResultsPage r = openCancun();
        long cheapest = StayModel.byPrice(all).get(0).baseCents();
        int limit = (int) (cheapest / 100);
        assertThat(limit % 100).as("el filtro avanza de 100 en 100").isZero();
        assertThat(r.priceValue()).isEqualTo("$8,000");

        r.setMaxPrice(limit);
        int n = (int) all.stream().filter(h -> h.baseCents() <= limit * 100L).count();
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(n)));
        assertThat(r.priceValue()).isEqualTo(pesos(limit));
        assertThat(n).as("el hotel de precio igual al límite se incluye").isGreaterThanOrEqualTo(1);

        r.setMaxPrice(limit - 100);
        r.eventually(() -> assertThat(r.countText()).isEqualTo("0 hoteles"));
        assertThat(r.emptyVisible()).isTrue();
        assertThat(r.emptyText()).startsWith("Ningún hotel cumple esos filtros");

        r.setMaxPrice(1500);
        int n1500 = (int) all.stream().filter(h -> h.baseCents() <= 150_000).count();
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(n1500)));
        assertThat(r.prices().stream().map(ResultsTests::cents)).allSatisfy(c -> assertThat(c).isLessThanOrEqualTo(150_000L));

        r.setMaxPrice(8000);
        r.eventually(() -> assertThat(r.countText()).isEqualTo("8 hoteles"));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Filtro por estrellas: OR entre categorías (CP-STAY-018)")
    void starsFilterIsOr() {
        StayResultsPage r = openCancun();
        int n5 = (int) all.stream().filter(h -> h.stars() == 5).count();
        int n4 = (int) all.stream().filter(h -> h.stars() == 4).count();
        r.toggleStars(5);
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(n5)));
        assertThat(r.starLabels()).hasSize(n5).allMatch("5 estrellas"::equals);
        r.toggleStars(4);
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(n5 + n4)));
        r.toggleStars(5);
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(n4)));
        assertThat(r.starLabels()).hasSize(n4).allMatch("4 estrellas"::equals);
        r.toggleStars(4);
        r.eventually(() -> assertThat(r.countText()).isEqualTo("8 hoteles"));
    }

    private long countWith(String... amenities) { return all.stream().filter(h -> h.amenities().containsAll(List.of(amenities))).count(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("Filtro de servicios: AND entre amenidades (CP-STAY-019)")
    void amenitiesFilterIsAnd() {
        StayResultsPage r = openCancun();
        r.toggleAmenity("wifi");
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel((int) countWith("wifi"))));
        assertThat(r.cardTexts()).allSatisfy(t -> assertThat(t).contains("Wi-Fi"));
        r.toggleAmenity("pool");
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel((int) countWith("wifi", "pool"))));
        assertThat(r.cardTexts()).allSatisfy(t -> assertThat(t).contains("Wi-Fi").contains("Alberca"));
        r.toggleAmenity("wifi");
        r.toggleAmenity("pool");
        r.toggleAmenity("spa");
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel((int) countWith("spa"))));
        assertThat(r.cardTexts()).allSatisfy(t -> assertThat(t).contains("Spa"));
        int spaOnly = r.count();
        r.toggleAmenity("parking");
        r.toggleAmenity("breakfast");
        int expected = (int) countWith("spa", "parking", "breakfast");
        r.eventually(() -> assertThat(r.countText()).isEqualTo(countLabel(expected)));
        assertThat(r.count()).isLessThanOrEqualTo(spaOnly);
        assertThat(r.cardTexts()).allSatisfy(t -> assertThat(t).contains("Spa").contains("Estacionamiento").contains("Desayuno incluido"));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Ordenar por calificación y las estrellas siguen siendo la categoría (CP-STAY-020)")
    void sortByRatingKeepsStarCategory() {
        StayResultsPage r = openCancun();
        List<Hotel> byRating = StayModel.byRating(all);
        r.sortBy("calificacion");
        r.eventually(() -> assertThat(r.names()).containsExactlyElementsOf(names(byRating)));
        assertThat(r.rating(0)).isEqualTo(String.format(Locale.US, "%.1f", byRating.get(0).rating()));
        assertThat(r.ratings()).containsExactlyElementsOf(byRating.stream().map(h -> String.format(Locale.US, "%.1f", h.rating())).toList());
        List<Double> ratings = r.ratings().stream().map(Double::parseDouble).toList();
        assertThat(ratings).isSortedAccordingTo(java.util.Comparator.reverseOrder());

        Hotel casaCondor = StayModel.hotelByName("CUN", "Casa Cóndor");
        int idx = r.names().indexOf(casaCondor.name());
        assertThat(r.stars(idx)).as("categoría real de Casa Cóndor").isEqualTo(casaCondor.stars() + " estrellas");

        r.sortBy("precio");
        r.eventually(() -> assertThat(r.names()).containsExactlyElementsOf(names(StayModel.byPrice(all))));
        assertThat(r.name(0)).isEqualTo(StayModel.byPrice(all).get(0).name());
    }

    @Test
    @Tag("opcional")
    @Tag("bug")
    @DisplayName("[stay.visual_stars] Las estrellas se redondean hacia arriba (stay.visual_stars) (CP-STAY-095)")
    void starsAreTheHotelCategory() {
        StayResultsPage r = openCancun();
        assertThat(r.shown()).isEqualTo(8);
        for (int i = 0; i < 8; i++) {
            Hotel h = StayModel.hotelByName("CUN", r.name(i));
            assertThat(r.stars(i)).as("estrellas de " + h.name() + " (calificación " + h.rating() + ")").isEqualTo(h.stars() + " estrellas");
        }
        r.toggleStars(5);
        long n5 = all.stream().filter(h -> h.stars() == 5).count();
        r.eventually(() -> assertThat(r.shown()).isEqualTo((int) n5));
        assertThat(r.starLabels()).allMatch("5 estrellas"::equals);
        assertThat(r.names()).containsExactlyInAnyOrderElementsOf(names(all.stream().filter(h -> h.stars() == 5).toList()));
    }
}
