package com.example.ironplan.config;

/**
 * Constantes para el seed de desarrollo ({@link DevDataInitializer}).
 */
public final class DevSeedData {

    private DevSeedData() {}

    public static final String DEMO_PASSWORD = "Demo1234!";

    public static final String EMAIL_ADMIN = "admin@ironplan.local";
    public static final String EMAIL_MARINA = "marina@ironplan.local";
    public static final String EMAIL_CARLOS = "carlos@ironplan.local";
    public static final String EMAIL_LUCIA = "lucia@ironplan.local";

    public static final String INVITE_CODE = "KINE2026";

    /** name, description, instructions, primaryMuscle, secondaryMuscle */
    public static final String[][] EXERCISES = {
            {"Press de banca", "Press horizontal con barra.", "Acostado, barra al pecho, empujar.", "Pecho", "Tríceps"},
            {"Press inclinado con mancuernas", "Énfasis en pecho superior.", "Banco 30°, empuje vertical.", "Pecho", "Hombros"},
            {"Aperturas en polea", "Aislamiento de pecho.", "Brazos ligeramente flexionados.", "Pecho", null},
            {"Remo con barra", "Remo horizontal.", "Torso inclinado, tirar hacia el ombligo.", "Espalda", "Bíceps"},
            {"Dominadas", "Tracción vertical.", "Agarre prono, pecho a la barra.", "Espalda", "Bíceps"},
            {"Jalón al pecho", "Tracción en polea.", "Torso erguido, barra al pecho.", "Espalda", null},
            {"Peso muerto rumano", "Cadena posterior.", "Cadera atrás, barra cerca de piernas.", "Piernas", "Espalda"},
            {"Sentadilla trasera", "Sentadilla con barra.", "Barra en trapecios, profundidad controlada.", "Piernas", "Glúteos"},
            {"Prensa de piernas", "Prensa en máquina.", "Pies ancho hombros, no bloquear rodillas.", "Piernas", null},
            {"Zancadas con mancuernas", "Unilateral.", "Paso largo, rodilla no pasa punta pie.", "Piernas", "Glúteos"},
            {"Press militar", "Press vertical.", "Barra frente, core activo.", "Hombros", "Tríceps"},
            {"Elevaciones laterales", "Deltoides lateral.", "Codos ligeramente flexionados.", "Hombros", null},
            {"Face pull", "Deltoides posterior.", "Codos altos, tirar a la cara.", "Hombros", "Espalda"},
            {"Curl de bíceps con barra", "Curl de pie.", "Codos fijos, subir sin balanceo.", "Brazos", null},
            {"Curl martillo", "Bíceps y braquial.", "Mancuernas neutras.", "Brazos", null},
            {"Extensiones de tríceps en polea", "Tríceps.", "Codos pegados al torso.", "Brazos", null},
            {"Fondos en paralelas", "Tríceps y pecho.", "Torso ligeramente inclinado.", "Brazos", "Pecho"},
            {"Plancha abdominal", "Core isométrico.", "Cuerpo recto, glúteos activos.", "Core", null},
            {"Crunch en polea", "Abdominales.", "Flexión de tronco controlada.", "Core", null},
            {"Hip thrust con barra", "Glúteos.", "Espalda en banco, empuje de cadera.", "Piernas", "Glúteos"},
            {"Elevación de gemelos de pie", "Gemelos.", "Rango completo, pausa arriba.", "Piernas", null},
            {"Peso muerto convencional", "Tirón desde el suelo.", "Espalda neutra, empuje de piernas.", "Piernas", "Espalda"},
            {"Remo en máquina", "Remo sentado.", "Pecho apoyado, tirar con codos.", "Espalda", null},
            {"Press de hombros con mancuernas", "Press sentado.", "Mancuernas a la altura de orejas.", "Hombros", "Tríceps"},
            {"Pullover en polea", "Dorsal y pecho.", "Brazos casi extendidos.", "Espalda", "Pecho"},
    };
}
