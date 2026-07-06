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

@Stateless
@Monitored
public class AuthService {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    // FIXED: Reduced iterations to prevent CPU starvation during concurrent load testing
    private static final int HASH_ITERATIONS = 10000;
    private static final SecureRandom random = new SecureRandom();

    public User register(String username, String email, String plainPassword) throws Exception {
        try {
            em.createNamedQuery("User.byUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            throw new IllegalArgumentException("Username already exists");
        } catch (NoResultException e) {}

        try {
            em.createNamedQuery("User.byEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new IllegalArgumentException("Email already registered");
        } catch (NoResultException e) {}

        String passwordHash = hashPassword(plainPassword);
        User user = new User(username, email, passwordHash);
        em.persist(user);
        return user;
    }

    public UserSession login(String username, String plainPassword, String userAgent, String ipAddress) throws Exception {
        User user = findUserByUsername(username);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!verifyPassword(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setLastLogin(LocalDateTime.now());

        int activeSessions = countActiveSessions(user.getId());
        user.setConcurrentSessions(activeSessions + 1);
        em.merge(user);

        String sessionToken = generateSessionToken();
        UserSession session = new UserSession(user.getId(), sessionToken, userAgent, ipAddress);
        em.persist(session);

        return session;
    }

    public void logout(String sessionToken) {
        try {
            UserSession session = em.createNamedQuery("UserSession.byToken", UserSession.class)
                    .setParameter("token", sessionToken)
                    .getSingleResult();

            session.setActive(false);
            em.merge(session);

            User user = em.find(User.class, session.getUserId());
            if (user != null) {
                int activeSessions = countActiveSessions(user.getId());
                user.setConcurrentSessions(Math.max(0, activeSessions));
                em.merge(user);
            }
        } catch (NoResultException e) {}
    }

    public UserSession validateSession(String sessionToken) {
        try {
            UserSession session = em.createNamedQuery("UserSession.byToken", UserSession.class)
                    .setParameter("token", sessionToken)
                    .getSingleResult();

            if (!session.isActive()) {
                return null;
            }

            session.setLastAccessed(LocalDateTime.now());
            em.merge(session);

            return session;
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<UserSession> getActiveSessions(Long userId) {
        return em.createNamedQuery("UserSession.activeSessions", UserSession.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // FIXED: Refactored to execute a highly efficient COUNT query instead of pulling all records into memory
    private int countActiveSessions(Long userId) {
        Number count = (Number) em.createQuery("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.active = true")
                .setParameter("userId", userId)
                .getSingleResult();
        return count.intValue();
    }

    public User findUserByUsername(String username) {
        try {
            return em.createNamedQuery("User.byUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public User findUserById(Long userId) {
        return em.find(User.class, userId);
    }

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

    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}