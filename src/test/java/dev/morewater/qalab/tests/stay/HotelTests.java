package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.pages.stay.StayHotelPage;
import dev.morewater.qalab.pages.stay.StayResultsPage;
import dev.morewater.qalab.stay.StayModel;
import dev.morewater.qalab.stay.StayModel.Hotel;
import dev.morewater.qalab.stay.StayModel.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-STAY-04: ficha del hotel y disponibilidad. */
class HotelTests extends StayTest {

    private record Sample(Hotel hotel, int room, LocalDate date, int left) {}

    /** Busca en el modelo una habitación de Cancún cuya disponibilidad cumpla la condición. */
    private Sample find(IntPredicate state, LocalDate from) {
        for (int i = 0; i < 40; i++) {
            LocalDate d = from.plusDays(i);
            for (Hotel h : StayModel.hotelsIn("CUN"))
                for (Room r : StayModel.roomsOf(h)) {
                    int left = StayModel.availability(h.id(), r.idx(), d.toString());
                    if (state.test(left)) return new Sample(h, r.idx(), d, left);
                }
        }
        throw new IllegalStateException("sin muestra");
    }

    private static String expectedLeft(int left) { return left == 0 ? "Agotada para esas fechas" : left <= 2 ? "¡Quedan " + left + "!" : "Disponible"; }

    private static String totalText(String roomText) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("total con impuestos (\\$[\\d,]+\\.\\d{2})").matcher(roomText);
        assertThat(m.find()).as("texto de total en: " + roomText).isTrue();
        return m.group(1);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_024_DetalleDelHotelConPoliticaYTresHabitaciones")
    void A_STAY_024_DetalleDelHotelConPoliticaYTresHabitaciones() {
        LocalDate t = today();
        Hotel first = StayModel.byPrice(StayModel.hotelsIn("CUN")).get(0);
        StayResultsPage r = results().open("CUN", t.plusDays(14).toString(), t.plusDays(17).toString(), 2, 0);
        r.setLatency(1500); // hace observable el estado de carga
        StayHotelPage h = r.view(0);
        assertThat(h.title()).isEqualTo(first.name());
        assertThat(h.roomsLoading()).as("«Consultando disponibilidad…»").isTrue();
        h.waitRoomsOk();
        assertThat(h.policy()).isEqualTo("Política de cancelación. Gratis hasta 3 días antes de la entrada. Con menos anticipación se cobra la primera noche.");
        List<Room> rooms = StayModel.roomsOf(first);
        for (Room room : rooms) {
            assertThat(h.roomName(room.idx())).isEqualTo(room.name());
            assertThat(h.roomRate(room.idx())).isEqualTo(StayModel.mxn(room.rateCents()));
            assertThat(h.roomText(room.idx())).contains("Hasta " + room.capacity() + " huéspedes");
        }
        Room sup = rooms.get(1);
        assertThat(h.roomText(1)).contains("por noche · total con impuestos " + StayModel.mxn(StayModel.quote(sup.rateCents(), 3).total()));
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_025_DisponibilidadAgotadaPocasHabitacionesYDisponible")
    void A_STAY_025_DisponibilidadAgotadaPocasHabitacionesYDisponible() {
        LocalDate from = today().plusDays(14);
        for (IntPredicate state : new IntPredicate[] {l -> l == 0, l -> l == 1 || l == 2, l -> l >= 3}) {
            Sample s = find(state, from);
            StayHotelPage h = hotelPage().open(s.hotel().id(), s.date().toString(), s.date().plusDays(3).toString(), 2, 0).waitRoomsOk();
            for (Room room : StayModel.roomsOf(s.hotel())) {
                int left = StayModel.availability(s.hotel().id(), room.idx(), s.date().toString());
                String ctx = s.hotel().name() + " " + room.name() + " el " + s.date();
                assertThat(h.roomLeft(room.idx())).as(ctx).isEqualTo(expectedLeft(left));
                assertThat(h.reserveEnabled(room.idx())).as("botón " + ctx).isEqualTo(left > 0);
                assertThat(h.reserveText(room.idx())).isEqualTo("Reservar");
            }
        }
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_STAY_027_CapacidadInsuficienteSegunHuespedes")
    void A_STAY_027_CapacidadInsuficienteSegunHuespedes() {
        Hotel hotel = StayModel.hotelById("CUN-5");
        LocalDate in = today().plusDays(14);
        int[][] guests = {{2, 0}, {2, 1}, {3, 1}, {4, 1}};
        for (int[] g : guests) {
            StayHotelPage h = hotelPage().open(hotel.id(), in.toString(), in.plusDays(3).toString(), g[0], g[1]).waitRoomsOk();
            for (Room room : StayModel.roomsOf(hotel)) {
                boolean fits = room.capacity() >= g[0] + g[1];
                int left = StayModel.availability(hotel.id(), room.idx(), in.toString());
                String ctx = (g[0] + g[1]) + " huéspedes en " + room.name();
                assertThat(h.reserveText(room.idx())).as(ctx).isEqualTo(fits ? "Reservar" : "Capacidad insuficiente");
                assertThat(h.reserveEnabled(room.idx())).as("botón, " + ctx).isEqualTo(fits && left > 0);
            }
        }
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[stay.slow_availability] A_STAY_091_LaDisponibilidadDeHabitacionesTarda35SStaySlowAvailability")
    void A_STAY_091_LaDisponibilidadDeHabitacionesTarda35SStaySlowAvailability() {
        LocalDate in = today().plusDays(14);
        Hotel first = StayModel.byPrice(StayModel.hotelsIn("CUN")).get(0);
        StayHotelPage h = hotelPage().open(first.id(), in.toString(), in.plusDays(3).toString(), 2, 0);
        long t0 = System.nanoTime();
        h.waitRoomsOk();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(ms).as("ms hasta ver las habitaciones (esperado < 3000)").isLessThan(3000);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[stay.flaky_availability] A_STAY_092_LaDisponibilidadFallaCon503AlAzarStayFlakyAvailability")
    void A_STAY_092_LaDisponibilidadFallaCon503AlAzarStayFlakyAvailability() {
        LocalDate in = today().plusDays(14);
        Hotel first = StayModel.byPrice(StayModel.hotelsIn("CUN")).get(0);
        // Semilla elegida para que la primera consulta caiga en el 35 % de fallos si el defecto está activo.
        search().open().setSeed(StayModel.seedWhereFirstCallFails());
        StayHotelPage h = hotelPage().open(first.id(), in.toString(), in.plusDays(3).toString(), 2, 0).waitRooms();
        assertThat(h.roomsError()).as("la consulta de disponibilidad no debe fallar al azar").isFalse();
        assertThat(h.roomName(0)).isEqualTo("Habitación estándar");
    }
}
