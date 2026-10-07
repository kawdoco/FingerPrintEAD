package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.LoginRequest;
import com.bcilab.attendance.dto.LoginResponse;
import com.bcilab.attendance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
