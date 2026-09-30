package com.banking.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class MainConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        // CORS preflight requests never carry a token, so they must pass.
                        // The gateway's globalcors config answers them.
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .anyExchange().authenticated()
                )
                // Validates "Authorization: Bearer <jwt>" on every request:
                // signature, expiry, issuer and audience (configured in application.yml).
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // Stateless: no WebSession, no SESSION cookie. The token is the only credential.
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // CSRF protects cookie-based auth. With bearer tokens in a header
                // there is no ambient credential for a malicious site to abuse.
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }
}
