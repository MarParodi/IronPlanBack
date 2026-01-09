package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Gender;
import com.example.ironplan.model.Level;

import java.time.LocalDate;

public record UserUpdateDTO(
        String username,
        String password,
        String email,
        Level level,
        Integer trainDays,
        Integer weight,
        Integer height,
        String currentPassword,
        String newPassword
) {



    public String getNewPassword() {
        return newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Level getLevel() {
        return level;
    }

    public Integer getTrainDays() {
        return trainDays;
    }

    public Integer getWeight() {
        return weight;
    }

    public Integer getHeight() {
        return height;
    }


}
