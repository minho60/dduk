package com.dduk.config;

import com.dduk.dto.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
    @Profile("!prod")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
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
                .requestMatchers(HttpMethod.POST, "/api/v1/ai/ocr/documents").hasAnyRole("ADMIN", "HR", "INVENTORY")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/vendors").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/vendors/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/inventory/vendors/*").permitAll()
                .requestMatchers("/api/v1/inventory/items", "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/items").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/purchase-orders").permitAll()
                
                // [개발환경 조회 제한적 완화 정책 - GET 핀포인트 13개만 permitAll]
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/dashboard").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/reports/analytics").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/reports/trial-balance").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/reports/balance-sheet").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/reports/profit-loss").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/monthly-closing").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/payroll-ledgers").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/vouchers").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/vouchers/summary").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/vouchers/accounts/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/periods").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/accounts/tree").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accounting/accounts/list").permitAll()
                
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/hr/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/v1/inventory/**", "/api/v1/inventories/**").hasAnyRole("ADMIN", "INVENTORY")
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
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
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
                .requestMatchers("/api/v1/callbacks/rpa").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/vendors").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/vendors/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/inventory/vendors/*").permitAll()
                .requestMatchers("/api/v1/inventory/items", "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/items/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/items").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/purchase-orders").permitAll()
                
                // 운영(prod) 환경에서는 회계 관련 permitAll이 전혀 없음
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/hr/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/v1/inventory/**", "/api/v1/inventories/**").hasAnyRole("ADMIN", "INVENTORY")
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
