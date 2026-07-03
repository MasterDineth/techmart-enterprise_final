# TechMart Enterprise Platform

Jakarta EE modernization prototype for TechMart Online — WildFly + MySQL.

## Architecture Overview

```
Browser (index.html)
  │  REST/JSON ──► JAX-RS Resources (/api/*)
  │  SSE ────────► SseResource ──► NotificationBroadcaster
  │
  ├─ OrderResource      POST /api/orders, GET /api/orders
  ├─ ProductResource    GET  /api/products
  ├─ InventoryResource  GET  /api/inventory
  └─ MetricsResource    GET  /api/metrics

JAX-RS ──► EJB Services (Stateless, pooled)
  ├─ OrderService      placeOrder → JMS queue → OrderProcessorMDB → fulfilOrder
  ├─ ProductService    catalogue reads (NamedQuery + 2nd-level cache)
  ├─ InventoryService  optimistic-lock reservation (no overselling)
  └─ CustomerService   get-or-create guest checkout

JMS
  ├─ orderQueue        → OrderProcessorMDB  (pool=20, persistent)
  └─ notificationTopic → NotificationMDB    (pool=10, non-persistent, TTL=60s)
                              └─► NotificationBroadcaster ──► SSE clients

Singletons
  ├─ PerformanceMonitor  lock-free metrics registry (@Startup)
  ├─ InventoryCache      container-managed concurrency read/write lock
  ├─ StartupService      JNDI verify + demo data seed
  └─ JmsConfig           @JMSDestinationDefinition declarations

Stateful
  └─ ShoppingCart        per-session cart, @StatefulTimeout=30 min
```

## Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 17+ |
| WildFly | 31+ (Full Profile) |
| MySQL | 8.x |
| Maven | 3.9+ |

## Quick Start

### 1. Database

```bash
mysql -u root -p < db/schema.sql
```

This creates the `techmart` database, the `techmart` user, and all tables.

### 2. WildFly — MySQL Driver + Datasource

**Option A — CLI script (recommended)**

Edit `deploy/install.cli` and replace `/path/to/mysql-connector-j-8.4.0.jar` with the actual path, then:

```bash
# Start WildFly with full profile
$WILDFLY_HOME/bin/standalone.sh -c standalone-full.xml

# In another terminal
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=deploy/install.cli
```

**Option B — Deployable datasource**

1. Install the MySQL driver module manually under `$WILDFLY_HOME/modules/com/mysql/main/`.
2. Copy `deploy/techmart-ds.xml` to `$WILDFLY_HOME/standalone/deployments/`.

### 3. Build & Deploy

```bash
mvn clean package
cp target/techmart.war $WILDFLY_HOME/standalone/deployments/
```

Or use the WildFly Maven plugin (server must be running):

```bash
mvn wildfly:deploy
```

### 4. Open the Dashboard

```
http://localhost:8080/techmart/
```

The `StartupService` seeds 8 demo products across 3 warehouses on first deploy.

---

## REST API

All endpoints are under `http://localhost:8080/techmart/api/`.

### Products

| Method | Path | Description |
|--------|------|-------------|
| GET | `/products` | List all products with live availability |
| GET | `/products/{id}` | Single product |

### Inventory

| Method | Path | Description |
|--------|------|-------------|
| GET | `/inventory` | All inventory items per warehouse |

### Orders

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/orders` | `PlaceOrderRequest` JSON | Place a new order |
| GET | `/orders` | — | List recent orders (max 100) |
| GET | `/orders/{id}` | — | Single order detail |

**Place order example:**

```bash
curl -X POST http://localhost:8080/techmart/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerName": "Alice",
    "customerEmail": "alice@example.com",
    "lines": [
      {"productId": 1, "quantity": 2},
      {"productId": 3, "quantity": 1}
    ]
  }'
```

### Metrics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/metrics` | Counters + per-method timing stats |

### SSE (Live Notifications)

```
GET /api/notifications/stream   Accept: text/event-stream
```

Events: `ORDER_CONFIRMED`, `INVENTORY_LOW`, `notification`

---

## Running Tests

### Unit Tests (no server required)

```bash
mvn test
```

Covers: `PerformanceMonitor`, `MethodStats`, domain entities, `ShoppingCart`.

### Integration Tests (Arquillian, requires WildFly)

```bash
# Start WildFly first (standalone-full.xml)
mvn verify -Parq-managed -Dwildfly.home=/path/to/wildfly
```

IT test classes:
- `ProductServiceIT` — catalogue and inventory queries
- `OrderServiceIT` — full order placement flow, error paths, metrics recording
- `PerformanceIT` — concurrent load test (20 users × 5 orders), latency assertion ≤ 500 ms avg

---

## Performance Design

| Concern | Solution |
|---------|----------|
| 10 000+ concurrent users | Stateless EJB pool; WildFly thread pool |
| Overselling | Optimistic locking (`@Version`) on `InventoryItem` with retry |
| Sub-second checkout | `placeOrder` only reserves + persists PENDING, then queues; MDB fulfils async |
| Read storm on inventory | `InventoryCache` singleton (container-managed R/W lock) |
| DB connection saturation | HikariCP-style pool: min=20, max=200, prepared-stmt cache=64 |
| JMS throughput | `orderQueue` pool=20 MDB instances; `notificationTopic` NON_PERSISTENT TTL=60s |
| Batch writes | `hibernate.jdbc.batch_size=50`, ordered inserts/updates |
| Metrics | Lock-free `AtomicLong` counters + `PerformanceInterceptor` on all `@Monitored` beans |

---

## Project Structure

```
src/main/java/com/techmart/
  cart/          ShoppingCart (Stateful EJB)
  config/        StartupService (Singleton, seeds demo data)
  dto/           Request/response DTOs
  entity/        JPA entities + enums
  interceptor/   @Monitored + PerformanceInterceptor
  jms/           JmsConfig, OrderMessageProducer, NotificationPublisher
  mdb/           OrderProcessorMDB, NotificationMDB
  metrics/       PerformanceMonitor (Singleton), MethodStats
  rest/          JAX-RS resources + SseResource + NotificationBroadcaster
  service/       OrderService, ProductService, InventoryService,
                 CustomerService, InventoryCache

src/main/webapp/
  index.html     Single-page dashboard (products, cart, orders, metrics, SSE)
  WEB-INF/
    web.xml      Session config, distributable
    beans.xml    CDI activation

src/main/resources/META-INF/
  persistence.xml  JTA PU, Hibernate tuning

deploy/
  techmart-ds.xml  Deployable datasource descriptor
  install.cli      WildFly CLI provisioning script

db/
  schema.sql       MySQL DDL + user creation
```
