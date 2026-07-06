package com.techmart.rest;

import com.techmart.dto.AuthResponse;
import com.techmart.dto.LoginRequest;
import com.techmart.dto.RegisterRequest;
import com.techmart.entity.User;
import com.techmart.entity.UserSession;
import com.techmart.service.AuthService;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for user authentication, registration, and session management.
 * Supports concurrent login testing scenarios.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @EJB
    private AuthService authService;

    /**
     * Register a new user
     */
    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getEmail() == null || request.getEmail().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().length() < 6) {
            
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new AuthResponse(false, "Invalid input: username, email, and password (min 6 chars) required"))
                    .build();
        }

        try {
            User user = authService.register(request.getUsername(), request.getEmail(), request.getPassword());
            AuthResponse resp = new AuthResponse(true, "User registered successfully");
            resp.setUserId(user.getId());
            resp.setUsername(user.getUsername());
            return Response.status(Response.Status.CREATED).entity(resp).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new AuthResponse(false, e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new AuthResponse(false, "Registration failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Login user and create session
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request, @Context HttpServletRequest httpRequest) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().isEmpty()) {
            
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new AuthResponse(false, "Username and password required"))
                    .build();
        }

        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);
            
            UserSession session = authService.login(request.getUsername(), request.getPassword(), userAgent, ipAddress);
            User user = authService.findUserById(session.getUserId());

            AuthResponse resp = new AuthResponse(true, "Login successful");
            resp.setSessionToken(session.getSessionToken());
            resp.setUserId(session.getUserId());
            resp.setUsername(user.getUsername());
            resp.setConcurrentSessions(user.getConcurrentSessions());

            return Response.ok(resp)
                    .header("X-Session-Token", session.getSessionToken())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponse(false, e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new AuthResponse(false, "Login failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Logout user session
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("X-Session-Token") String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new AuthResponse(false, "Session token required"))
                    .build();
        }

        try {
            authService.logout(sessionToken);
            return Response.ok(new AuthResponse(true, "Logout successful")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new AuthResponse(false, "Logout failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Validate session
     */
    @GET
    @Path("/validate")
    public Response validateSession(@HeaderParam("X-Session-Token") String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponse(false, "Session token required"))
                    .build();
        }

        UserSession session = authService.validateSession(sessionToken);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponse(false, "Invalid or expired session"))
                    .build();
        }

        User user = authService.findUserById(session.getUserId());
        AuthResponse resp = new AuthResponse(true, "Session valid");
        resp.setUserId(session.getUserId());
        resp.setUsername(user.getUsername());
        resp.setConcurrentSessions(user.getConcurrentSessions());

        return Response.ok(resp).build();
    }

    /**
     * Get active sessions for current user
     */
    @GET
    @Path("/sessions")
    public Response getActiveSessions(@HeaderParam("X-Session-Token") String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponse(false, "Session token required"))
                    .build();
        }

        UserSession currentSession = authService.validateSession(sessionToken);
        if (currentSession == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponse(false, "Invalid session"))
                    .build();
        }

        var activeSessions = authService.getActiveSessions(currentSession.getUserId());
        return Response.ok(activeSessions).build();
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
