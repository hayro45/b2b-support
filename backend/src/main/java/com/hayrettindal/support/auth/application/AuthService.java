package com.hayrettindal.support.auth.application;

import com.hayrettindal.support.auth.api.LoginRequest;
import com.hayrettindal.support.auth.api.LoginResponse;
import com.hayrettindal.support.auth.api.MeResponse;
import com.hayrettindal.support.auth.infrastructure.AppUserEntity;
import com.hayrettindal.support.auth.infrastructure.AppUserRepository;
import com.hayrettindal.support.common.NotFoundException;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AppUserEntity user = appUserRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(
            user.getId(),
            user.getOrganizationId(),
            user.getEmail(),
            user.getRole()
        );

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    public MeResponse me(UUID userId) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return new MeResponse(
            user.getId(),
            user.getOrganizationId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}
