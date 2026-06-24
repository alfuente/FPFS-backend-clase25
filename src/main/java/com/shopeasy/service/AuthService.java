package com.shopeasy.service;

import com.shopeasy.dto.request.LoginRequest;
import com.shopeasy.dto.request.RegisterRequest;
import com.shopeasy.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
