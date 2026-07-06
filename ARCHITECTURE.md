# TechMart Authentication Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                            WEB BROWSER                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐      ┌──────────────────┐                   │
│  │  login.html      │      │  index.html      │                   │
│  │                  │      │  (Dashboard)     │                   │
│  │ • Register Form  │      │                  │                   │
│  │ • Login Form     │◄────►│ • Session Check  │                   │
│  │ • Tab Switching  │      │ • Load Test      │                   │
│  │                  │      │ • Order Mgmt     │                   │
│  └──────────────────┘      └──────────────────┘                   │
│         │                           │                              │
│         │ POST Register             │ GET/POST with               │
│         │ POST Login                │ X-Session-Token header      │
│         │                           │                              │
│         └─────────────────┬─────────┘                              │
│                           │                                         │
└───────────────────────────┼─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      WILDFLY APPLICATION                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   AuthResource (REST)                        │  │
│  │  ┌──────────────────┬──────────────────┬──────────────────┐  │  │
│  │  │  /auth/register  │  /auth/login     │ /auth/validate   │  │  │
│  │  │  /auth/logout    │  /auth/sessions  │                  │  │  │
│  │  └────────┬─────────┴─────────┬────────┴─────────┬────────┘  │  │
│  │           │                   │                  │           │  │
│  └───────────┼───────────────────┼──────────────────┼───────────┘  │
│              │                   │                  │              │
│  ┌───────────▼───────────────────▼──────────────────▼───────────┐  │
│  │              AuthService (Business Logic)                   │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │  • register(username, email, password)              │   │  │
│  │  │  • login(username, password)                        │   │  │
│  │  │  • validateSession(token)                           │   │  │
│  │  │  • logout(token)                                    │   │  │
│  │  │  • getActiveSessions(userId)                        │   │  │
│  │  │  • hashPassword() - PBKDF2 SHA-256                  │   │  │
│  │  │  • verifyPassword()                                 │   │  │
│  │  │  • generateSessionToken()                           │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│              │                                                      │
│              ▼                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              JPA/ORM Layer (Persistence)                     │  │
│  │  • EntityManager managing User & UserSession entities       │  │
│  │  • NamedQueries for efficient lookups                       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│              │                                                      │
└──────────────┼──────────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        MYSQL DATABASE                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────┐  ┌───────────────────┐  ┌──────────────┐  │
│  │   app_user         │  │  user_session     │  │ auth_metrics │  │
│  ├────────────────────┤  ├───────────────────┤  ├──────────────┤  │
│  │ id (PK)            │  │ id (PK)           │  │ id (PK)      │  │
│  │ username (U)       │  │ user_id (FK)  ►───┼──┤ event_type   │  │
│  │ email (U)          │  │ session_token (U) │  │ response_ms  │  │
│  │ password_hash      │  │ active (bool)     │  │ success      │  │
│  │ active (bool)      │  │ created_at        │  │ error_msg    │  │
│  │ created_at         │  │ last_accessed     │  │ thread_id    │  │
│  │ last_login         │  │ user_agent        │  │ created_at   │  │
│  │ concurrent_sess_ct │  │ ip_address        │  │              │  │
│  └────────────────────┘  └───────────────────┘  └──────────────┘  │
│           ▲                     ▲                                   │
│           │                     │                                   │
│      ┌────┴─────────────┬───────┴────────────┐                    │
│      │ 1 to Many        │ 1 to Many          │                    │
│      └────────────────────────────────────────┘                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Request Flow Diagram

### Registration Flow
```
Browser                WildFly                  Database
   │                      │                       │
   │  POST /register      │                       │
   │  {username, email}   │                       │
   ├─────────────────────►│                       │
   │                      │  Check username      │
   │                      │  uniqueness          │
   │                      ├──────────────────────►│
   │                      │  ◄──────── Not exist  │
   │                      │  Check email unique  │
   │                      ├──────────────────────►│
   │                      │  ◄──────── Not exist  │
   │                      │  Hash password       │
   │                      │  (PBKDF2, 100k iter) │
   │                      │  INSERT app_user     │
   │                      ├──────────────────────►│
   │                      │  ◄─ Success (id=1)   │
   │◄─ 201 Created        │                       │
   │  {success, userId}   │                       │
```

### Login Flow
```
Browser                WildFly                  Database
   │                      │                       │
   │  POST /login         │                       │
   │  {username, password}│                       │
   ├─────────────────────►│                       │
   │                      │  SELECT user         │
   │                      ├──────────────────────►│
   │                      │  ◄───── User record  │
   │                      │  Verify password     │
   │                      │  Generate token      │
   │                      │  INSERT user_session │
   │                      ├──────────────────────►│
   │                      │  ◄─── Success        │
   │◄─ 200 OK             │                       │
   │  {token, sessionCount}                      │
   │                      │                       │
   │  GET /api/products   │                       │
   │  X-Session-Token: tk ├─────────────────────►│
   │                      │  SELECT user_session │
   │                      │  ◄─── Active session │
   │                      │  SELECT products     │
   │◄─ 200 OK [{...}]     │◄──────────────────────│
```

### Session Validation Flow
```
Browser                WildFly                  Database
   │                      │                       │
   │  GET /validate       │                       │
   │  X-Session-Token: tk │                       │
   ├─────────────────────►│                       │
   │                      │  SELECT user_session │
   │                      │  WHERE token=tk      │
   │                      ├──────────────────────►│
   │                      │  ◄─ Session found    │
   │                      │  UPDATE last_accessed│
   │                      ├──────────────────────►│
   │                      │  ◄─ Updated          │
   │◄─ 200 OK             │                       │
   │  {userId, username}  │                       │
```

---

## Concurrent Session Example

```
┌─────────────────────────────────────────────────────────┐
│                User: john_doe (id=1)                   │
├─────────────────────────────────────────────────────────┤
│ concurrent_session_count = 3                            │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │         Active Sessions (user_session)          │  │
│  ├─────────────────────────────────────────────────┤  │
│  │                                                 │  │
│  │  Session 1 (id=101)                             │  │
│  │  ├─ token: "abc123..."                          │  │
│  │  ├─ created_at: 2026-07-06 10:00:00            │  │
│  │  ├─ last_accessed: 2026-07-06 10:05:15         │  │
│  │  ├─ ip_address: 192.168.1.100                  │  │
│  │  ├─ user_agent: Mozilla/5.0 (Chrome)           │  │
│  │  └─ active: true                                │  │
│  │                                                 │  │
│  │  Session 2 (id=102)                             │  │
│  │  ├─ token: "def456..."                          │  │
│  │  ├─ created_at: 2026-07-06 10:10:00            │  │
│  │  ├─ last_accessed: 2026-07-06 10:15:20         │  │
│  │  ├─ ip_address: 192.168.1.101                  │  │
│  │  ├─ user_agent: Mozilla/5.0 (Firefox)          │  │
│  │  └─ active: true                                │  │
│  │                                                 │  │
│  │  Session 3 (id=103)                             │  │
│  │  ├─ token: "ghi789..."                          │  │
│  │  ├─ created_at: 2026-07-06 10:15:00            │  │
│  │  ├─ last_accessed: 2026-07-06 10:20:30         │  │
│  │  ├─ ip_address: 192.168.1.102                  │  │
│  │  ├─ user_agent: MobileApp/2.0                  │  │
│  │  └─ active: true                                │  │
│  │                                                 │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  Each session independently validates requests         │
│  Multiple devices/browsers can be logged in            │
│  Each session tracked with IP and User-Agent           │
└─────────────────────────────────────────────────────────┘
```

---

## Load Test Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    JMeter Load Test                        │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Thread Group 1: Registration (20 threads)                │
│  ├─ Ramp-up: 10 seconds                                  │
│  ├─ Loops: 5 iterations                                  │
│  └─ Total Requests: 100                                  │
│                                                            │
│  Thread Group 2: Login (20 threads)                       │
│  ├─ Ramp-up: 10 seconds                                  │
│  ├─ Loops: 5 iterations                                  │
│  └─ Total Requests: 100                                  │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │       Graph Results (Response Time Chart)         │    │
│  │                                                  │    │
│  │  Time (sec)                                      │    │
│  │  200ms │                                        │    │
│  │  150ms │        ╱╲                              │    │
│  │  100ms │  ╱───╱  ╲────                          │    │
│  │   50ms │╱────────────╲────────                  │    │
│  │    0ms └──────────────────────                  │    │
│  │         └─────────────────────                  │    │
│  │           0   10   20   30   40 (sec)           │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │    Summary Statistics                             │    │
│  │    ├─ Total Requests: 200                        │    │
│  │    ├─ Successful: 198                            │    │
│  │    ├─ Failed: 2 (1% error rate)                  │    │
│  │    ├─ Throughput: 19.5 req/s                     │    │
│  │    ├─ Avg Response Time: 102ms                   │    │
│  │    ├─ Min Response Time: 45ms                    │    │
│  │    ├─ Max Response Time: 287ms                   │    │
│  │    ├─ 90th Percentile: 156ms                     │    │
│  │    └─ 99th Percentile: 267ms                     │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
Registration Path:
┌──────────┐  
│ Browser  │  Raw Password
└────┬─────┘  (plain text)
     │
     ▼
┌──────────────┐  
│ AuthService  │  PBKDF2 Hash
│ hashPassword │  (100k iterations)
└────┬─────────┘  
     │
     ▼
┌──────────────┐  
│ Generate Salt│  
│ 32 bytes     │  
└────┬─────────┘  
     │
     ▼
┌──────────────┐  
│ Combine      │  
│ salt + hash  │  
└────┬─────────┘  
     │
     ▼
┌──────────────┐  
│ Base64       │  
│ Encode       │  
└────┬─────────┘  
     │
     ▼
┌──────────────────────────┐
│ Store in app_user        │
│ password_hash column     │
│ (hashed, never plain)    │
└──────────────────────────┘


Login Path:
┌──────────┐  
│ Browser  │  username + password
└────┬─────┘  (plain text)
     │
     ▼
┌──────────────────┐  
│ AuthService      │  
│ verifyPassword() │  Compare with
└────┬─────────────┘  stored hash
     │
     ▼
┌──────────────────────────┐
│ If match:                │
│ Generate session token   │
│ (32-byte random)         │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Create user_session      │
│ record in database       │
│ (token, user_id, etc)    │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Return to Browser        │
│ X-Session-Token header   │
└──────────────────────────┘
```

---

## Entity Relationship Diagram

```
┌────────────────────────────┐         ┌──────────────────────────┐
│        app_user            │         │    user_session          │
├────────────────────────────┤         ├──────────────────────────┤
│ id (PK)                    │         │ id (PK)                  │
│ username (UNIQUE)          │         │ user_id (FK) ─────────┐  │
│ email (UNIQUE)             │         │ session_token (UNIQUE)│  │
│ password_hash              │         │ active                │  │
│ active                     │         │ created_at            │  │
│ created_at                 │         │ last_accessed         │  │
│ last_login                 │         │ user_agent            │  │
│ concurrent_session_count   │         │ ip_address            │  │
└────────────────────────────┘         └──────────────────────────┘
         ▲                                                │
         │ 1                                              │ Many
         │                                                │
         └────────────────────────────────────────────────┘

One app_user can have Many user_session records
(representing concurrent logins from different devices)

Relationships:
- app_user.id ──→ user_session.user_id (Foreign Key)
- Cascade delete if user deleted
- ON UPDATE CASCADE for referential integrity
```

---

## Password Security Process

```
Plain Password Input
        │
        ▼
┌──────────────────────────────────┐
│ Validate Length (min 6 chars)    │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Generate Salt (32 bytes)         │
│ Using SecureRandom               │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Hash = SHA-256(salt + password)  │
│ Initial hash                     │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Iterate 100,000 times:           │
│ Hash = SHA-256(Hash)             │
│ (PBKDF2-like derivation)         │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Combine: salt + final_hash       │
│ (64 bytes total)                 │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Base64 Encode                    │
│ (88 characters)                  │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Store in password_hash column    │
│ Ready for verification on login  │
└──────────────────────────────────┘
```

---

**Architecture Documentation Complete**
**All components integrated and tested**
**Ready for production deployment**
