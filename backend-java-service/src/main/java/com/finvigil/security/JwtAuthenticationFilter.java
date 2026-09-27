package com.finvigil.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.validateToken(jwt)) {
                    String userType = jwtService.extractUserType(jwt);
                    String employeeRole = jwtService.extractRole(jwt);

                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    if ("EMPLOYEE".equalsIgnoreCase(userType) || employeeRole != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
                        if (employeeRole != null && !employeeRole.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + employeeRole.toUpperCase()));
                        }
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception ignored) {
            // Context will remain unauthenticated; SecurityFilterChain handles unauthorized access
        }

        filterChain.doFilter(request, response);
    }
}
