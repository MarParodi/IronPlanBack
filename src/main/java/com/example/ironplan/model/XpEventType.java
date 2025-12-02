// src/main/java/com/example/ironplan/model/XpEventType.java
package com.example.ironplan.model;

public enum XpEventType {
    WORKOUT_COMPLETED,      // terminó una sesión
    ACHIEVEMENT_UNLOCKED,   // logró una hazaña
    ROUTINE_PURCHASE,       // compró una rutina con XP (xpDelta negativo)
    MANUAL_ADJUSTMENT       // ajustes de admin o pruebas
}
