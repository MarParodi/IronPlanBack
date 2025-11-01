package com.example.ironplan.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class RegisterReq {

    // uniques
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email no puede estar vacío")
    private String email;

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 20, message = "El nombre de usuario debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El nombre de usuario solo puede contener letras, números o guiones bajos")
    private String username;

    // password en texto plano -> se hashea en el servicio
    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número"
    )
    private String password;

    // ====== Campos que en tu tabla son NOT NULL ======
    @NotNull(message = "El género es obligatorio")
    private com.example.ironplan.model.Gender gender; // FEMALE | MALE | OTHER

    @NotNull(message = "El nivel es obligatorio")
    private com.example.ironplan.model.Level level;   // AVANZADO | INTERMEDIO | NOVATO

    // ADMIN | USER

    @NotNull(message = "Los días de entrenamiento son obligatorios")
    @Min(value = 0, message = "No puede ser negativo")
    @Max(value = 7, message = "Máximo 7 días por semana")
    private Integer trainDays;



    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd") // <-- clave para parsear JSON a LocalDate
    private LocalDate birthday;
}
