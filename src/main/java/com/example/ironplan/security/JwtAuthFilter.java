package com.example.ironplan.security;

import com.example.ironplan.model.Role;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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
        if (auth != null && auth.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = auth.substring(7);

            try {
                Claims claims = jwt.parseClaims(token);
                Long uid = jwt.extractUserId(claims);
                Role role = parseRole(jwt.extractRole(claims));

                User user = resolveUser(uid, claims.getSubject());
                if (user != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // token inválido/expirado → dejamos pasar sin auth; Security dirá 401 en endpoints protegidos
                System.err.println("[JwtAuthFilter] Error procesando token: " + e.getMessage());
            }
        }

        chain.doFilter(req, res);
    }

    /**
     * Con uid: proxy Hibernate (sin SQL). getId() no toca MySQL; el resto de campos
     * se cargan si un servicio los usa (requiere OSIV activo en el request).
     * Sin uid (tokens viejos): lookup por email/username.
     */
    private User resolveUser(Long uid, String subject) {
        if (uid != null) {
            return users.getReferenceById(uid);
        }
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return users.findByEmail(subject)
                .orElseGet(() -> users.findByUsername(subject).orElse(null));
    }

    private static Role parseRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }
}
