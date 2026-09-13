package com.example.ironplan.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Calendario civil del producto: Los Mochis, Sinaloa ({@code America/Mazatlan}, UTC−7).
 * El reto agrupa y puntúa por este día, no por UTC.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("America/Mazatlan");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
    }

    private AppTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
