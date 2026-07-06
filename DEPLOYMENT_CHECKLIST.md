# ✅ TechMart Authentication Module - Developer Checklist

## Pre-Deployment Verification

### Database Setup
- [ ] MySQL server is running
- [ ] Create database: `mysql -u root -p < db/schema.sql`
- [ ] Verify tables created:
  ```sql
  SHOW TABLES LIKE '%user%';
  -- Should show: app_user, user_session, auth_metrics
  ```
- [ ] Check indexes are created
- [ ] Verify foreign key constraints

### Code Compilation
- [ ] Run: `mvn clean compile`
- [ ] No compilation errors
- [ ] Check warnings if any
- [ ] Verify all dependencies resolved:
  ```
  - Jakarta EE 10 APIs
  - JPA/Hibernate
  - MySQL JDBC driver
  ```

### Unit Tests
- [ ] Run: `mvn test`
- [ ] All tests passing
- [ ] No test failures or errors

### Integration Tests (Optional)
- [ ] Start WildFly server
- [ ] Run: `mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly`
- [ ] All concurrent tests passing
- [ ] Check test output for performance metrics

### Package Build
- [ ] Run: `mvn package`
- [ ] No build errors
- [ ] WAR file created: `target/techmart.war`
- [ ] WAR file size reasonable (< 50MB)

---

## WildFly Deployment

### Pre-Deployment
- [ ] WildFly server downloaded and configured
- [ ] Java 17+ installed: `java -version`
- [ ] WILDFLY_HOME environment variable set
- [ ] MySQL driver available in WildFly

### Deploy Application
- [ ] Copy `target/techmart.war` to `$WILDFLY_HOME/standalone/deployments/`
- [ ] Start WildFly: `./bin/standalone.sh`
- [ ] Check startup logs for errors
- [ ] Verify deployment: `http://localhost:8080/techmart/`
- [ ] Should see 404 (no index.html at root) or redirect

### Verify Services
- [ ] Login page accessible: `http://localhost:8080/techmart/login.html`
- [ ] Page loads without errors
- [ ] Registration form visible
- [ ] CSS and JavaScript loaded correctly

---

## Feature Testing

### Registration Feature
- [ ] Navigate to login page
- [ ] Switch to "Register" tab
- [ ] Enter test data:
  - Username: `testuser1`
  - Email: `test1@example.com`
  - Password: `TestPassword123`
  - Confirm: `TestPassword123`
- [ ] Click "Create Account"
- [ ] Success message shown
- [ ] Redirected to login tab
- [ ] Try registering same username → Error message
- [ ] Try registering same email → Error message
- [ ] Database check:
  ```sql
  SELECT * FROM app_user WHERE username = 'testuser1';
  ```
  Should show record with hashed password (not plain text)

### Login Feature
- [ ] Enter username: `testuser1`
- [ ] Enter password: `TestPassword123`
- [ ] Click "Sign In"
- [ ] Redirected to dashboard (index.html)
- [ ] User info shows: `👤 testuser1 (1 sessions)`
- [ ] Dashboard loads with products and orders
- [ ] Database check:
  ```sql
  SELECT * FROM user_session WHERE user_id = 1 AND active = TRUE;
  ```
  Should show active session record

### Logout Feature
- [ ] Click "Logout" button
- [ ] Redirected to login page
- [ ] Session becomes inactive:
  ```sql
  SELECT * FROM user_session WHERE user_id = 1;
  ```
  Should show `active = FALSE` for that session

### Concurrent Sessions
- [ ] Login with same account in different browser/tab
- [ ] User info shows: `👤 testuser1 (2 sessions)`
- [ ] Login again in third browser
- [ ] User info shows: `👤 testuser1 (3 sessions)`
- [ ] Database check:
  ```sql
  SELECT COUNT(*) FROM user_session 
  WHERE user_id = 1 AND active = TRUE;
  ```
  Should show 3

### Dashboard Features
- [ ] Products load and display
- [ ] Add items to cart
- [ ] Place order with customer info
- [ ] Order appears in Recent Orders
- [ ] Metrics update
- [ ] Notifications display
- [ ] SSE connection status shows

### Load Test Feature
- [ ] Find "🧪 Concurrent Login Test" section
- [ ] Set Number of Users: 10
- [ ] Set Requests per User: 5
- [ ] Click "▶ Start Load Test"
- [ ] Results show after completion:
  - Success count > 40
  - Total time displayed
  - Throughput displayed (req/s)

---

## API Testing

### Test with cURL

#### Register
```bash
curl -X POST http://localhost:8080/techmart/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "apiuser",
    "email": "api@test.com",
    "password": "ApiPassword123"
  }'
```
- [ ] Status: 201
- [ ] Response contains: success, userId, username

#### Login
```bash
curl -X POST http://localhost:8080/techmart/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "apiuser",
    "password": "ApiPassword123"
  }'
```
- [ ] Status: 200
- [ ] Response contains: sessionToken, userId, concurrentSessions
- [ ] Save sessionToken for next requests

#### Validate Session
```bash
curl -X GET http://localhost:8080/techmart/api/auth/validate \
  -H "X-Session-Token: <YOUR_TOKEN>"
```
- [ ] Status: 200
- [ ] Response contains: userId, username, concurrentSessions

#### Get Metrics (requires auth)
```bash
curl -X GET http://localhost:8080/techmart/api/metrics \
  -H "X-Session-Token: <YOUR_TOKEN>"
```
- [ ] Status: 200
- [ ] Returns metrics object

#### Logout
```bash
curl -X POST http://localhost:8080/techmart/api/auth/logout \
  -H "X-Session-Token: <YOUR_TOKEN>"
```
- [ ] Status: 200
- [ ] Response: success = true

#### List Sessions
```bash
curl -X GET http://localhost:8080/techmart/api/auth/sessions \
  -H "X-Session-Token: <YOUR_TOKEN>"
```
- [ ] Status: 200
- [ ] Returns array of user_session objects

---

## Database Validation

### Check All Tables Exist
```sql
[ ] SHOW TABLES;
    Should include:
    - app_user
    - user_session
    - auth_metrics
    - (plus existing tables: product, orders, etc.)
```

### Verify Schema
```sql
[ ] DESCRIBE app_user;
    [ ] Has columns: id, username, email, password_hash, 
        active, created_at, last_login, concurrent_session_count
    [ ] username UNIQUE
    [ ] email UNIQUE

[ ] DESCRIBE user_session;
    [ ] Has columns: id, user_id, session_token, active,
        created_at, last_accessed, user_agent, ip_address
    [ ] session_token UNIQUE
    [ ] Has foreign key to app_user

[ ] DESCRIBE auth_metrics;
    [ ] Has columns: id, event_type, response_time_ms,
        success, error_message, thread_id, created_at
```

### Check Indexes
```sql
[ ] SHOW INDEX FROM app_user;
    [ ] Should have indexes on: username, email, active, created_at

[ ] SHOW INDEX FROM user_session;
    [ ] Should have indexes on: session_token, user_id, active, created_at
```

---

## Performance Verification

### Response Time Check
```bash
# Time a registration request
time curl -X POST http://localhost:8080/techmart/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"perf1","email":"perf1@test.com","password":"Test123"}'
```
- [ ] Response time < 200ms (acceptable)
- [ ] Preferably < 100ms (good)

### Concurrent User Test
- [ ] Open dashboard
- [ ] Run load test with 20 users
- [ ] Throughput: > 10 req/s
- [ ] Error rate: < 1%
- [ ] Response times: < 200ms average

### Database Performance
```sql
[ ] SELECT COUNT(*) FROM user_session;
    Should be manageable (thousands are fine)

[ ] SELECT COUNT(*) FROM app_user;
    Should show all registered users

[ ] SELECT COUNT(*) FROM auth_metrics;
    Should track all auth events
```

---

## JMeter Load Testing

### Setup JMeter
- [ ] Download JMeter from apache.org
- [ ] Extract to local directory
- [ ] Verify: `jmeter -version`

### Run Headless Test
```bash
[ ] jmeter -n -t TechMart-Auth-LoadTest.jmx \
    -l results.jtl \
    -j jmeter.log \
    -e -o report/
```
- [ ] Test completes without errors
- [ ] results.jtl file created
- [ ] report/ directory created with HTML report

### Check JMeter Results
- [ ] HTML report opens in browser
- [ ] Shows graphs and statistics
- [ ] Analyze metrics:
  - [ ] Throughput (requests/second)
  - [ ] Response time distribution
  - [ ] Error rate (should be < 1%)
  - [ ] 90th and 95th percentiles

### Create Custom Test Plan
- [ ] Open JMeter GUI
- [ ] Load TechMart-Auth-LoadTest.jmx
- [ ] Modify thread counts:
  - [ ] 5 threads for smoke test
  - [ ] 20 threads for normal load
  - [ ] 50 threads for stress test
- [ ] Run test
- [ ] Observe live graph
- [ ] Check response times under load

---

## Security Verification

### Password Hashing
```sql
[ ] SELECT password_hash FROM app_user WHERE username = 'testuser1';
    Should show:
    - [ ] Long string (Base64 encoded)
    - [ ] NOT plain password "TestPassword123"
    - [ ] Different for each user (different salts)
```

### Session Tokens
```sql
[ ] SELECT session_token FROM user_session LIMIT 5;
    Should show:
    - [ ] Unique tokens per session
    - [ ] Different from other sessions
    - [ ] ~44 characters (32 bytes Base64 encoded)
```

### SQL Injection Test
- [ ] Try login with: `'; DROP TABLE app_user; --`
  - [ ] Should fail gracefully
  - [ ] Database intact
  - [ ] Error message shown

### XSS Test
- [ ] Try username with: `<script>alert('xss')</script>`
  - [ ] Should be stored/displayed as text
  - [ ] No script execution
  - [ ] Escaped properly

---

## Documentation Review

- [ ] AUTH_SETUP_GUIDE.md - Complete and accurate
- [ ] QUICKSTART_AUTH.md - Easy to follow
- [ ] IMPLEMENTATION_SUMMARY.md - Describes all changes
- [ ] ARCHITECTURE.md - Diagrams clear
- [ ] This checklist - Used successfully

---

## Final Sign-Off

### Pre-Production Checklist
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] No compilation warnings
- [ ] Database schema verified
- [ ] All APIs tested and working
- [ ] Load tests show acceptable performance
- [ ] Security checks passed
- [ ] Documentation complete
- [ ] JAR/WAR file ready for deployment

### Deployment Checklist
- [ ] Source code committed to version control
- [ ] Build artifacts created
- [ ] Deployment documented
- [ ] Rollback plan documented
- [ ] Monitoring setup ready
- [ ] Team trained on new features

### Post-Deployment Checklist
- [ ] Application running without errors
- [ ] No errors in application logs
- [ ] Database connections working
- [ ] All users can login
- [ ] Load test results recorded
- [ ] Performance metrics established
- [ ] Team notified of deployment

---

## Troubleshooting Quick Links

| Issue | Solution |
|-------|----------|
| Login page not loading | Check WAR deployment, verify URL |
| Database connection error | Verify MySQL running, schema created |
| Password verification fails | Check password hashing logic in AuthService |
| Session token invalid | Verify token format, check database active status |
| High error rate in load test | Reduce thread count, increase ramp-up time |
| Slow response times | Check database indexes, verify MySQL performance |
| Session count not updating | Check concurrent_session_count field in app_user |
| JMeter not connecting | Verify server URL, check firewall settings |

---

## Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | __________ | __________ | __________ |
| QA Lead | __________ | __________ | __________ |
| DevOps | __________ | __________ | __________ |
| PM | __________ | __________ | __________ |

---

**Status:** Ready for Production ✅
**Version:** 1.0
**Last Updated:** 2026-07-06

All checklist items completed = Ready for Production Release
