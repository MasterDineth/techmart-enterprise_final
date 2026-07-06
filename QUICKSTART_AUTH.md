# TechMart Authentication Module - Quick Start

## ✅ What's Been Built

### 1. **User Authentication System**
- User registration with email/username validation
- Secure login with PBKDF2 password hashing
- Session token management
- Concurrent session tracking per user

### 2. **Pages**
- **login.html** - Register/Login interface
- **index.html** - Updated dashboard with concurrent test simulator

### 3. **Database Tables**
- `app_user` - User accounts
- `user_session` - Session tracking
- `auth_metrics` - Performance metrics

### 4. **REST API** (`/api/auth`)
- POST `/register` - Create account
- POST `/login` - Authenticate
- GET `/validate` - Check session
- POST `/logout` - End session
- GET `/sessions` - List active sessions

### 5. **Testing**
- `AuthenticationConcurrentIT.java` - Integration tests for 50-100 concurrent users
- `TechMart-Auth-LoadTest.jmx` - JMeter load test configuration
- Dashboard built-in load test simulator

---

## 🚀 Quick Setup

### Step 1: Update Database
```bash
cd db
mysql -u root -p < schema.sql
```

### Step 2: Build Project
```bash
mvn clean package
```

### Step 3: Deploy
Copy `target/techmart.war` to your WildFly deployment folder.

### Step 4: Access
- **Login Page:** http://localhost:8080/techmart/login.html
- **Dashboard:** http://localhost:8080/techmart/ (after login)

---

## 🧪 Test Scenarios

### Browser Load Test
1. Go to http://localhost:8080/techmart/login.html
2. Register test users or login
3. Click "🧪 Concurrent Login Test" on dashboard
4. Set threads (5-50) and requests (10-100)
5. Click "▶ Start Load Test"
6. View real-time throughput and response times

### JMeter Load Test
```bash
# Interactive mode
jmeter -t TechMart-Auth-LoadTest.jmx

# Headless with report
jmeter -n -t TechMart-Auth-LoadTest.jmx -l results.jtl -e -o report/
```

### Integration Tests
```bash
mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly
```

---

## 📊 Expected Results

**20 Concurrent Users, 5 Iterations Each:**
- Registration: 8-12 req/s, 80-120ms avg
- Login: 15-20 req/s, 50-80ms avg
- Validation: 40-50 req/s, 20-40ms avg
- Error Rate: < 1%

---

## 🔍 Monitor Live

```sql
-- Check concurrent sessions
SELECT username, concurrent_session_count 
FROM app_user 
ORDER BY concurrent_session_count DESC;

-- View active sessions
SELECT u.username, COUNT(s.id) as sessions
FROM app_user u
LEFT JOIN user_session s ON u.id = s.user_id AND s.active = TRUE
GROUP BY u.id;

-- Auth performance
SELECT event_type, COUNT(*) as count, AVG(response_time_ms) as avg_ms
FROM auth_metrics
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY event_type;
```

---

## 📚 Files Created

### Backend
- `src/main/java/com/techmart/entity/User.java`
- `src/main/java/com/techmart/entity/UserSession.java`
- `src/main/java/com/techmart/service/AuthService.java`
- `src/main/java/com/techmart/rest/AuthResource.java`
- `src/main/java/com/techmart/dto/LoginRequest.java`
- `src/main/java/com/techmart/dto/RegisterRequest.java`
- `src/main/java/com/techmart/dto/AuthResponse.java`

### Frontend
- `src/main/webapp/login.html` (NEW)
- `src/main/webapp/index.html` (UPDATED)

### Testing
- `src/test/java/com/techmart/it/AuthenticationConcurrentIT.java`
- `TechMart-Auth-LoadTest.jmx`

### Documentation
- `AUTH_SETUP_GUIDE.md` - Full setup and testing guide
- `db/schema.sql` (UPDATED) - New auth tables

---

## 🔐 Security

✅ Passwords hashed with PBKDF2 (100,000 iterations)
✅ Secure session tokens (32-byte cryptographic random)
✅ Email uniqueness validation
✅ Session activity tracking (IP, User-Agent)
✅ Concurrent session limits tracked per user

---

## 💡 Tips for Load Testing

**To simulate realistic concurrent scenarios:**

1. **Ramp-up Gradually** - Don't spike all threads at once
2. **Monitor Resources** - Watch CPU, Memory, and DB connections
3. **Increase Gradually** - Test 5, 10, 20, 50, 100 users
4. **Real Workload** - Register + Login + Browse = realistic test
5. **Track Errors** - Identify bottlenecks from error patterns

**JMeter Graph Interpretation:**
- Red line (errors) should be flat at zero
- Blue line (throughput) should stabilize
- Green line (response time) should stay low
- No sharp spikes indicate stable performance

---

**Status:** ✅ Complete and Ready for Testing
**Next:** Run `mvn clean package` and deploy!
