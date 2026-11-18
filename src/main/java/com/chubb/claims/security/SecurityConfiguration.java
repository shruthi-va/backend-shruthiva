package com.chubb.claims.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService demoUsers(
            PasswordEncoder passwordEncoder,
            @Value("${claims.demo-users.claimant-username}") String claimantUsername,
            @Value("${claims.demo-users.claimant-password}") String claimantPassword,
            @Value("${claims.demo-users.staff-username}") String staffUsername,
            @Value("${claims.demo-users.staff-password}") String staffPassword,
            @Value("${claims.demo-users.manager-username}") String managerUsername,
            @Value("${claims.demo-users.manager-password}") String managerPassword) {
        return new InMemoryUserDetailsManager(
                user(claimantUsername, claimantPassword, "CLAIMANT", passwordEncoder),
                user(staffUsername, staffPassword, "CLAIMS_STAFF", passwordEncoder),
                user(managerUsername, managerPassword, "MANAGER", passwordEncoder));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    private UserDetails user(
            String username, String password, String role, PasswordEncoder passwordEncoder) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Configure distinct demo usernames and non-empty passwords for claimant, staff, and manager.");
        }
        return User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build();
    }
}
