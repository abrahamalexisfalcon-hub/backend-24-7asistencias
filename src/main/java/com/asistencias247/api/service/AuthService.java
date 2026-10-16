package com.asistencias247.api.service;

import com.asistencias247.api.dto.AuthRequestDto;
import com.asistencias247.api.dto.AuthResponseDto;

public interface AuthService {
    AuthResponseDto autenticar(AuthRequestDto request);
}
