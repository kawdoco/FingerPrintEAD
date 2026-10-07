package com.bcilab.attendance.security;

import com.bcilab.attendance.config.AppProperties;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates ESP32 fingerprint devices on /api/v1/scan using a simple
 * shared-secret header (X-Device-Key), rather than JWT. Each device is
 * provisioned via POST /api/v1/devices (admin) and gets its own key.
 */
@Component
@RequiredArgsConstructor
public class DeviceApiKeyFilter extends OncePerRequestFilter {

    private final FingerprintDeviceRepository deviceRepository;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/v1/scan")) {
            String apiKey = request.getHeader(appProperties.getDevice().getApiKeyHeader());
            Optional<FingerprintDevice> device = apiKey == null
                    ? Optional.empty()
                    : deviceRepository.findByApiKey(apiKey);

            if (device.isPresent()) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        device.get().getDeviceCode(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            // If missing/invalid, we simply don't authenticate — SecurityConfig
            // will reject the request with 401 further down the chain.
        }
        filterChain.doFilter(request, response);
    }
}
