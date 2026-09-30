package com.banking.apigateway.controller;


import org.springframework.web.bind.annotation.RestController;

@RestController

public class AppController {
    /*@GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user,
                                        @RegisteredOAuth2AuthorizedClient("auth0") OAuth2AuthorizedClient authorizedClient) {

        // This is your raw, encoded JWT ID token string ('eyJhbGci...')
        String rawIdToken = user.getIdToken().getTokenValue();
        System.out.println("Raw JWT ID Token: " + rawIdToken);

        // This is the access token used for securing downstream microservice APIs
        String rawAccessToken = authorizedClient.getAccessToken().getTokenValue();
        System.out.println("Raw Access Token: " + rawAccessToken);

        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    System.out.println("Reactive Auth Object: " + authentication);
                    System.out.println("AuthenticationPrincipal OidcUser Object: " + user);
                    return authentication;
                })
                .map(auth -> Map.of(
                        "subject", user.getSubject(),
                        "email", user.getEmail() != null ? user.getEmail() : "(no email scope/claim returned)",
                        "name", user.getFullName() != null ? user.getFullName() : "(no name claim returned)"
                ));
    }*/
}
