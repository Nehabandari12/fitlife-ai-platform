package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Sets X-User-ID for downstream services from the JWT that Spring Security has already validated,
 * and creates the user in the user service on first sight.
 *
 * Whatever X-User-ID the client sent is removed first, so a caller can never choose the identity
 * downstream services see. Without an authenticated principal (the public /api/auth/** routes, or
 * if this filter ever ran before authentication) the request goes on with no X-User-ID at all.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    public static final String USER_ID_HEADER = "X-User-ID";

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange stripped = exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove(USER_ID_HEADER)))
                .build();

        return stripped.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(principal -> Optional.of(((JwtAuthenticationToken) principal).getToken()))
                .defaultIfEmpty(Optional.empty())
                .flatMap(jwt -> jwt.isEmpty()
                        ? chain.filter(stripped)
                        : syncUser(jwt.get()).then(Mono.defer(() -> chain.filter(withUserId(stripped, jwt.get())))));
    }

    private static ServerWebExchange withUserId(ServerWebExchange exchange, Jwt jwt) {
        return exchange.mutate()
                .request(request -> request.header(USER_ID_HEADER, jwt.getSubject()))
                .build();
    }

    private Mono<Void> syncUser(Jwt jwt) {
        String bearer = jwt.getTokenValue();
        return userService.validateUser(jwt.getSubject(), bearer)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.empty();
                    }
                    log.info("First request from user {}; registering in the user service", jwt.getSubject());
                    return userService.registerUser(fromClaims(jwt), bearer).then();
                });
    }

    static RegisterRequest fromClaims(Jwt jwt) {
        RegisterRequest request = new RegisterRequest();
        request.setKeycloakId(jwt.getSubject());
        request.setEmail(jwt.getClaimAsString("email"));
        request.setFirstName(jwt.getClaimAsString("given_name"));
        request.setLastName(jwt.getClaimAsString("family_name"));
        return request;
    }
}
