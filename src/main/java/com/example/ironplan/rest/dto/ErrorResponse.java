package com.example.ironplan.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String error;
    private LocalDateTime timestamp;

    public static ErrorResponse of(int status, String message, String error) {
        return new ErrorResponse(status,error,message,LocalDateTime.now());
    }
}
