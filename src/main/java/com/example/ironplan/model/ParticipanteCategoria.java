package com.example.ironplan.model;

public enum ParticipanteCategoria {
    PRINCIPIANTE,
    INTERMEDIO,
    AVANZADO;

    /** Deriva la categoría del nivel de entrenamiento del perfil del usuario. */
    public static ParticipanteCategoria fromUserLevel(Level level) {
        if (level == null) return PRINCIPIANTE;
        return switch (level) {
            case NOVATO -> PRINCIPIANTE;
            case INTERMEDIO -> INTERMEDIO;
            case AVANZADO -> AVANZADO;
        };
    }
}
