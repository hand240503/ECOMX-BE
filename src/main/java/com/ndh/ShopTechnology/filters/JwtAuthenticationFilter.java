package com.ndh.ShopTechnology.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.constant.SystemConstant;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.services.auth.JwtService;
import com.ndh.ShopTechnology.services.user.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Autowired
    public JwtAuthenticationFilter(CustomUserDetailsService userDetailsService, JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SystemConstant.HEADER_STRING);
        String username = null;
        String authToken = null;

        if (header != null && header.startsWith(SystemConstant.TOKEN_PREFIX)) {
            authToken = header.replace(SystemConstant.TOKEN_PREFIX, "");
            try {
                username = jwtService.extractUsername(authToken);
            } catch (IllegalArgumentException e) {
                logger.error(MessageConstant.ERROR_GET_USERNAME, e);
            } catch (ExpiredJwtException e) {
                logger.warn(MessageConstant.TOKEN_EXPIRED_LOG, e);
                writeUnauthorizedResponse(response, MessageConstant.TOKEN_EXPIRED_RESPONSE);
                return;
            } catch (JwtException e) {
                logger.warn("Invalid JWT token format or signature", e);
                writeUnauthorizedResponse(response, MessageConstant.AUTH_FAILED);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.validateAccessToken(authToken, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                String method = request.getMethod();
                String uri = request.getRequestURI();
                String query = request.getQueryString();
                String fullPath = query != null && !query.isBlank() ? uri + "?" + query : uri;
                logger.info(String.format("%s | method=%s | uri=%s",
                        String.format(MessageConstant.AUTH_SUCCESS_PREFIX, username),
                        method,
                        fullPath));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        APIResponse<Void> apiResponse = APIResponse.of(
                false,
                message,
                null,
                List.of(ErrorResponse.builder()
                        .field("token")
                        .message(message)
                        .build()),
                null
        );
        response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
    }
}