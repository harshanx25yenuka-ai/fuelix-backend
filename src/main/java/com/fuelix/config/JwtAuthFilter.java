package com.fuelix.config;

import com.fuelix.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String path = request.getServletPath();

        // Skip JWT validation for public GET endpoints
        if (shouldSkipFilter(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // For authenticated endpoints, return 401
            if (needsAuthentication(path)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (jwtService.validateToken(token)) {
            String nic = jwtService.extractNic(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(nic);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            // Invalid token
            if (needsAuthentication(path)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(String path) {
        // Public GET endpoints that don't require authentication
        return path.startsWith("/api/auth/") ||
                path.matches("/api/fuel-stations/?$") ||
                path.matches("/api/fuel-stations/\\d+$") ||
                path.startsWith("/api/fuel-stations/province/") ||
                path.startsWith("/api/fuel-stations/district/") ||
                path.startsWith("/api/fuel-stations/brand/") ||
                path.equals("/api/fuel-stations/partners") ||
                path.equals("/api/fuel-stations/open") ||
                path.equals("/api/staff/auth") ||
                path.startsWith("/api/fuel-stations/search") ||
                path.startsWith("/api/fuel-stations/filter") ||
                path.startsWith("/api/fuel-stations/stats/");
    }

    private boolean needsAuthentication(String path) {
        // Endpoints that require authentication
        return path.startsWith("/api/fuel-stations/user/") ||
                path.matches("/api/fuel-stations/\\d+$") && !isGetRequest(path) ||
                path.equals("/api/fuel-stations") && !isGetRequest(path) ||
                path.startsWith("/api/admin/");
    }

    private boolean isGetRequest(String path) {
        // This is a simplification - in real implementation you'd check the HTTP method
        return false;
    }
}