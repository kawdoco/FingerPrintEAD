package com.bcilab.attendance.service;

import com.bcilab.attendance.dto.LoginRequest;
import com.bcilab.attendance.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
