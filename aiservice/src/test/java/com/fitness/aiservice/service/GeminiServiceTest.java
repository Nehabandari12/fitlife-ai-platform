package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.service.AiResponseException.Reason;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Gemini's failure modes against a local stand-in server: no network and no API key needed. */
class GeminiServiceTest {

    private HttpServer server;
    private final AtomicReference<Reply> reply = new AtomicReference<>();
    private final AtomicReference<String> receivedKey = new AtomicReference<>();

    record Reply(int status, String body, long delayMillis) {
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/generate", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("X-goog-api-key"));
            Reply r = reply.get();
            try {
                Thread.sleep(r.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = r.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(r.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // the client gave up (timeout test)
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GeminiService client(String key, Duration timeout) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/generate";
        return new GeminiService(WebClient.builder(), new ObjectMapper(), url, key, timeout);
    }

    private GeminiService client() {
        return client("test-key", Duration.ofSeconds(5));
    }

    private static Reason reasonOf(Runnable call) {
        try {
            call.run();
        } catch (AiResponseException e) {
            return e.getReason();
        }
        throw new AssertionError("expected an AiResponseException");
    }

    @Test
    void returnsTheFirstCandidateTextAndSendsTheKeyAsAHeader() {
        reply.set(new Reply(200, """
                {"candidates": [{"content": {"parts": [{"text": "hello"}], "role": "model"}, "finishReason": "STOP"}]}
                """, 0));
        assertThat(client().generate("prompt")).isEqualTo("hello");
        assertThat(receivedKey.get()).isEqualTo("test-key");
    }

    @Test
    void aBlockedPromptHasNoCandidates() {
        reply.set(new Reply(200, """
                {"promptFeedback": {"blockReason": "SAFETY"}}
                """, 0));
        assertThatThrownBy(() -> client().generate("prompt"))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void anEmptyCandidateHasNoCandidates() {
        reply.set(new Reply(200, """
                {"candidates": [{"finishReason": "MAX_TOKENS", "content": {"parts": []}}]}
                """, 0));
        assertThat(reasonOf(() -> client().generate("prompt"))).isEqualTo(Reason.NO_CANDIDATES);
    }

    @Test
    void anHttpErrorIsAModelError() {
        reply.set(new Reply(429, "{\"error\": {\"code\": 429}}", 0));
        assertThat(reasonOf(() -> client().generate("prompt"))).isEqualTo(Reason.MODEL_ERROR);
    }

    @Test
    void aReplyThatIsNotJsonIsMalformed() {
        reply.set(new Reply(200, "<html>proxy error</html>", 0));
        assertThat(reasonOf(() -> client().generate("prompt"))).isEqualTo(Reason.MALFORMED_RESPONSE);
    }

    @Test
    void aSlowReplyTimesOut() {
        reply.set(new Reply(200, "{}", 3000));
        assertThat(reasonOf(() -> client("test-key", Duration.ofMillis(300)).generate("prompt"))).isEqualTo(Reason.TIMEOUT);
    }

    @Test
    void anUnreachableServerIsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        GeminiService unreachable = new GeminiService(WebClient.builder(), new ObjectMapper(),
                "http://127.0.0.1:" + closedPort + "/generate", "test-key", Duration.ofSeconds(5));
        assertThat(reasonOf(() -> unreachable.generate("prompt"))).isEqualTo(Reason.UNAVAILABLE);
    }

    @Test
    void withoutAKeyNothingIsSent() {
        reply.set(new Reply(200, "{}", 0));
        assertThat(reasonOf(() -> client("", Duration.ofSeconds(5)).generate("prompt"))).isEqualTo(Reason.NOT_CONFIGURED);
        assertThat(receivedKey.get()).isNull();
    }
}
