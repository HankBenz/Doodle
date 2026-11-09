# Doodle

Doodle is a small meeting scheduling backend built with Spring Boot and Java. It lets users create and manage calendar time slots, mark availability as free or busy, book available slots as meetings, and query availability for a time period. Data is persisted in PostgreSQL. 
The project is a backend implementation of the supplied coding challenge; calendars are represented in the domain rather than as a separate UI feature.

## Start locally

Run the application and its dependencies with Docker Compose from the project root:

```powershell
docker compose up --build
```

The main endpoints are:

- Application: <http://localhost:18080>
- Swagger UI: <http://localhost:18080/swagger-ui.html>
- Keycloak: <http://localhost:8180>
- pgAdmin: <http://localhost:5050>

## Run tests

With Docker running, run the Gradle test suite from the project root:

```powershell
.\gradlew.bat test
```

The integration tests use Testcontainers to start PostgreSQL and Keycloak.

## Keycloak logins

The imported realm is `doodle`.

| Purpose | URL | Username | Password |
| --- | --- | --- | --- |
| Doodle realm user | <http://localhost:8180> | `doodle` | `doodle` |
| Keycloak admin console | <http://localhost:8180/admin> | `admin` | `admin` |

The realm user is for application authentication. The admin account is for the Keycloak administration console.

## pgAdmin and database

Open pgAdmin at <http://localhost:5050> and sign in with:

- Email: `admin@doodle.dev`
- Password: `admin`

The `Doodle` PostgreSQL server is preconfigured in pgAdmin. Select its `doodle` database. If adding a connection manually, use:

| Setting | Value |
| --- | --- |
| Host (from pgAdmin) | `postgres` |
| Port | `5432` |
| Database | `doodle` |
| Username | `doodle` |
| Password | `P@s3woooord` |

## Limitations

- Authentication is configured, but fine-grained authorization policies and role/scope checks are not implemented.
- Resource ownership is not enforced. Authenticated users are not yet restricted to viewing, changing, or deleting only their own slots and meetings.
