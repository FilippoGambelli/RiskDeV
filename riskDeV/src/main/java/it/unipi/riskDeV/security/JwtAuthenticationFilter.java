package it.unipi.riskDeV.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <--- Importante per i log

@Component
@RequiredArgsConstructor
@Slf4j // <--- Abilita i log
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        
        // LOG 1: Vediamo se l'header arriva
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("JWT Filter: Header Authorization mancante o non inizia con Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        
        try {
            // LOG 2: Proviamo a validare
            if (jwtUtil.validateToken(token)) {
                
                String userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getUserRoleFromToken(token);

                // LOG 3: Vediamo cosa c'è dentro il token
                log.info("JWT Filter: Token Valido. UserID: '{}', Role nel token: '{}'", userId, role);

                // Normalizzazione difensiva (anche se nel DB è giusto, non fa male)
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                    log.info("JWT Filter: Ruolo normalizzato a '{}'", role);
                }

                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,           
                    null,             
                    authorities
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT Filter: SecurityContext aggiornato con successo.");
            } else {
                // LOG 4: Validazione fallita
                log.error("JWT Filter: validateToken ha restituito false.");
            }
        } catch (Exception e) {
            // LOG 5: Eccezione imprevista
            log.error("JWT Filter: Errore durante l'elaborazione del token", e);
        }
        
        filterChain.doFilter(request, response);
    }
}