package com.ndh.ShopTechnology.services.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY = "======================ndh===========================";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000;

    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRATION_MS, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_EXPIRATION_MS, TOKEN_TYPE_REFRESH);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && TOKEN_TYPE_ACCESS.equals(extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class)));
    }

    public boolean validateRefreshToken(String token) {
        return !isTokenExpired(token)
                && TOKEN_TYPE_REFRESH.equals(extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class)));
    }

    public long getAccessTokenExpirationSeconds() {
        return ACCESS_TOKEN_EXPIRATION_MS / 1000;
    }

    private String generateToken(String username, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
