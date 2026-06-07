package com.insync.service;

import com.insync.domain.model.User;
import com.insync.exception.EmailAlreadyExistsException;
import com.insync.repository.UserRepository;
import com.insync.security.JwtService;
import com.insync.web.dto.request.LoginRequest;
import com.insync.web.dto.request.RefreshRequest;
import com.insync.web.dto.request.RegisterRequest;
import com.insync.web.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();

        userRepository.save(user);

        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email()).orElseThrow();

        return issueTokenPair(user);
    }

    public AuthResponse refresh(RefreshRequest request) {

        String token = request.refreshToken();

        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email).orElseThrow();

        if (!jwtService.isTokenValid(token, user) || !jwtService.isRefreshToken(token)) {
            throw new io.jsonwebtoken.JwtException("Provided token is not a valid refresh token");
        }

        return issueTokenPair(user);
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpiryMs());
    }
}
