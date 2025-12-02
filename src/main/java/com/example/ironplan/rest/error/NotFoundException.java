// src/main/java/com/example/ironplan/rest/error/NotFoundException.java
package com.example.ironplan.rest.error;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
