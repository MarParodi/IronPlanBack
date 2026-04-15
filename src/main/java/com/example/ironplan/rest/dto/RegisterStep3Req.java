package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterStep3Req {

    @jakarta.validation.constraints.NotBlank(message = "El token de registro es obligatorio")
    private String onboardingToken;

    @Size(max = 120, message = "El código no puede superar 120 caracteres")
    private String organizationCode;

    @Size(max = 200, message = "El grupo no puede superar 200 caracteres")
    private String organizationGroup;

    @Size(max = 120, message = "El rol no puede superar 120 caracteres")
    private String organizationRole; // opcional
}

