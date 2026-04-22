package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.filters.JwtAuthenticationFilter;
import com.ndh.ShopTechnology.services.user.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final CustomUserDetailsService      userDetailsService;
    private final JwtAuthenticationEntryPoint   authenticationEntryPoint;
    private final JwtAuthenticationFilter       jwtAuthenticationFilter;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Autowired
    public WebSecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService         = userDetailsService;
        this.authenticationEntryPoint   = authenticationEntryPoint;
        this.jwtAuthenticationFilter    = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Normalizes {@code api.prefix} so request matchers always use a single leading slash (avoids
     * {@code //api/...} when the property is configured with a leading slash).
     */
    private String apiBasePath() {
        String p = apiPrefix == null ? "" : apiPrefix.trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? "" : "/" + p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        final String base = apiBasePath();
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // THÊM DÒNG NÀY
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> {
                    requests
                            // CORS preflight must not require JWT (browser sends OPTIONS without Authorization)
                            .requestMatchers(HttpMethod.OPTIONS, "/**")
                            .permitAll()

                            .requestMatchers(String.format("%s/admin/**", base))
                            .hasAnyRole(RoleConstant.ROLE_ADMIN, RoleConstant.ROLE_EMPLOYEE, RoleConstant.ROLE_MANAGER)

                            .requestMatchers(String.format("%s/auth/**", base), String.format("%s/document/**", base))
                            .permitAll()

                            .requestMatchers(String.format("%s/recommendations/**", base))
                            .permitAll()

                            .requestMatchers(String.format("%s/search/**", base))
                            .permitAll()

                            .requestMatchers(HttpMethod.GET, String.format("%s/products/**", base))
                            .permitAll()

                            .requestMatchers(HttpMethod.POST, String.format("%s/products/by-ids", base))
                            .permitAll()

                            .requestMatchers(HttpMethod.GET, String.format("%s/categories/**", base))
                            .permitAll()

                            .anyRequest()
                            .authenticated();
                })

                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}