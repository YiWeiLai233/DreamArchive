package com.yiweilai.DreamArchive.service;

import com.sun.net.httpserver.HttpServer;
import com.yiweilai.DreamArchive.DTO.AiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderHeartbeatServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void heartbeatReportsLatencyAndSuccessForEnabledProvider() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(200, requestCount);
        AiProvider provider = provider("local", baseUrl(), true);
        AiProviderPool pool = poolWith(provider);
        AiProviderHeartbeatService service = service(pool);

        service.checkAllProviders();

        assertEquals(1, requestCount.get());
        assertTrue(provider.getAvgLatencyMs() > 0);
        assertEquals(0, provider.getFailCount());
        assertFalse(provider.isCircuitOpen());
    }

    @Test
    void heartbeatSkipsDisabledProviders() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(200, requestCount);
        AiProvider provider = provider("disabled", baseUrl(), false);
        AiProviderPool pool = poolWith(provider);
        AiProviderHeartbeatService service = service(pool);

        service.checkAllProviders();

        assertEquals(0, requestCount.get());
        assertEquals(0, provider.getAvgLatencyMs());
        assertEquals(0, provider.getFailCount());
    }

    @Test
    void heartbeatDoesNotCallProvidersWhenConfigDisabled() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(200, requestCount);
        AiProvider provider = provider("local", baseUrl(), true);
        AiProviderPool pool = poolWith(provider);
        AiProviderHeartbeatService service = service(pool, false, Duration.ofSeconds(2));

        service.checkAllProviders();

        assertEquals(0, requestCount.get());
        assertEquals(0, provider.getAvgLatencyMs());
        assertEquals(0, provider.getFailCount());
    }

    @Test
    void heartbeatReportsFailureForNonSuccessResponse() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(500, requestCount);
        AiProvider provider = provider("broken", baseUrl(), true);
        AiProviderPool pool = poolWith(provider);
        AiProviderHeartbeatService service = service(pool);

        service.checkAllProviders();

        assertEquals(1, requestCount.get());
        assertTrue(provider.getAvgLatencyMs() > 0);
        assertEquals(1, provider.getFailCount());
        assertFalse(provider.isCircuitOpen());
    }

    @Test
    void startupHeartbeatRunsAfterApplicationReadyAndUsesTaskExecutor() throws Exception {
        Method method = AiProviderHeartbeatService.class.getMethod("warmUpAfterStartup");

        EventListener eventListener = method.getAnnotation(EventListener.class);
        Async async = method.getAnnotation(Async.class);

        assertNotNull(eventListener);
        assertEquals(1, eventListener.value().length);
        assertEquals(ApplicationReadyEvent.class, eventListener.value()[0]);
        assertNotNull(async);
        assertEquals("taskExecutor", async.value());
    }

    @Test
    void springContextCreatesHeartbeatServiceWithConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AiProviderPool.class, AiProviderHeartbeatService.class);

            assertDoesNotThrow(context::refresh);

            assertNotNull(context.getBean(AiProviderHeartbeatService.class));
        }
    }

    private void startServer(int statusCode, AtomicInteger requestCount) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}".getBytes();
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1";
    }

    private AiProvider provider(String name, String url, boolean enabled) {
        return new AiProvider(name, url, "test-key", "test-model", 10, enabled);
    }

    private AiProviderPool poolWith(AiProvider provider) {
        AiProviderPool pool = new AiProviderPool();
        pool.setProviders(List.of(provider));
        pool.init();
        return pool;
    }

    private AiProviderHeartbeatService service(AiProviderPool pool) {
        return service(pool, true, Duration.ofSeconds(2));
    }

    private AiProviderHeartbeatService service(AiProviderPool pool, boolean heartbeatEnabled, Duration requestTimeout) {
        return new AiProviderHeartbeatService(
                pool,
                heartbeatEnabled,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                requestTimeout
        );
    }
}
