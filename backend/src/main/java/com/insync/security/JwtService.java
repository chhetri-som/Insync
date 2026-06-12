package com.insync.security;

import com.insync.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${insync.jwt.secret}")
    private String secretKey;

    // values are exposed so the AuthService can include it in AuthResponse.expiresIn
    @Getter
    @Value("${insync.jwt.expiry-ms:86400000}")
    private long accessTokenExpiryMs;

    @Value("${insync.jwt.refresh-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // embed UUID
        if (userDetails instanceof User user) {
            extraClaims.put("userId", user.getId().toString());
        }
        return buildToken(extraClaims, userDetails, accessTokenExpiryMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(Map.of("tokenType", "refresh"), userDetails, refreshTokenExpiryMs);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiryMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername()) // email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // token validation

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String subject = extractUsername(token);
        return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, c -> c.get("tokenType", String.class)));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // claim extraction

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
