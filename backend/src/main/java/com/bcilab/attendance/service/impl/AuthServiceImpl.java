package com.bcilab.attendance.service.impl;

import com.bcilab.attendance.dto.LoginRequest;
import com.bcilab.attendance.dto.LoginResponse;
import com.bcilab.attendance.model.AdminUser;
import com.bcilab.attendance.repository.AdminUserRepository;
import com.bcilab.attendance.security.JwtService;
import com.bcilab.attendance.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements com.bcilab.attendance.service.AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Override
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name(),
                appProperties.getJwt().getExpirationMinutes());
    }
}
