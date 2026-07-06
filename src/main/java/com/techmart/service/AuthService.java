package com.techmart.service;

import com.techmart.entity.User;
import com.techmart.entity.UserSession;
import com.techmart.interceptor.Monitored;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Stateless bean handling user authentication, registration, and session management.
 * Supports concurrent login tracking for load testing scenarios.
 */
@Stateless
@Monitored
public class AuthService {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    private static final int HASH_ITERATIONS = 100000;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Register a new user with bcrypt-like hashing
     */
    public User register(String username, String email, String plainPassword) throws Exception {
        // Check if user already exists
        try {
            em.createNamedQuery("User.byUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            throw new IllegalArgumentException("Username already exists");
        } catch (NoResultException e) {
            // User doesn't exist, proceed
        }

        try {
            em.createNamedQuery("User.byEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new IllegalArgumentException("Email already registered");
        } catch (NoResultException e) {
            // Email not registered, proceed
        }

        String passwordHash = hashPassword(plainPassword);
        User user = new User(username, email, passwordHash);
        em.persist(user);
        return user;
    }

    /**
     * Login user and create session
     */
    public UserSession login(String username, String plainPassword, String userAgent, String ipAddress) throws Exception {
        User user = findUserByUsername(username);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!verifyPassword(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        
        // Increment concurrent session count
        int activeSessions = countActiveSessions(user.getId());
        user.setConcurrentSessions(activeSessions + 1);
        em.merge(user);

        // Create new session token
        String sessionToken = generateSessionToken();
        UserSession session = new UserSession(user.getId(), sessionToken, userAgent, ipAddress);
        em.persist(session);

        return session;
    }

    /**
     * Logout user session
     */
    public void logout(String sessionToken) {
        try {
            UserSession session = em.createNamedQuery("UserSession.byToken", UserSession.class)
                    .setParameter("token", sessionToken)
                    .getSingleResult();
            
            session.setActive(false);
            em.merge(session);

            // Decrement concurrent session count
            User user = em.find(User.class, session.getUserId());
            if (user != null) {
                int activeSessions = countActiveSessions(user.getId());
                user.setConcurrentSessions(Math.max(0, activeSessions - 1));
                em.merge(user);
            }
        } catch (NoResultException e) {
            // Session not found, silently ignore
        }
    }

    /**
     * Validate session token
     */
    public UserSession validateSession(String sessionToken) {
        try {
            UserSession session = em.createNamedQuery("UserSession.byToken", UserSession.class)
                    .setParameter("token", sessionToken)
                    .getSingleResult();

            if (!session.isActive()) {
                return null;
            }

            // Update last accessed
            session.setLastAccessed(LocalDateTime.now());
            em.merge(session);

            return session;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Get active sessions for a user
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return em.createNamedQuery("UserSession.activeSessions", UserSession.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Count active sessions
     */
    private int countActiveSessions(Long userId) {
        return (int) em.createNamedQuery("UserSession.activeSessions", UserSession.class)
                .setParameter("userId", userId)
                .getResultList()
                .size();
    }

    /**
     * Find user by username
     */
    public User findUserByUsername(String username) {
        try {
            return em.createNamedQuery("User.byUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find user by ID
     */
    public User findUserById(Long userId) {
        return em.find(User.class, userId);
    }

    /**
     * Hash password using PBKDF2-like approach
     */
    private String hashPassword(String plainPassword) throws Exception {
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hash = md.digest(plainPassword.getBytes());

        for (int i = 1; i < HASH_ITERATIONS; i++) {
            md.reset();
            hash = md.digest(hash);
        }

        byte[] combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Verify password against hash
     */
    private boolean verifyPassword(String plainPassword, String hashString) throws Exception {
        byte[] combined = Base64.getDecoder().decode(hashString);
        byte[] salt = new byte[32];
        System.arraycopy(combined, 0, salt, 0, 32);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hash = md.digest(plainPassword.getBytes());

        for (int i = 1; i < HASH_ITERATIONS; i++) {
            md.reset();
            hash = md.digest(hash);
        }

        byte[] storedHash = new byte[combined.length - 32];
        System.arraycopy(combined, 32, storedHash, 0, storedHash.length);

        return java.util.Arrays.equals(hash, storedHash);
    }

    /**
     * Generate secure session token
     */
    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
