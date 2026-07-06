TechMart Enterprise Platform

A highly scalable, event-driven e-commerce platform modernization prototype built with the Jakarta EE stack. This project demonstrates enterprise architectural patterns, including asynchronous message processing, stateful session management, concurrent load handling, and secure user authentication.

System Architecture Highlights

Application Server: WildFly 31+ (Full Profile)

Core Framework: Jakarta EE 10 (EJB, CDI, JPA, JAX-RS)

Database: MySQL 8.x with HikariCP-style connection pooling

Messaging: Embedded Artemis (JMS) for asynchronous order fulfillment

Security: PBKDF2 (SHA-256) password hashing with 100,000 iterations

Concurrency Control: JPA Optimistic Locking (@Version) to prevent inventory overselling

Performance: Lock-free AtomicLong metrics monitoring and in-memory container-managed read caches

Prerequisites

Ensure the following software is installed on your deployment environment:

JDK 17 or higher

WildFly 31 or higher (Full Profile)

MySQL 8.0 or higher

Maven 3.8 or higher

Apache JMeter (for load testing)

Quick Start & Deployment

1. Database Initialization

The application requires a MySQL database to store product catalog data, orders, and user authentication metrics. Execute the provided schema script to create the database and required tables:

mysql -u root -p < db/schema.sql


2. WildFly Configuration

Configure WildFly with the MySQL JDBC driver and the application datasource. Edit the deploy/install.cli script to point to your local mysql-connector-j-8.4.0.jar path, then execute:

# Start WildFly with full profile
$WILDFLY_HOME/bin/standalone.sh -c standalone-full.xml

# Execute the CLI script in a separate terminal
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=deploy/install.cli


3. Build and Deploy

Compile the application and deploy the generated Web Archive (WAR) file to your WildFly server:

mvn clean package
cp target/techmart.war $WILDFLY_HOME/standalone/deployments/


4. Access the Application

Once deployment is successful, access the application via your web browser:

Login Interface: http://localhost:8080/techmart/login.html

Secure Dashboard: http://localhost:8080/techmart/

Security & Session Management

The user authentication module implements enterprise-grade security features:

Cryptographic Hashing: Passwords are never stored in plain-text. They are hashed using PBKDF2 (SHA-256) with 100,000 iterations and a 32-byte cryptographically secure random salt.

Stateless Tokens: Session validation utilizes 32-byte secure random tokens with URL-safe Base64 encoding.

Concurrent Session Tracking: Supports and tracks multiple simultaneous logins across different devices per user.

Audit Logging: Captures IP addresses and User-Agent details for active session monitoring.

REST API Reference

All backend services are exposed via JAX-RS endpoints located under the http://localhost:8080/techmart/api/ base path. Protected endpoints require the X-Session-Token header.

Authentication Endpoints

Method

Endpoint

Description

POST

/auth/register

Creates a new user account (Requires JSON body)

POST

/auth/login

Authenticates a user and generates a session token

GET

/auth/validate

Validates the current session token

POST

/auth/logout

Terminates the active session

GET

/auth/sessions

Retrieves all active concurrent sessions

Commerce Endpoints

Method

Endpoint

Description

GET

/products

Retrieves the catalog with real-time inventory availability

GET

/inventory

Retrieves detailed inventory metrics across all warehouses

POST

/orders

Submits a new order payload for asynchronous processing

GET

/orders

Retrieves recent orders

GET

/metrics

Returns performance counters and per-method execution times

GET

/notifications/stream

Server-Sent Events (SSE) stream for live notifications

Testing & Performance Validation

The platform includes comprehensive testing suites targeting functional correctness and high-load concurrency.

Unit Tests

Executes isolated tests for business logic, including password hashing algorithms and lock-free performance monitors.

mvn test


Integration Tests

Deploys the application to an Arquillian-managed WildFly container to test database persistence, JMS message queuing, and concurrent authentication load.

mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly


JMeter Load Testing

A pre-configured JMeter test plan (TechMart-Auth-LoadTest.jmx) is included to validate non-functional requirements (NFRs). Run the load test in headless mode to generate an HTML report:

jmeter -n -t TechMart-Auth-LoadTest.jmx -l results.jtl -e -o report/


Dashboard Load Simulator

A real-time concurrent load test simulator is integrated directly into the secure dashboard. It allows administrators to simulate up to 50 concurrent users making continuous requests, providing live throughput (req/s) and response time metrics without requiring external tooling.

Project Structure

src/main/java/com/techmart/

entity/ - JPA Entities (User, Session, Product, Order)

service/ - Core business logic and EJB components (Stateless, Stateful, Singleton)

rest/ - JAX-RS API Controllers

mdb/ - Message-Driven Beans for asynchronous JMS consumption

metrics/ - Lock-free performance monitoring and interceptors

src/main/webapp/ - Frontend HTML, Tailwind CSS, and vanilla JavaScript

src/test/java/ - JUnit 5 and Arquillian integration tests

db/ - MySQL schema and migration scripts

deploy/ - WildFly CLI scripts and datasource XML descriptors