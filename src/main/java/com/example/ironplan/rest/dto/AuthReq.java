package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class AuthReq {

    @NotBlank(message = "El identificador (email o username) no puede estar vacío")
    private String identifier;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;
}
