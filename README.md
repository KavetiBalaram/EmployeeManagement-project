# Employee Management System

Spring Boot 3 REST API for employees and departments (Java 17, MySQL, JPA).

## Run

1. Start MySQL. Use your own, or: `DB_PASSWORD=yourpassword docker compose up -d`
2. Set credentials and start the app:

```bash
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
mvn spring-boot:run
```

The `employeetable` database is created automatically if missing.
Run the tests (no MySQL needed, they use in-memory H2): `mvn test`

## Endpoints

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/departments` | List departments with employee counts |
| GET | `/api/departments/{id}` | One department |
| POST | `/api/departments` | Create |
| PUT | `/api/departments/{id}` | Rename |
| DELETE | `/api/departments/{id}` | Delete (409 if it still has employees) |
| GET | `/api/employees?q=&departmentId=&page=&size=&sort=` | Search, filter, page, sort |
| GET | `/api/employees/{id}` | One employee |
| POST | `/api/employees` | Create |
| PUT | `/api/employees/{id}` | Update (full replace) |
| DELETE | `/api/employees/{id}` | Delete |

Sortable fields: `id`, `firstName`, `lastName`, `email`, `salary`. Max page size is 100.

## Try it

```bash
# 1. Departments
curl -i -X POST localhost:8080/api/departments -H 'Content-Type: application/json' -d '{"name":"Engineering"}'
curl -i -X POST localhost:8080/api/departments -H 'Content-Type: application/json' -d '{"name":"HR"}'

# 2. Employees (departmentId is optional)
curl -i -X POST localhost:8080/api/employees -H 'Content-Type: application/json' \
  -d '{"firstName":"Ann","lastName":"Lee","email":"ann@acme.com","salary":5000.50,"departmentId":1}'
curl -i -X POST localhost:8080/api/employees -H 'Content-Type: application/json' \
  -d '{"firstName":"Bob","lastName":"Stone","email":"bob@acme.com","salary":4000,"departmentId":1}'

# 3. Search, filter, page, sort
curl 'localhost:8080/api/employees?q=stone'
curl 'localhost:8080/api/employees?departmentId=1&size=1&page=0&sort=salary,desc'

# 4. Move an employee to HR
curl -X PUT localhost:8080/api/employees/1 -H 'Content-Type: application/json' \
  -d '{"firstName":"Ann","lastName":"Lee","email":"ann@acme.com","salary":5500,"departmentId":2}'

# 5. Error cases
curl -i -X POST localhost:8080/api/employees -H 'Content-Type: application/json' \
  -d '{"firstName":"","lastName":"X","email":"bad","salary":-1}'            # 400 + field errors
curl -i -X POST localhost:8080/api/employees -H 'Content-Type: application/json' \
  -d '{"firstName":"A","lastName":"B","email":"ann@acme.com","salary":1}'    # 409 duplicate email
curl -i -X DELETE localhost:8080/api/departments/1                           # 409 while it has employees
curl -i localhost:8080/api/employees/9999                                    # 404
```

Errors are JSON problem details. Validation errors include an `errors` map keyed by field.
