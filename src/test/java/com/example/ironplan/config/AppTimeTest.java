package com.example.ironplan.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTimeTest {

    @Test
    void zonaDeLosMochisEsUtcMenosSiete() {
        assertEquals("America/Mazatlan", AppTime.ZONE.getId());
        assertEquals(ZoneOffset.ofHours(-7), AppTime.ZONE.getRules().getOffset(AppTime.now()));
    }
}
