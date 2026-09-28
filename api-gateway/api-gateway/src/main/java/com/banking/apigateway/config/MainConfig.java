package com.banking.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class MainConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain (ServerHttpSecurity http){

        http.authorizeExchange(exchanges -> exchanges
                // Public: health check, and the paths Spring Security itself uses
                // to run the OAuth2 login redirect/callback dance.
                .pathMatchers(
                        "/actuator/health/**",
                        "/login/**",
                        "/oauth2/**"
                ).permitAll()
                .anyExchange().authenticated()
        )
        // Same call regardless of provider — Spring Security reads whichever
        // registration(s) are configured in application.yml (here: "auth0").
        .oauth2Login(oauth2 -> {})
        .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Adds "audience=<your API identifier>" to the authorization request sent to Auth0.
     * Without this parameter Auth0 returns an opaque access token (only valid for /userinfo).
     * With it, Auth0 returns a JWT access token whose "aud" claim contains your API identifier.
     */
    @Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        DefaultServerOAuth2AuthorizationRequestResolver resolver =
                new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> params.put("audience", "https://api.banking.com")));

        return resolver;
    }
}
