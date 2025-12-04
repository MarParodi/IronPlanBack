// src/main/java/com/example/ironplan/model/XpRank.java
package com.example.ironplan.model;

public enum XpRank {

    NOVATO_I(0,           "Novato I"),
    NOVATO_II(1000,       "Novato II"),
    NOVATO_III(2500,      "Novato III"),
    INTERMEDIO_I(5000,    "Intermedio I"),
    INTERMEDIO_II(10000,  "Intermedio II"),
    INTERMEDIO_III(20000, "Intermedio III"),
    AVANZADO_I(35000,     "Avanzado I"),
    AVANZADO_II(55000,    "Avanzado II"),
    AVANZADO_III(80000,   "Avanzado III"),
    ELITE(110000,         "Elite"),
    MAESTRO(150000,       "Maestro"),
    LEYENDA(200000,       "Leyenda");

    private final int minXp;
    private final String displayName;

    XpRank(int minXp, String displayName) {
        this.minXp = minXp;
        this.displayName = displayName;
    }

    public int getMinXp() {
        return minXp;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Devuelve el rango que corresponde a una cantidad de XP acumulada (lifetime). */
    public static XpRank fromLifetimeXp(int lifetimeXp) {
        XpRank result = NOVATO_I;
        for (XpRank rank : values()) {
            if (lifetimeXp >= rank.minXp) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }
}
