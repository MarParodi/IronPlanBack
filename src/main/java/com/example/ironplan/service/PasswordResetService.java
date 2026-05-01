package com.example.ironplan.service;

import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // PASO 1: genera código y lo manda al email
    public void sendResetCode(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese email"));

        String code = String.format("%06d", new Random().nextInt(999999));
        user.setResetPasswordToken(code);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("IronPlan – Código para restablecer tu contraseña");
        message.setText(
            "Hola " + user.getDisplayUsername() + ",\n\n" +
            "Tu código para restablecer tu contraseña es:\n\n" +
            "  " + code + "\n\n" +
            "Este código expira en 15 minutos.\n" +
            "Si no solicitaste esto, ignora este mensaje.\n\n" +
            "– Equipo IronPlan"
        );
        mailSender.send(message);
    }

    // PASO 2: valida que el código sea correcto y no haya expirado
    public void verifyCode(String email, String code) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email no encontrado"));

        if (user.getResetPasswordToken() == null ||
            !user.getResetPasswordToken().equals(code)) {
            throw new RuntimeException("Código incorrecto");
        }

        if (user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código ha expirado");
        }
    }

    // PASO 3: cambia la contraseña y limpia el token
    public void resetPassword(String email, String code, String newPassword) {
        verifyCode(email, code); // reutiliza la validación

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }
}