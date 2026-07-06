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
| GET    | /inventory                | Retrieves detailed inventory metrics across all warehouses    |
| POST   | /orders                   | Submits a new order payload for asynchronous processing       |
| GET    | /orders                   | Retrieves recent orders                                        |
| GET    | /metrics                  | Returns performance counters and per-method execution times   |
| GET    | /notifications/stream     | Server-Sent Events (SSE) stream for live notifications        |

> Note: All endpoints are rooted at `/api` (for example: `http://localhost:8080/techmart/api/products`).

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
- db/               - MySQL schema and migration scripts
- deploy/           - WildFly CLI scripts and datasource XML descriptors

---

If you want, I can also:

- add a short development section explaining how to run the app in an IDE,
- include example request/response JSON for key endpoints, or
- add badges and a license section.
