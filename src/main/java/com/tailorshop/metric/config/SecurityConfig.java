package com.tailorshop.metric.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Value("${app.security.oauth2.enabled:true}")
    private boolean oauth2Enabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder())
            .and()
            .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"errorCode\":\"UNAUTHORIZED\"," +
                        "\"message\":\"Authentication required. Please login first.\"," +
                        "\"timestamp\":\"" + LocalDateTime.now() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"errorCode\":\"FORBIDDEN\"," +
                        "\"message\":\"Access denied. You do not have permission to access this resource.\"," +
                        "\"timestamp\":\"" + LocalDateTime.now() + "\"}"
                    );
                })
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/auth/**", "/public/**", "/health", "/", "/pages/**", "/oauth2/**", "/error"
                ).permitAll()
                .requestMatchers(
                    "/css/**", "/js/**", "/img/**", "/images/**", "/fonts/**", "/static/**"
                ).permitAll()
                .requestMatchers(
                    "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/webjars/**"
                ).permitAll()
                .requestMatchers(
                    "/actuator/health", "/actuator/info", "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        if (oauth2Enabled) {
            http
                .oauth2Login(oauth2 -> oauth2.loginPage("/pages/login"))
                .logout(logout -> logout
                    .logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/pages/login")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                );
        } else {
            http
                .formLogin(form -> form
                    .loginPage("/pages/login")
                    .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                    .logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/pages/login")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                );
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
