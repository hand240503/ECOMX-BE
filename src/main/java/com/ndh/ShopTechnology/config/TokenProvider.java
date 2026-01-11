package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.constant.SystemConstant;
import io.jsonwebtoken.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider implements Serializable {

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 phút mặc định
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}") // 7 ngày mặc định
    private long refreshTokenExpirationMs;

    // ==================== GET CLAIMS FROM TOKEN ====================

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // ==================== GENERATE TOKENS ====================

    /**
     * ✅ Generate Access Token (ngắn hạn - 15 phút)
     * Dùng cho việc xác thực API calls
     */
    public String generateAccessToken(Authentication authentication) {
        final String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(SystemConstant.AUTHORITIES_KEY, authorities)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ Generate Access Token từ username
     * Dùng khi refresh token
     */
    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ Generate Refresh Token (dài hạn - 7 ngày)
     * Dùng để làm mới access token khi hết hạn
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ⚠️ DEPRECATED: Sử dụng generateAccessToken() thay thế
     * Giữ lại để tương thích với code cũ
     */
    @Deprecated
    public String generateToken(Authentication authentication) {
        return generateAccessToken(authentication);
    }

    // ==================== VALIDATE TOKEN ====================

    /**
     * ✅ Validate token với UserDetails
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Validate token đơn giản (không cần UserDetails)
     * Dùng cho refresh token validation
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // ==================== GET AUTHENTICATION ====================

    public UsernamePasswordAuthenticationToken getAuthentication(
            final String token,
            final Authentication existingAuth,
            final UserDetails userDetails) {

        final JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build();

        final Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
        final Claims claims = claimsJws.getBody();

        final Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(SystemConstant.AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // ==================== HELPER METHODS ====================

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SystemConstant.SIGNING_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * ✅ Getter cho refresh token expiration time (ms)
     * Cần thiết cho RefreshTokenService để tính expiry date
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    /**
     * ✅ Getter cho access token expiration time (ms)
     * Có thể dùng để trả về cho client
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /**
     * ✅ Get access token expiration time in seconds
     * Thuận tiện cho response trả về client
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}