package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.constant.SystemConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider implements Serializable {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_FAMILY_ID = "family_id";
    private static final String CLAIM_PARENT_TOKEN_ID = "parent_token_id";
    private static final String CLAIM_DEVICE_ID = "device_id";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.issuer:ecomx-be}")
    private String issuer;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public String generateAccessToken(Authentication authentication) {
        final String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setSubject(authentication.getName())
                .claim(SystemConstant.AUTHORITIES_KEY, authorities)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return generateRefreshToken(
                username,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                null,
                null);
    }

    public String generateRefreshToken(
            String username,
            String tokenId,
            String familyId,
            String parentTokenId,
            String deviceId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpirationMs);

        var builder = Jwts.builder()
                .setId(tokenId)
                .setIssuer(issuer)
                .setSubject(username)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_FAMILY_ID, familyId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256);

        if (parentTokenId != null && !parentTokenId.isBlank()) {
            builder.claim(CLAIM_PARENT_TOKEN_ID, parentTokenId);
        }

        if (deviceId != null && !deviceId.isBlank()) {
            builder.claim(CLAIM_DEVICE_ID, deviceId);
        }

        return builder.compact();
    }

    public String getUsernameFromAccessToken(String token) {
        return parseAccessClaims(token).getSubject();
    }

    // Backward compatibility for older call sites
    public String getUsernameFromToken(String token) {
        return getUsernameFromAccessToken(token);
    }

    public String getUsernameFromRefreshToken(String token) {
        return parseRefreshClaims(token).getSubject();
    }

    public Claims parseRefreshTokenClaims(String token) {
        return parseRefreshClaims(token);
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseAccessClaims(token);
            return Objects.equals(claims.getSubject(), userDetails.getUsername());
        } catch (Exception e) {
            log.error("Access token invalid: {}", e.getMessage());
            return false;
        }
    }

    // Backward compatibility for older call sites
    public boolean validateToken(String token, UserDetails userDetails) {
        return validateAccessToken(token, userDetails);
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Refresh token invalid: {}", e.getMessage());
            return false;
        }
    }

    public UsernamePasswordAuthenticationToken getAuthentication(
            String accessToken,
            UserDetails userDetails) {
        Claims claims = parseAccessClaims(accessToken);

        String scopes = claims.get(SystemConstant.AUTHORITIES_KEY, String.class);
        if (scopes == null || scopes.isBlank()) {
            throw new IllegalArgumentException("Access token missing scopes claim");
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // Backward compatibility for older call sites
    public UsernamePasswordAuthenticationToken getAuthentication(
            String accessToken,
            Authentication ignoredAuthentication,
            UserDetails userDetails) {
        return getAuthentication(accessToken, userDetails);
    }

    private Claims parseAccessClaims(String token) {
        JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .requireIssuer(issuer)
                .require(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .build();

        Jws<Claims> jws = parser.parseClaimsJws(token);
        return jws.getBody();
    }

    private Claims parseRefreshClaims(String token) {
        JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .requireIssuer(issuer)
                .require(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .build();

        Jws<Claims> jws = parser.parseClaimsJws(token);
        return jws.getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SystemConstant.SIGNING_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}