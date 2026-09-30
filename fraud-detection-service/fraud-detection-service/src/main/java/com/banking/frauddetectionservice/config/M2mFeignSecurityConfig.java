package com.banking.frauddetectionservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class M2mFeignSecurityConfig {

    @Value("${auth0.audience}")
    private String audience;

    /**
     * In Spring Boot 4.1+ / Spring Security 6.4+, you register a standalone token response client bean.
     * It uses the non-deprecated RestClient implementation to force append custom parameters like 'audience'.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsAccessTokenResponseClient() {
        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();

        // Custom parameters hook for modern Spring Security
        tokenResponseClient.setParametersConverter(grantRequest -> {
            var parameters = new org.springframework.util.LinkedMultiValueMap<String, String>();
            parameters.add("audience", audience);
            return parameters;
        });

        return tokenResponseClient;
    }

    @Bean
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
            OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> responseClient) {

        // Inject the responseClient bean straight into the standard builder configuration
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials(clientCredentials ->
                                clientCredentials.accessTokenResponseClient(responseClient))
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, oAuth2AuthorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RequestInterceptor m2mFeignRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId("auth0-m2m")
                        .principal("fraud-detection-service")
                        .build();

                OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    String tokenValue = authorizedClient.getAccessToken().getTokenValue();
                    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue);
                }
            }
        };
    }
}
