# Inventory Management System — REST API

Java 17 + Spring Boot 3 + MySQL. A real backend implementation of the Inventory Management
System project — REST API, normalized relational schema, and optimized aggregate SQL queries.

## Stack
- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation)
- MySQL 8
- Maven
- springdoc-openapi (Swagger UI)
- Lombok

## Setup

1. **Create the database** (or let `createDatabaseIfNotExist=true` in the JDBC URL do it for you):
   ```sql
   CREATE DATABASE inventory_db;
   ```

2. **Set your MySQL credentials** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_actual_password
   ```

3. **Run the app:**
   ```bash
   mvn spring-boot:run
   ```
   On first run, Hibernate creates all tables (`ddl-auto=update`) and `data.sql` seeds two
   categories and two suppliers so you have something to attach products to immediately.

4. **Explore the API:**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Raw OpenAPI spec: http://localhost:8080/api-docs

## Quick demo walkthrough

```bash
# 1. Check seeded categories/suppliers
curl http://localhost:8080/api/categories
curl http://localhost:8080/api/suppliers

# 2. Create a product (use the category/supplier IDs returned above)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Wireless Mouse",
        "sku": "WM-001",
        "categoryId": 1,
        "supplierId": 1,
        "unitPrice": 799.00,
        "reorderLevel": 20,
        "initialQuantity": 50,
        "warehouseLocation": "Rack A1"
      }'

# 3. List products (paginated, filterable)
curl "http://localhost:8080/api/products?page=0&size=10&categoryId=1"

# 4. Stock movement
curl -X POST http://localhost:8080/api/inventory/1/stock-out \
  -H "Content-Type: application/json" \
  -d '{"quantity": 60, "referenceNote": "Bulk order", "createdBy": "admin"}'
# → 409 Conflict: "Insufficient stock ... requested 60, available 50"

curl -X POST http://localhost:8080/api/inventory/1/stock-in \
  -H "Content-Type: application/json" \
  -d '{"quantity": 10, "referenceNote": "Restock", "createdBy": "admin"}'

# 5. Transaction history for a product
curl http://localhost:8080/api/inventory/1/transactions

# 6. Low-stock alert
curl http://localhost:8080/api/inventory/low-stock

# 7. Reports — real aggregate SQL, not looped in Java
curl http://localhost:8080/api/reports/stock-value
curl http://localhost:8080/api/reports/category-summary
curl http://localhost:8080/api/reports/top-moving-products?days=30
```

## What to point at in an interview

- **`ProductRepository.search()`** — one `@EntityGraph` + JPQL query handles filtering,
  pagination, AND eager-loads `category`/`supplier`/`inventory` in the same query, avoiding the
  classic N+1 problem. Turn on `logging.level.org.hibernate.SQL=DEBUG` (already set) and watch
  the console — listing 20 products fires **one** SELECT, not 21.
- **`ReportRepository`** — `stock-value`, `category-summary`, and `top-moving-products` are all
  native aggregate SQL (`SUM`, `GROUP BY`) computed by MySQL, not Java loops over all rows. This
  is the concrete answer to "what did you actually optimize?"
- **`InventoryService.stockOut()`** — validates sufficient quantity before decrementing, throwing
  a `409 Conflict` instead of allowing negative stock; every stock change writes an
  `inventory_transaction` audit row rather than only overwriting the running total.
- **Indexes** — `product.sku` (unique), `product.category_id`, `product.supplier_id`,
  `inventory_transaction.product_id`, `inventory_transaction.created_at` — declared directly on
  the `@Table(indexes = ...)` annotations, matching what each query above actually filters/joins on.

## Not yet implemented (documented as next steps, don't claim these are done)

- Authentication/authorization (Spring Security + JWT) — endpoints are currently open
- Unit tests (JUnit/Mockito) for the service layer
- Docker/`docker-compose` for one-command startup
- Database migrations (Flyway/Liquibase) instead of `ddl-auto=update`

## Project structure

```
src/main/java/com/inventory/
├── InventoryApiApplication.java
├── entity/         Category, Supplier, Product, Inventory, InventoryTransaction
├── repository/     Spring Data JPA repositories + native aggregate queries (ReportRepository)
├── dto/            Request/response DTOs (validated separately from entities)
├── service/        ProductService, InventoryService — business logic lives here
├── controller/      REST endpoints (Product, Inventory, Report, Category, Supplier)
└── exception/       Custom exceptions + @RestControllerAdvice global handler
src/main/resources/
├── application.properties
└── data.sql        seed data (categories + suppliers)
```
