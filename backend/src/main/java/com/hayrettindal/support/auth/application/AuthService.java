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
import org.springframework.transaction.annotation.Transactional;
import com.hayrettindal.support.auth.api.AgentResponse;
import com.hayrettindal.support.auth.domain.UserRole;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;
    private final boolean production;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        LoginThrottle loginThrottle,
        org.springframework.core.env.Environment environment
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottle = loginThrottle;
        this.production = environment.matchesProfiles("prod");
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        loginThrottle.attempt(email);
        AppUserEntity user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive() || (production && user.getEmail().toLowerCase(java.util.Locale.ROOT).endsWith("@demo.local"))
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        loginThrottle.success(email);

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

    public java.util.List<AgentResponse> agents(UUID organizationId) {
        return appUserRepository.findByOrganizationIdAndActiveTrueAndRoleInOrderByFullNameAsc(
            organizationId, java.util.List.of(UserRole.AGENT, UserRole.ADMIN))
            .stream().map(u -> new AgentResponse(u.getId(), u.getFullName(), u.getEmail())).toList();
    }
}
