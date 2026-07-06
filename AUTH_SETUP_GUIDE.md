# TechMart Enterprise - Authentication Module & Load Testing Guide

## 📋 Summary

A complete user authentication system has been implemented with support for:
- ✅ User registration & login
- ✅ Session management with concurrent login tracking
- ✅ Secure password hashing (PBKDF2-based)
- ✅ Concurrent user testing capabilities
- ✅ Load testing with JMeter
- ✅ Integration tests for authentication scenarios

---

## 🗄️ Database Changes

### New Tables Added

#### 1. `app_user` - User Account Management
```sql
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    concurrent_session_count INT NOT NULL DEFAULT 0,
    KEY idx_user_active (active),
    KEY idx_user_email (email),
    KEY idx_user_created (created_at)
);
```

#### 2. `user_session` - Session Tracking for Concurrent Logins
```sql
CREATE TABLE user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed TIMESTAMP NULL,
    user_agent VARCHAR(512),
    ip_address VARCHAR(45),
    KEY idx_session_token (session_token),
    KEY idx_session_user (user_id),
    KEY idx_session_active (active),
    KEY idx_session_created (created_at),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
```

#### 3. `auth_metrics` - Authentication Performance Metrics
```sql
CREATE TABLE auth_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    response_time_ms INT NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(255),
    thread_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_metrics_type (event_type),
    KEY idx_metrics_created (created_at),
    KEY idx_metrics_success (success)
);
```

### Database Migration
Run the updated schema:
```bash
mysql -u root -p < db/schema.sql
```

---

## 🔐 New Components Created

### Entities
- **`User.java`** - User entity with password hashing and concurrent session tracking
- **`UserSession.java`** - Session entity for tracking multiple concurrent logins per user

### Services
- **`AuthService.java`** - Business logic for:
  - User registration with email/username validation
  - Login with password verification
  - Session creation and validation
  - Concurrent session tracking
  - Logout functionality

### REST Endpoints (AuthResource.java)
**Base Path:** `/api/auth`

#### POST /register
Register a new user
```json
Request:
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securePass123"
}

Response (201 Created):
{
  "success": true,
  "message": "User registered successfully",
  "userId": 1,
  "username": "john_doe"
}
```

#### POST /login
Authenticate and create session
```json
Request:
{
  "username": "john_doe",
  "password": "securePass123"
}

Response (200 OK):
{
  "success": true,
  "message": "Login successful",
  "sessionToken": "base64-encoded-secure-token",
  "userId": 1,
  "username": "john_doe",
  "concurrentSessions": 2
}
```

#### GET /validate
Validate current session
```
Headers: X-Session-Token: <token>

Response (200 OK):
{
  "success": true,
  "message": "Session valid",
  "userId": 1,
  "username": "john_doe",
  "concurrentSessions": 2
}
```

#### POST /logout
Logout session
```
Headers: X-Session-Token: <token>

Response (200 OK):
{
  "success": true,
  "message": "Logout successful"
}
```

#### GET /sessions
Get all active sessions for user
```
Headers: X-Session-Token: <token>

Response (200 OK):
[
  {
    "id": 1,
    "userId": 1,
    "sessionToken": "token1",
    "active": true,
    "createdAt": "2026-07-06T11:00:00",
    "lastAccessed": "2026-07-06T11:45:00",
    "userAgent": "Mozilla/5.0...",
    "ipAddress": "192.168.1.100"
  },
  {
    "id": 2,
    "userId": 1,
    "sessionToken": "token2",
    "active": true,
    "createdAt": "2026-07-06T11:30:00",
    "lastAccessed": "2026-07-06T11:40:00",
    "userAgent": "Mozilla/5.0...",
    "ipAddress": "192.168.1.101"
  }
]
```

### Web Pages

#### login.html
- User registration form
- User login form
- Session token management
- Responsive design for concurrent testing

#### index.html (Dashboard)
- Updated with session validation
- Concurrent load test simulator
- Session info display in header
- Logout button

---

## 🧪 Testing - Concurrent Scenarios

### Integration Tests
**File:** `AuthenticationConcurrentIT.java`

Run concurrent authentication tests:
```bash
mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly
```

#### Test Scenarios:
1. **50 Concurrent Registrations** - Tests user creation under load
2. **100 Concurrent Logins** - Tests authentication throughput
3. **5 Concurrent Sessions Per User** - Tests session tracking
4. **100 Concurrent Session Validations** - Tests token validation performance

### Unit Tests
```bash
mvn test
```

### Load Testing with JMeter

#### Prerequisites
1. Download JMeter: https://jmeter.apache.org/download_jmeter.html
2. Ensure server is running on `http://localhost:8080`

#### Quick Start
```bash
# Open JMeter
jmeter -t TechMart-Auth-LoadTest.jmx

# Or run headless
jmeter -n -t TechMart-Auth-LoadTest.jmx -l results.jtl -j jmeter.log -e -o report/
```

#### JMeter Test Configuration

**Default Parameters:**
- Threads: 20 concurrent users
- Ramp-up: 10 seconds
- Loop count: 5 iterations per thread
- Total requests: 200 (20 threads × 5 loops × 2 scenarios)

**Results Output:**
- `jmeter-results.jtl` - JMeter results file
- Graph visualization showing:
  - Response times over time
  - Throughput (requests/second)
  - Error rate
  - Average/Min/Max response times

#### Interpreting Results

**Good Performance Metrics:**
- Response time < 500ms for auth endpoints
- Error rate < 1%
- Throughput > 10 req/s for registration
- Throughput > 50 req/s for login

**Graph Interpretation:**
1. **Response Time Line Chart** - Shows latency trend
2. **Throughput Chart** - Shows requests/second over time
3. **Summary Statistics** - Mean, median, min, max, 90th/95th/99th percentiles

---

## 📊 Built-in Dashboard Load Test

The dashboard now includes a concurrent load test simulator:

1. Open `http://localhost:8080/techmart/`
2. Login with your account
3. Go to "🧪 Concurrent Login Test" section
4. Set parameters:
   - Number of Users: 5-50
   - Requests per User: 10-100
5. Click "▶ Start Load Test"
6. View real-time results:
   - Success/Failure counts
   - Total time
   - Requests per second (TPS)

---

## 🔍 Monitoring Concurrent Sessions

### Check Active Sessions
```sql
-- View all active sessions for a user
SELECT * FROM user_session 
WHERE user_id = ? AND active = TRUE
ORDER BY created_at DESC;

-- Check concurrent session count
SELECT concurrent_session_count 
FROM app_user 
WHERE id = ?;

-- Find users with most active sessions
SELECT u.username, COUNT(s.id) as active_sessions
FROM app_user u
LEFT JOIN user_session s ON u.id = s.user_id AND s.active = TRUE
GROUP BY u.id
ORDER BY active_sessions DESC
LIMIT 10;
```

### View Authentication Metrics
```sql
-- Get registration performance
SELECT 
  COUNT(*) as total_registrations,
  AVG(response_time_ms) as avg_time,
  MAX(response_time_ms) as max_time,
  SUM(CASE WHEN success = FALSE THEN 1 ELSE 0 END) as failures
FROM auth_metrics
WHERE event_type = 'REGISTER'
AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Get login performance
SELECT 
  COUNT(*) as total_logins,
  AVG(response_time_ms) as avg_time,
  MAX(response_time_ms) as max_time,
  SUM(CASE WHEN success = FALSE THEN 1 ELSE 0 END) as failures
FROM auth_metrics
WHERE event_type = 'LOGIN'
AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR);
```

---

## 🚀 Usage Examples

### Register New User
```bash
curl -X POST http://localhost:8080/techmart/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePassword123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/techmart/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "SecurePassword123"
  }'
```

### Use Session Token
```bash
curl -X GET http://localhost:8080/techmart/api/metrics \
  -H "X-Session-Token: <token-from-login>"
```

### Validate Session
```bash
curl -X GET http://localhost:8080/techmart/api/auth/validate \
  -H "X-Session-Token: <token>"
```

### Logout
```bash
curl -X POST http://localhost:8080/techmart/api/auth/logout \
  -H "X-Session-Token: <token>"
```

---

## 🔐 Security Features

### Password Hashing
- **Algorithm:** PBKDF2 with SHA-256
- **Iterations:** 100,000
- **Salt:** 32-byte random salt per user
- **Storage:** Base64-encoded (salt + hash)

### Session Management
- **Token:** 32-byte cryptographically secure random tokens
- **Encoding:** URL-safe Base64
- **Validation:** Active session checks with timestamp
- **Tracking:** IP address and User-Agent logging

### Input Validation
- Username: Required, alphanumeric
- Email: Required, must be unique
- Password: Minimum 6 characters, hashed before storage

---

## 📈 Performance Benchmarks

**Expected Performance under Test:**

| Scenario | Users | Throughput | Avg Response Time |
|----------|-------|-----------|-------------------|
| Registration | 20 | 8-12 req/s | 80-120ms |
| Login | 20 | 15-20 req/s | 50-80ms |
| Session Validation | 50 | 40-50 req/s | 20-40ms |
| Concurrent Sessions | 100 | 50-100 req/s | 30-60ms |

---

## 🛠️ Troubleshooting

### Login fails with "Invalid username or password"
- Verify credentials are correct
- Check user exists in database
- Ensure password was saved correctly

### Session token invalid
- Token may have expired
- Check token format in headers
- Verify X-Session-Token header is present

### Concurrent session count not updating
- Sessions may not be committing to database
- Check MySQL connection
- Review application logs

### JMeter shows high error rate
- Increase ramp-up time
- Reduce number of threads
- Check server resource availability
- Monitor application memory usage

---

## 📝 Next Steps

1. **Deploy schema**: Run `db/schema.sql` to create auth tables
2. **Build project**: `mvn clean package`
3. **Deploy WAR**: Copy `target/techmart.war` to WildFly
4. **Test login**: Navigate to `http://localhost:8080/techmart/login.html`
5. **Run JMeter**: Execute load test with provided configuration
6. **Monitor dashboard**: Check concurrent session metrics in real-time

---

## 📚 Files Summary

| File | Purpose |
|------|---------|
| `User.java` | User entity with password hashing |
| `UserSession.java` | Session tracking entity |
| `AuthService.java` | Auth business logic |
| `AuthResource.java` | REST endpoints |
| `AuthResponse.java` | DTO for auth responses |
| `LoginRequest.java` | DTO for login requests |
| `RegisterRequest.java` | DTO for registration requests |
| `login.html` | Login/Registration UI |
| `index.html` | Updated dashboard with concurrent testing |
| `AuthenticationConcurrentIT.java` | Concurrent integration tests |
| `TechMart-Auth-LoadTest.jmx` | JMeter load test configuration |
| `db/schema.sql` | Updated database schema |

---

**Version:** 1.0
**Date:** 2026-07-06
**Status:** Production Ready ✅
