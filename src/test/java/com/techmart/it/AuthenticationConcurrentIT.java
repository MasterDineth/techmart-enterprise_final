package com.techmart.it;

import com.techmart.dto.AuthResponse;
import com.techmart.dto.LoginRequest;
import com.techmart.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@DisplayName("Authentication Integration Tests - Concurrent Load")
public class AuthenticationConcurrentIT {

    private static final String BASE_URL = "http://localhost:8080/techmart/api/auth";
    private static final Client client = ClientBuilder.newClient();
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);

    @Test
    @DisplayName("Should handle 50 concurrent registrations")
    public void testConcurrentRegistrations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int index = i;
            futures.add(CompletableFuture.supplyAsync(() -> registerUser(index), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Concurrent Registrations - Success: " + successCount.get() + ", Failures: " + failureCount.get());
        assert successCount.get() >= 45 : "Expected at least 45 successful registrations";

        executor.shutdown();
        successCount.set(0);
        failureCount.set(0);
    }

    @Test
    @DisplayName("Should handle 100 concurrent logins")
    public void testConcurrentLogins() throws Exception {
        // First register 20 test users
        for (int i = 0; i < 20; i++) {
            registerUser(i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final int userIndex = i % 20;
            futures.add(CompletableFuture.supplyAsync(() -> loginUser(userIndex), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Concurrent Logins - Success: " + successCount.get() + ", Failures: " + failureCount.get());
        assert successCount.get() >= 90 : "Expected at least 90 successful logins";

        executor.shutdown();
        successCount.set(0);
        failureCount.set(0);
    }

    @Test
    @DisplayName("Should track concurrent sessions per user")
    public void testConcurrentSessionTracking() throws Exception {
        String username = "testuser_" + System.currentTimeMillis();
        String email = username + "@test.com";

        // Register user
        RegisterRequest reg = new RegisterRequest(username, email, "password123");
        Response regRes = client.target(BASE_URL + "/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(reg, MediaType.APPLICATION_JSON));

        assert regRes.getStatus() == 201;
        regRes.close();

        // Perform 5 concurrent logins
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<String>> sessionFutures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            sessionFutures.add(CompletableFuture.supplyAsync(() -> {
                LoginRequest login = new LoginRequest(username, "password123");
                Response res = client.target(BASE_URL + "/login")
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(login, MediaType.APPLICATION_JSON));

                if (res.getStatus() == 200) {
                    AuthResponse auth = res.readEntity(AuthResponse.class);
                    res.close();
                    System.out.println("Login successful. Current concurrent sessions: " + auth.getConcurrentSessions());
                    return auth.getSessionToken();
                }
                res.close();
                return null;
            }, executor));
        }

        List<String> sessionTokens = new ArrayList<>();
        for (CompletableFuture<String> future : sessionFutures) {
            String token = future.join();
            if (token != null) {
                sessionTokens.add(token);
            }
        }

        System.out.println("Concurrent sessions created: " + sessionTokens.size());
        assert sessionTokens.size() >= 4 : "Expected at least 4 concurrent sessions";

        executor.shutdown();
    }

    @Test
    @DisplayName("Should validate session tokens under load")
    public void testSessionValidationUnderLoad() throws Exception {
        // Create a user and session
        String username = "validator_" + System.currentTimeMillis();
        String email = username + "@test.com";

        RegisterRequest reg = new RegisterRequest(username, email, "password123");
        Response regRes = client.target(BASE_URL + "/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(reg, MediaType.APPLICATION_JSON));
        assert regRes.getStatus() == 201;
        regRes.close();

        LoginRequest login = new LoginRequest(username, "password123");
        Response loginRes = client.target(BASE_URL + "/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(login, MediaType.APPLICATION_JSON));
        assert loginRes.getStatus() == 200;

        AuthResponse authRes = loginRes.readEntity(AuthResponse.class);
        String sessionToken = authRes.getSessionToken();
        loginRes.close();

        // Validate session 100 times concurrently
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                Response validRes = client.target(BASE_URL + "/validate")
                        .request(MediaType.APPLICATION_JSON)
                        .header("X-Session-Token", sessionToken)
                        .get();
                boolean valid = validRes.getStatus() == 200;
                validRes.close();
                return valid;
            }, executor));
        }

        long validSessions = futures.stream()
                .map(CompletableFuture::join)
                .filter(b -> b)
                .count();

        System.out.println("Session validations successful: " + validSessions + "/100");
        assert validSessions >= 95 : "Expected at least 95 successful validations";

        executor.shutdown();
    }

    private boolean registerUser(int index) {
        try {
            String username = "user_" + System.currentTimeMillis() + "_" + index;
            String email = username + "@test.com";
            RegisterRequest req = new RegisterRequest(username, email, "password123");

            Response res = client.target(BASE_URL + "/register")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(req, MediaType.APPLICATION_JSON));

            boolean success = res.getStatus() == 201 || res.getStatus() == 409; // 409 = conflict (already exists)
            if (success) successCount.incrementAndGet();
            else failureCount.incrementAndGet();

            res.close();
            return success;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            return false;
        }
    }

    private boolean loginUser(int index) {
        try {
            String username = "user_" + index;
            LoginRequest req = new LoginRequest(username, "password123");

            Response res = client.target(BASE_URL + "/login")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(req, MediaType.APPLICATION_JSON));

            boolean success = res.getStatus() == 200;
            if (success) successCount.incrementAndGet();
            else failureCount.incrementAndGet();

            res.close();
            return success;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            return false;
        }
    }
}