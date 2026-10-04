package dev.morewater.qalab.stay;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * Réplica de las fórmulas de qalab (lib/stay.ts, lib/prng.ts) para calcular los valores esperados sin copiarlos a mano del texto de los casos.
 * Hoteles, tarifas, disponibilidad, impuestos y política de reembolso se derivan igual que lo hace la aplicación.
 */
public final class StayModel {
    private StayModel() {}

    public static final String[] CITY_CODES = {"CUN", "MEX", "OAX", "PVR", "MID", "SJD"};
    public static final String[][] AMENITIES = {{"wifi", "Wi-Fi"}, {"pool", "Alberca"}, {"breakfast", "Desayuno incluido"}, {"parking", "Estacionamiento"}, {"spa", "Spa"}};
    private static final String[] PRE = {"Casa", "Hotel", "Villa", "Posada", "Torre", "Mar"};
    private static final String[] SUF = {"Aurora", "Nimbo", "Cóndor", "Jacaranda", "Sirena", "Pacífico", "Marlín", "Copal"};
    private static final long[] STAR_BASE = {0, 0, 90_000, 150_000, 240_000, 380_000, 620_000};
    public static final double IVA = 0.16;
    public static final double ISH = 0.03;

    public record Hotel(String id, String city, String name, int stars, double rating, long baseCents, List<String> amenities) {}
    public record Room(int idx, String name, int capacity, long rateCents) {}
    public record Quote(int nights, long base, long iva, long ish, long total) {}

    // ---------- PRNG (mulberry32 + hash) ----------
    public static DoubleSupplier mulberry32(long seed) {
        int[] a = {(int) seed};
        return () -> {
            a[0] += 0x6d2b79f5;
            int t = a[0];
            t = (t ^ (t >>> 15)) * (t | 1);
            t ^= t + (t ^ (t >>> 7)) * (t | 61);
            return Integer.toUnsignedLong(t ^ (t >>> 14)) / 4294967296.0;
        };
    }

    public static long hashStr(String s) {
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        return Integer.toUnsignedLong(h);
    }

    // ---------- Hoteles ----------
    public static List<Hotel> hotelsIn(String city) {
        DoubleSupplier rng = mulberry32(hashStr("stay-" + city));
        Set<String> names = new HashSet<>();
        List<Hotel> out = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String name;
            do {
                String pre = PRE[(int) Math.floor(rng.getAsDouble() * PRE.length)];
                String suf = SUF[(int) Math.floor(rng.getAsDouble() * SUF.length)];
                name = pre + " " + suf;
            } while (names.contains(name));
            names.add(name);
            int stars = 2 + (int) Math.floor(rng.getAsDouble() * 4);
            double rating = Math.round((6.8 + rng.getAsDouble() * 2.8) * 10) / 10.0;
            long base = Math.round((STAR_BASE[stars] * (0.85 + rng.getAsDouble() * 0.4)) / 1000) * 1000;
            List<String> am = new ArrayList<>();
            for (String[] a : AMENITIES) if (rng.getAsDouble() < 0.5 + stars * 0.08) am.add(a[0]);
            out.add(new Hotel(city + "-" + i, city, name, stars, rating, base, am));
        }
        return out;
    }

    public static Hotel hotelByName(String city, String name) {
        return hotelsIn(city).stream().filter(h -> h.name().equals(name)).findFirst().orElseThrow();
    }

    public static Hotel hotelById(String id) {
        return hotelsIn(id.split("-")[0]).stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
    }

    public static List<Room> roomsOf(Hotel h) {
        return List.of(
                new Room(0, "Habitación estándar", 2, h.baseCents()),
                new Room(1, "Habitación superior", 3, Math.round(h.baseCents() * 1.3 / 100) * 100),
                new Room(2, "Suite familiar", 4, Math.round(h.baseCents() * 1.75 / 100) * 100));
    }

    /** Habitaciones libres (0 = agotada). */
    public static int availability(String hotelId, int roomIdx, String checkIn) {
        DoubleSupplier rng = mulberry32(hashStr("avail-" + hotelId + "-" + roomIdx + "-" + checkIn));
        return rng.getAsDouble() < 0.22 ? 0 : 1 + (int) Math.floor(rng.getAsDouble() * 5);
    }

    /** Primera fecha de entrada a partir de {@code from} con la habitación disponible. */
    public static LocalDate availableCheckIn(Hotel h, int roomIdx, LocalDate from) {
        for (int i = 0; i < 120; i++) {
            LocalDate d = from.plusDays(i);
            if (availability(h.id(), roomIdx, d.toString()) > 0) return d;
        }
        throw new IllegalStateException("Sin disponibilidad para " + h.id());
    }

    // ---------- Precios y política ----------
    public static Quote quote(long rateCents, int nights) {
        long base = rateCents * nights;
        long iva = Math.round(base * IVA);
        long ish = Math.round(base * ISH);
        return new Quote(nights, base, iva, ish, base + iva + ish);
    }

    /** Reembolso: gratis con 3 o más días de anticipación; si no, se pierde la primera noche. */
    public static long refund(long total, int nights, LocalDate checkIn, LocalDate today) {
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, checkIn);
        return daysLeft >= 3 ? total : total - Math.round((double) total / nights);
    }

    // ---------- Listados ----------
    public static List<Hotel> byPrice(List<Hotel> l) { return l.stream().sorted((a, b) -> Long.compare(a.baseCents(), b.baseCents())).toList(); }
    public static List<Hotel> byRating(List<Hotel> l) { return l.stream().sorted((a, b) -> Double.compare(b.rating(), a.rating())).toList(); }

    // ---------- Formato ----------
    public static String mxn(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return sign + "$" + String.format(Locale.US, "%,d", abs / 100) + "." + String.format("%02d", abs % 100);
    }

    public static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    public static String monthTitle(java.time.YearMonth ym) { return MONTHS[ym.getMonthValue() - 1] + " " + ym.getYear(); }

    // ---------- Semillas deterministas para fallos intermitentes ----------
    /** Réplica de random() de lib/net.ts para la n-ésima extracción (n desde 0) con una semilla dada. */
    public static double netRandom(long seed, int n) {
        int s = (int) seed;
        int mixed = s + (n + 1) * 0x9e3779b1;
        return mulberry32(Integer.toUnsignedLong(mixed)).getAsDouble();
    }

    /** Semilla con la que la 2.ª extracción (la del defecto flaky de la primera llamada tras fijarla) cae por debajo de 0.35. */
    public static int seedWhereFirstCallFails() {
        for (int seed = 2; seed < 500; seed++) if (netRandom(seed, 1) < 0.35) return seed;
        throw new IllegalStateException("sin semilla");
    }
}
