# TechMart Enterprise Platform

A highly scalable, event-driven e‑commerce platform modernization prototype built with the Jakarta EE stack. This project demonstrates enterprise architectural patterns, including asynchronous messaging, optimistic concurrency control, and lock‑free performance monitors.

---

## Table of Contents

- [Highlights](#highlights)
- [Prerequisites](#prerequisites)
- [Quick Start & Deployment](#quick-start--deployment)
  - [1. Database Initialization](#1-database-initialization)
  - [2. WildFly Configuration](#2-wildfly-configuration)
  - [3. Build and Deploy](#3-build-and-deploy)
  - [4. Access the Application](#4-access-the-application)
- [Security & Session Management](#security--session-management)
- [REST API Reference](#rest-api-reference)
  - [Authentication Endpoints](#authentication-endpoints)
  - [Commerce Endpoints](#commerce-endpoints)
- [Example Request / Response JSON](#example-request--response-json)
  - [Authentication](#authentication)
  - [Orders](#orders)
  - [Products](#products)
- [Testing & Performance Validation](#testing--performance-validation)
- [Project Structure](#project-structure)

---

## Highlights

- Application Server: WildFly 31+ (Full Profile)
- Core Framework: Jakarta EE 10 (EJB, CDI, JPA, JAX-RS)
- Database: MySQL 8.x with HikariCP-style connection pooling
- Messaging: Embedded Artemis (JMS) for asynchronous order fulfillment
- Security: PBKDF2 (SHA-256) password hashing with 100,000 iterations
- Concurrency Control: JPA optimistic locking (@Version) to prevent inventory overselling
- Performance: Lock-free AtomicLong metrics and in-memory container-managed read caches

## Prerequisites

Ensure the following software is installed on your deployment environment:

- JDK 17 or higher
- WildFly 31 or higher (Full Profile)
- MySQL 8.0 or higher
- Maven 3.8 or higher
- Apache JMeter (for load testing)

## Quick Start & Deployment

Follow these steps to get the application running locally.

### 1. Database Initialization

The application requires a MySQL database to store product catalog data, orders, and user authentication metrics. Execute the provided schema script to create the database and required tables:

```bash
mysql -u root -p < db/schema.sql
```

### 2. WildFly Configuration

Configure WildFly with the MySQL JDBC driver and the application datasource. Edit `deploy/install.cli` to point to your local `mysql-connector-j-8.x.x.jar` path, then start WildFly and apply the CLI script:

```bash
# Start WildFly with the full profile
$WILDFLY_HOME/bin/standalone.sh -c standalone-full.xml

# Execute the CLI script in a separate terminal
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=deploy/install.cli
```

### 3. Build and Deploy

Compile the application and deploy the generated WAR to your WildFly server:

```bash
mvn clean package
cp target/techmart.war $WILDFLY_HOME/standalone/deployments/
```

### 4. Access the Application

Once deployment is successful, open the application in your browser:

- Login interface: http://localhost:8080/techmart/login.html
- Secure dashboard: http://localhost:8080/techmart/

## Security & Session Management

The authentication module implements enterprise-grade security features:

- Cryptographic hashing: Passwords are hashed using PBKDF2 (SHA-256) with 100,000 iterations and a 32‑byte cryptographically secure random salt. Plain-text passwords are never stored.
- Stateless tokens: Session validation uses 32‑byte secure random tokens with URL‑safe Base64 encoding.
- Concurrent session tracking: Supports and tracks multiple simultaneous logins across different devices per user.
- Audit logging: Captures IP addresses and User‑Agent details for active session monitoring.

## REST API Reference

All backend services are exposed under the base path `http://localhost:8080/techmart/api/`. Protected endpoints require the `X-Session-Token` header.

### Authentication Endpoints

| Method | Endpoint         | Description                                         |
|--------|------------------|-----------------------------------------------------|
| POST   | /auth/register   | Creates a new user account (JSON body required)     |
| POST   | /auth/login      | Authenticates a user and returns a session token    |
| GET    | /auth/validate   | Validates the current session token                 |
| POST   | /auth/logout     | Terminates the active session                       |
| GET    | /auth/sessions   | Retrieves all active concurrent sessions for a user |

### Commerce Endpoints

| Method | Endpoint                  | Description                                                   |
|--------|---------------------------|---------------------------------------------------------------|
| GET    | /products                 | Retrieves the catalog with real-time inventory availability    |
| GET    | /products/{id}            | Retrieves a single product detail                             |
| GET    | /inventory                | Retrieves detailed inventory metrics across all warehouses    |
| POST   | /orders                   | Submits a new order payload for asynchronous processing       |
| GET    | /orders                   | Retrieves recent orders                                        |
| GET    | /orders/{id}              | Retrieves a single order                                      |
| GET    | /metrics                  | Returns performance counters and per-method execution times   |
| GET    | /notifications/stream     | Server-Sent Events (SSE) stream for live notifications        |

> Note: All endpoints are rooted at `/api` (for example: `http://localhost:8080/techmart/api/products`).

## Example Request / Response JSON

This section provides concrete example request and response payloads for commonly used endpoints, assembled from the project's documentation and the authentication guide.

### Authentication

Register (POST /api/auth/register)

Request:

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securePass123"
}
```

Response (201 Created):

```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": 1,
  "username": "john_doe"
}
```

Login (POST /api/auth/login)

Request:

```json
{
  "username": "john_doe",
  "password": "securePass123"
}
```

Response (200 OK):

```json
{
  "success": true,
  "message": "Login successful",
  "sessionToken": "VGVzdEJhY2tUb2tlbg==",
  "userId": 1,
  "username": "john_doe",
  "concurrentSessions": 2
}
```

Validate (GET /api/auth/validate)

Request headers: `X-Session-Token: <token>`

Response (200 OK):

```json
{
  "success": true,
  "message": "Session valid",
  "userId": 1,
  "username": "john_doe",
  "concurrentSessions": 2
}
```

Logout (POST /api/auth/logout)

Request headers: `X-Session-Token: <token>`

Response (200 OK):

```json
{
  "success": true,
  "message": "Logout successful"
}
```

Get Sessions (GET /api/auth/sessions)

Request headers: `X-Session-Token: <token>`

Response (200 OK):

```json
[
  {
    "id": 1,
    "userId": 1,
    "sessionToken": "VGVzdDE=",
    "active": true,
    "createdAt": "2026-07-06T11:00:00",
    "lastAccessed": "2026-07-06T11:45:00",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "ipAddress": "192.168.1.100"
  },
  {
    "id": 2,
    "userId": 1,
    "sessionToken": "VGVzdDI=",
    "active": true,
    "createdAt": "2026-07-06T11:30:00",
    "lastAccessed": "2026-07-06T11:40:00",
    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
    "ipAddress": "192.168.1.101"
  }
]
```

### Orders

Place Order (POST /api/orders)

Request:

```json
{
  "customerName": "Alice",
  "customerEmail": "alice@example.com",
  "lines": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

Response (202 Accepted - queued for async fulfillment):

```json
{
  "orderId": 101,
  "status": "PENDING",
  "message": "Order received and queued for processing",
  "estimatedFulfilment": "2026-07-06T12:10:00Z"
}
```

Get Recent Orders (GET /api/orders)

Response (200 OK):

```json
[
  {
    "orderId": 101,
    "userId": 1,
    "status": "PENDING",
    "total": 149.98,
    "createdAt": "2026-07-06T11:05:00Z",
    "lines": [
      { "productId": 1, "quantity": 2, "unitPrice": 49.99 },
      { "productId": 3, "quantity": 1, "unitPrice": 49.99 }
    ]
  },
  {
    "orderId": 100,
    "userId": 2,
    "status": "CONFIRMED",
    "total": 29.99,
    "createdAt": "2026-07-06T10:50:00Z",
    "lines": [
      { "productId": 2, "quantity": 1, "unitPrice": 29.99 }
    ]
  }
]
```

Get Single Order (GET /api/orders/{id})

Response (200 OK):

```json
{
  "orderId": 101,
  "userId": 1,
  "status": "PENDING",
  "total": 149.98,
  "createdAt": "2026-07-06T11:05:00Z",
  "lines": [
    { "productId": 1, "quantity": 2, "unitPrice": 49.99 },
    { "productId": 3, "quantity": 1, "unitPrice": 49.99 }
  ],
  "fulfilmentNotes": null
}
```

### Products

List Products (GET /api/products)

Response (200 OK):

```json
[
  {
    "productId": 1,
    "sku": "TM-001",
    "name": "Wireless Headphones",
    "price": 49.99,
    "currency": "USD",
    "availableQuantity": 120,
    "warehouses": [
      { "warehouseId": 1, "quantity": 60 },
      { "warehouseId": 2, "quantity": 60 }
    ]
  },
  {
    "productId": 2,
    "sku": "TM-002",
    "name": "USB-C Charger",
    "price": 29.99,
    "currency": "USD",
    "availableQuantity": 300,
    "warehouses": [
      { "warehouseId": 2, "quantity": 200 },
      { "warehouseId": 3, "quantity": 100 }
    ]
  }
]
```

Get Single Product (GET /api/products/{id})

Response (200 OK):

```json
{
  "productId": 1,
  "sku": "TM-001",
  "name": "Wireless Headphones",
  "description": "Over-ear, noise-isolating headphones with 20h battery life.",
  "price": 49.99,
  "currency": "USD",
  "availableQuantity": 120,
  "warehouses": [
    { "warehouseId": 1, "quantity": 60 },
    { "warehouseId": 2, "quantity": 60 }
  ]
}
```

> Notes: Example payloads merge information from the authentication setup guide (DB tables and auth endpoints) and the old README architecture notes. Use these as API documentation examples; actual fields returned may vary slightly depending on the deployed version.

## Testing & Performance Validation

The project includes unit, integration, and load testing tooling.

### Unit tests

Run JUnit unit tests with:

```bash
mvn test
```

### Integration tests (Arquillian)

Deploy and run integration tests against an Arquillian-managed WildFly container:

```bash
mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly
```

### JMeter load testing

A pre-configured JMeter test plan (`TechMart-Auth-LoadTest.jmx`) is provided. Run the load test in headless mode to generate an HTML report:

```bash
jmeter -n -t TechMart-Auth-LoadTest.jmx -l results.jtl -e -o report/
```

## Project Structure

- src/main/java/com/techmart/
  - entity/   - JPA entities (User, Session, Product, Order)
  - service/  - Core business logic and EJB components (Stateless, Stateful, Singleton)
  - rest/     - JAX-RS API controllers
  - mdb/      - Message-driven beans for asynchronous JMS consumption
  - metrics/  - Lock-free performance monitoring and interceptors
- src/main/webapp/  - Frontend HTML, Tailwind CSS, and vanilla JavaScript
- src/test/java/    - JUnit 5 and Arquillian integration tests
- db/               - MySQL schema and migration scripts (includes `app_user`, `user_session`, `auth_metrics` additions)
- deploy/           - WildFly CLI scripts and datasource XML descriptors

---

If you want, I can also:

- add a short development section explaining how to run the app in an IDE,
- expand these examples with actual curl commands and full request headers,
- generate OpenAPI/Swagger definitions from the JAX-RS resources (if you provide the resource sources), or
- add badges and a license section.
