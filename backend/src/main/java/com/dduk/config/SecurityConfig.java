package com.dduk.config;

import com.dduk.dto.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/dashboard.html",
                        "/assets/**",
                        "/pages/**",
                        "/services/**",
                        "/styles/**"
                ).permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/callbacks/rpa").permitAll() // RPA 비동기 웹훅 콜백 단일 엔드포인트 핀포인트 허용
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/vendors").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/vendors/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/inventory/vendors/*").permitAll()
                .requestMatchers("/api/v1/inventory/items", "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/items").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/purchase-orders").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/hr/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "INVENTORY")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint((request, response, authException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.", "UNAUTHORIZED"))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.", "FORBIDDEN"))
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message, code));
    }
}
