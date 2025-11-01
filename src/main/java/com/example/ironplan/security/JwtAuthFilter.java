package com.example.ironplan.security;

import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                String subject = jwt.extractUsername(token); // normalmente email (según tu AuthService)
                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Soporta subject como email o username:
                    User user = users.findByEmail(subject)
                            .orElseGet(() -> users.findByUsername(subject).orElse(null));

                    if (user != null && jwt.isValid(token, subject)) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ignored) {
                // token inválido/expirado → dejamos pasar sin auth; Security dirá 401 en endpoints protegidos
            }
        }

        chain.doFilter(req, res);
    }
}
