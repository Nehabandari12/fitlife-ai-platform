package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserResponse;
import com.fitness.gateway.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Downstream services must only ever see the user id of the validated token. */
class KeycloakUserSyncFilterTest {

    private boolean userExists = true;
    private final List<RegisterRequest> registered = new ArrayList<>();
    private final List<String> forwardedTokens = new ArrayList<>();

    private final UserService users = new UserService(null) {
        @Override
        public Mono<Boolean> validateUser(String userId, String bearerToken) {
            forwardedTokens.add(bearerToken);
            return Mono.just(userExists);
        }

        @Override
        public Mono<UserResponse> registerUser(RegisterRequest request, String bearerToken) {
            registered.add(request);
            return Mono.just(new UserResponse());
        }
    };

    private final KeycloakUserSyncFilter filter = new KeycloakUserSyncFilter(users);

    private static Jwt token(String subject) {
        return Jwt.withTokenValue("token-of-" + subject)
                .header("alg", "RS256")
                .subject(subject)
                .claim("email", subject + "@example.com")
                .claim("given_name", "Alice")
                .build();
    }

    /** Runs the filter and returns the request the rest of the chain (the router) would see. */
    private ServerHttpRequest forwarded(MockServerHttpRequest request, Jwt jwt) {
        MockServerWebExchange.Builder exchange = MockServerWebExchange.builder(request);
        if (jwt != null) {
            exchange.principal(new JwtAuthenticationToken(jwt));
        }
        AtomicReference<ServerHttpRequest> seen = new AtomicReference<>();
        filter.filter(exchange.build(), next -> {
            seen.set(next.getRequest());
            return Mono.empty();
        }).block();
        return seen.get();
    }

    @Test
    void aForgedUserIdHeaderIsReplacedByTheTokenSubject() {
        ServerHttpRequest out = forwarded(
                MockServerHttpRequest.get("/api/activities").header("X-User-ID", "victim").build(), token("alice"));

        assertThat(out.getHeaders().get("X-User-ID")).containsExactly("alice");
        assertThat(forwardedTokens).containsExactly("token-of-alice");
    }

    @Test
    void withoutAnAuthenticatedPrincipalNoUserIdIsForwarded() {
        ServerHttpRequest out = forwarded(
                MockServerHttpRequest.post("/api/auth/register").header("X-User-ID", "victim").build(), null);

        assertThat(out.getHeaders().containsKey("X-User-ID")).isFalse();
        assertThat(forwardedTokens).isEmpty();
    }

    @Test
    void aNewUserIsRegisteredFromTheTokenClaims() {
        userExists = false;
        forwarded(MockServerHttpRequest.get("/api/activities").build(), token("alice"));

        assertThat(registered).hasSize(1);
        assertThat(registered.get(0).getKeycloakId()).isEqualTo("alice");
        assertThat(registered.get(0).getEmail()).isEqualTo("alice@example.com");
        assertThat(registered.get(0).getPassword()).isNull();
    }
}
