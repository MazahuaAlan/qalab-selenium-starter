package dev.morewater.qalab.tests.stay;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.stay.*;
import dev.morewater.qalab.stay.StayModel;
import dev.morewater.qalab.stay.StayModel.Hotel;
import dev.morewater.qalab.stay.StayModel.Quote;
import dev.morewater.qalab.stay.StayModel.Room;
import java.time.LocalDate;

/** Base de las pruebas de Stay: páginas, «hoy» del navegador y atajos de preparación (la URL del hotel evita repetir el buscador). */
abstract class StayTest extends BaseTest {

    /** Reserva planeada: hotel, habitación y fechas con disponibilidad (calculada con el modelo). */
    record Plan(Hotel hotel, int roomIdx, LocalDate in, int nights, int adults, int kids) {
        LocalDate out() { return in.plusDays(nights); }
        Room room() { return StayModel.roomsOf(hotel).get(roomIdx); }
        Quote quote() { return StayModel.quote(room().rateCents(), nights); }
    }

    StaySearchPage search() { return new StaySearchPage(driver); }
    StayResultsPage results() { return new StayResultsPage(driver); }
    StayHotelPage hotelPage() { return new StayHotelPage(driver); }
    StayBookPage book() { return new StayBookPage(driver); }
    StayConfirmationPage confirmation() { return new StayConfirmationPage(driver); }
    StayStaysPage stays() { return new StayStaysPage(driver); }
    WalletPage wallet() { return new WalletPage(driver); }

    LocalDate today() { return search().today(); }

    /** Casa Cóndor (Cancún), habitación superior, 3 noches, con entrada disponible a partir de hoy + minOffset. */
    Plan defaultPlan(int minOffset) { return plan("Casa Cóndor", 1, minOffset, 3); }

    Plan plan(String hotelName, int roomIdx, int minOffset, int nights) {
        Hotel h = StayModel.hotelByName("CUN", hotelName);
        LocalDate in = StayModel.availableCheckIn(h, roomIdx, today().plusDays(minOffset));
        return new Plan(h, roomIdx, in, nights, 2, 0);
    }

    /** Reserva con entrada en una fecha concreta: primera habitación disponible de Cancún (preferida: superior). */
    Plan planOn(LocalDate in, int nights) {
        for (Hotel h : StayModel.hotelsIn("CUN"))
            for (int r : new int[] {1, 0, 2})
                if (StayModel.availability(h.id(), r, in.toString()) > 0) return new Plan(h, r, in, nights, 2, 0);
        throw new IllegalStateException("Sin habitaciones disponibles el " + in);
    }

    static String countLabel(int n) { return n + (n == 1 ? " hotel" : " hoteles"); }

    static String nightsLabel(int n) { return n + (n == 1 ? " noche" : " noches"); }

    static String pesos(long n) { return "$" + String.format(java.util.Locale.US, "%,d", n); }

    static final java.util.Map<String, String> CITY_NAMES = java.util.Map.of("CUN", "Cancún", "MEX", "Ciudad de México", "OAX", "Oaxaca",
            "PVR", "Puerto Vallarta", "MID", "Mérida", "SJD", "Los Cabos");

    StayHotelPage openHotel(Plan p) {
        return hotelPage().open(p.hotel().id(), p.in().toString(), p.out().toString(), p.adults(), p.kids()).waitRoomsOk();
    }

    /** Abre la ficha del hotel y reserva la habitación del plan: queda en el formulario con la retención activa. */
    StayBookPage startBooking(Plan p) { return openHotel(p).reserve(p.roomIdx()); }

    /** Reserva completa con datos válidos; devuelve el código de confirmación. */
    String completeBooking(Plan p) {
        StayBookPage b = startBooking(p).fillValid();
        return b.confirmOk().code();
    }
}
