package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Gender;
import com.example.ironplan.model.Goal;
import com.example.ironplan.model.Level;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterStep2Req {

    @NotBlank(message = "El token de registro es obligatorio")
    private String onboardingToken;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @NotNull(message = "El género es obligatorio")
    private Gender gender;

    @NotNull(message = "El nivel es obligatorio")
    private Level level;

    @NotNull(message = "Los días de entrenamiento son obligatorios")
    @Min(value = 0, message = "No puede ser negativo")
    @Max(value = 7, message = "Máximo 7 días por semana")
    private Integer trainDays;

    @Min(value = 1, message = "El peso debe ser mayor a 0")
    private Integer weight;

    @Min(value = 1, message = "La altura debe ser mayor a 0")
    private Integer height;

    @NotNull(message = "El objetivo es obligatorio")
    private Goal goal;
}

