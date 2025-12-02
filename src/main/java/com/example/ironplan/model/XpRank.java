// src/main/java/com/example/ironplan/model/XpRank.java
package com.example.ironplan.model;

public enum XpRank {

    NOVATO_I(0,        "Novato I"),
    NOVATO_II(1000,    "Novato II"),
    NOVATO_III(2500,   "Novato III"),
    INTERMEDIO_I(4000, "Intermedio I"),
    INTERMEDIO_II(6000,"Intermedio II"),
    AVANZADO_I(9000,   "Avanzado I");

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
