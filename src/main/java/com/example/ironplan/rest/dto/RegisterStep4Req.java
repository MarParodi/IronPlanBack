package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterStep4Req {

    @NotBlank(message = "El token de registro es obligatorio")
    private String onboardingToken;

    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private boolean acceptedTerms;

    @AssertTrue(message = "Debes aceptar el aviso de privacidad")
    private boolean acceptedPrivacy;

    @AssertTrue(message = "Debes aceptar el consentimiento de uso de datos para métricas del programa")
    private boolean consentProgramMetrics;
}

