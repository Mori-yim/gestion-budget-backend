//package com.budgetcam.security;
//
//import com.budgetcam.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//    private final JwtAuthFilter jwtFilter;
//    private final UserRepository userRepo;
//    @Value("${cors.allowed-origins}") private String origins;
//
//    @Bean
//    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
//        return http.csrf(AbstractHttpConfigurer::disable)
//                .cors(c -> c.configurationSource(corsSource()))
//                .authorizeHttpRequests(a -> a
//                        .requestMatchers("/api/v1/auth/**", "/actuator/**").permitAll()
//                        .anyRequest().authenticated())
//                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authenticationProvider(authProvider())
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//                .build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsSource() {
//        CorsConfiguration cfg = new CorsConfiguration();
//        cfg.setAllowedOrigins(Arrays.asList(origins.split(",")));
//        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
//        cfg.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","Accept"));
//        cfg.setAllowCredentials(true);
//        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
//        src.registerCorsConfiguration("/**", cfg);
//        return src;
//    }
//
//    @Bean
//    public UserDetailsService uds() {
//        return email -> userRepo.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
//    }
//
//    @Bean
//    public AuthenticationProvider authProvider() {
//        var p = new DaoAuthenticationProvider();
//        p.setUserDetailsService(uds());
//        p.setPasswordEncoder(passwordEncoder());
//        return p;
//    }
//
//    @Bean
//    public AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception {
//        return c.getAuthenticationManager();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
//}
package com.budgetcam.security;

import com.budgetcam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    private final UserRepository userRepository;

    @Value("${cors.allowed-origins}")
    private String origins;

//    @Bean
//    public UserDetailsService userDetailsService() {
//
//        return email -> userRepository
//
//                .findByEmail(email)
//
//                .orElseThrow(
//
//                        () -> new UsernameNotFoundException(
//
//                                "Utilisateur introuvable : "
//
//                                        + email
//
//                        )
//
//                );
//
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =

                new DaoAuthenticationProvider();

        provider.setUserDetailsService(

                userDetailsService

        );

        provider.setPasswordEncoder(

                passwordEncoder()

        );

        return provider;

    }

    @Bean
    public AuthenticationManager authenticationManager(

            AuthenticationConfiguration configuration

    ) throws Exception {

        return configuration.getAuthenticationManager();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =

                new CorsConfiguration();

        config.setAllowedOrigins(

                Arrays.asList(

                        origins.split(",")

                )

        );

        config.setAllowedMethods(

                List.of(

                        "GET",

                        "POST",

                        "PUT",

                        "DELETE",

                        "OPTIONS"

                )

        );

        config.setAllowedHeaders(

                List.of(

                        "Authorization",

                        "Content-Type",

                        "Accept"

                )

        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =

                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(

                "/**",

                config

        );

        return source;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(

            HttpSecurity http

    ) throws Exception {

        return http

                .csrf(

                        AbstractHttpConfigurer::disable

                )

                .cors(

                        cors -> cors.configurationSource(

                                corsConfigurationSource()

                        )

                )

                .sessionManagement(

                        session -> session

                                .sessionCreationPolicy(

                                        SessionCreationPolicy.STATELESS

                                )

                )

                .authorizeHttpRequests(

                        auth -> auth

                                .requestMatchers(

                                        "/api/v1/auth/**",

                                        "/actuator/**"

                                )

                                .permitAll()

                                .anyRequest()

                                .authenticated()

                )

                .authenticationProvider(

                        authenticationProvider()

                )

                .addFilterBefore(

                        jwtFilter,

                        UsernamePasswordAuthenticationFilter.class

                )

                .build();

    }

}