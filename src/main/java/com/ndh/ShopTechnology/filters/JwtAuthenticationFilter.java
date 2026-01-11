    package com.ndh.ShopTechnology.filters;
    
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.ndh.ShopTechnology.config.TokenProvider;
    import com.ndh.ShopTechnology.constant.MessageConstant;
    import com.ndh.ShopTechnology.constant.SystemConstant;
    import com.ndh.ShopTechnology.dto.response.APIResponse;
    import com.ndh.ShopTechnology.dto.response.ErrorResponse;
    import com.ndh.ShopTechnology.services.user.CustomUserDetailsService;
    import io.jsonwebtoken.ExpiredJwtException;
    import io.jsonwebtoken.SignatureException;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.beans.factory.annotation.Autowired;
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
        private final TokenProvider jwtTokenUtil;
    
        @Autowired
        public JwtAuthenticationFilter(CustomUserDetailsService userDetailsService, TokenProvider jwtTokenUtil) {
            this.userDetailsService = userDetailsService;
            this.jwtTokenUtil = jwtTokenUtil;
        }
    
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String header = request.getHeader(SystemConstant.HEADER_STRING);
            String username = null;
            String authToken = null;

            if (header != null && header.startsWith(SystemConstant.TOKEN_PREFIX)) {
                authToken = header.replace(SystemConstant.TOKEN_PREFIX, "");
                try {
                    username = jwtTokenUtil.getUsernameFromToken(authToken);
                } catch (IllegalArgumentException e) {
                    logger.error(MessageConstant.ERROR_GET_USERNAME, e);
                } catch (ExpiredJwtException e) {
                    logger.warn(MessageConstant.TOKEN_EXPIRED_LOG, e);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
    
                    APIResponse<Void> apiResponse = APIResponse.of(
                            false,
                            MessageConstant.TOKEN_EXPIRED_RESPONSE,
                            null,
                            List.of(ErrorResponse.builder()
                                    .field("token")
                                    .message(MessageConstant.TOKEN_EXPIRED_RESPONSE)
                                    .build()),
                            null
                    );
                    response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                    return;
                } catch (SignatureException e) {
                    logger.error(MessageConstant.AUTH_FAILED);
                }
    
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    
                if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            jwtTokenUtil.getAuthentication(authToken, SecurityContextHolder.getContext().getAuthentication(), userDetails);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    logger.info(String.format(MessageConstant.AUTH_SUCCESS_PREFIX, username));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
    
            filterChain.doFilter(request, response);
        }
    }