package com.banking.accountservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects any JWT whose "aud" claim does not contain the expected API identifier.
 */
@Slf4j
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audience;

    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
            log.info("config audience: {} matched with jwt audience: {}", audience, jwt.getAudience());
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required audience '" + audience + "' is missing",
                null);
        log.info("missing required audience: {}, found: {}", audience, jwt.getAudience());
        return OAuth2TokenValidatorResult.failure(error);
    }
}