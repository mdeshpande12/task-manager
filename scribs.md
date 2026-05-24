# Database Integration Steps

## 1. Dependencies added to `pom.xml`

- `dropwizard-jdbi3` — JDBI 3 integration for database access
- `dropwizard-db` — provides `DataSourceFactory` for connection pooling
- `dropwizard-migrations` — Liquibase-based schema migrations support
- `postgresql` (42.7.1) — PostgreSQL JDBC driver

## 2. Configuration (`config.yml`)

Added a `database` section with PostgreSQL (Neon) connection settings:

```yaml
database:
  driverClass: org.postgresql.Driver
  url: jdbc:postgresql://ep-XXXXX.us-east-2.aws.neon.tech/task_manager?sslmode=require
  user: REPLACE_WITH_YOUR_NEON_USER
  password: REPLACE_WITH_YOUR_NEON_PASSWORD
  maxWaitForConnection: 1s
  validationQuery: "SELECT 1"
  validationQueryTimeout: 3s
  minSize: 2
  maxSize: 8
  checkConnectionWhileIdle: true
  evictionInterval: 10s
  minIdleTime: 1 minute
```

## 3. Configuration class (`TaskManagerConfiguration.java`)

Added a validated `DataSourceFactory` field with getter/setter, exposed via the `"database"` JSON property:

```java
@Valid
@NotNull
@JsonProperty("database")
private DataSourceFactory database = new DataSourceFactory();
```

## 4. Application bootstrap (`TaskManagerApplication.java`)

Used `JdbiFactory` to build a managed `Jdbi` instance from the configuration:

```java
final JdbiFactory factory = new JdbiFactory();
final Jdbi jdbi = factory.build(environment, configuration.getDatabase(), "postgresql");
```

## 5. Database schema (`src/main/resources/migrations.sql`)

Defined three tables:

- **users** — id, username, email, full_name, timestamps
- **projects** — id, name, description, owner_id (FK → users), timestamps
- **tasks** — id, title, description, status, priority, project_id, assignee_id, creator_id, due_date, timestamps

With constraints (`status IN ('TODO','IN_PROGRESS','DONE','CANCELLED')`, `priority IN ('LOW','MEDIUM','HIGH','CRITICAL')`) and indexes on status, assignee, project, and due_date.

## Next Steps

- ~~Create DAO interfaces in `db/`~~  ✅
- ~~Create domain model classes in `core/`~~ ✅ (using OpenAPI-generated models instead)
- ~~Create REST resource classes in `resources/`~~ ✅

---

## 6. OpenAPI Code Generation

Added `openapi-generator-maven-plugin` to `pom.xml` to auto-generate JAX-RS interfaces and model classes from an OpenAPI spec.

**Plugin config:**
- Generator: `jaxrs-spec` (produces Jersey-compatible interfaces for Dropwizard)
- Input spec: `src/main/resources/openapi/task-manager.yaml`
- `interfaceOnly: true` — generates only interfaces, we implement them
- `useJakartaEe: true` — uses `jakarta.*` imports (Dropwizard 4.x)
- `dateLibrary: java8` — uses `java.time.LocalDate` / `OffsetDateTime`

**Also added:**
- `build-helper-maven-plugin` — adds `target/generated-sources/openapi/src/gen/java` as a source directory
- `swagger-annotations` dependency — required by generated code

**Generated files (in `target/generated-sources/`):**
- `TasksApi.java`, `UsersApi.java`, `ProjectsApi.java` — REST interfaces
- `Task.java`, `User.java`, `Project.java` — response models
- `CreateTaskRequest.java`, `CreateUserRequest.java`, `CreateProjectRequest.java` — request models

## 7. DAO Interfaces (`db/`)

Created JDBI 3 DAO interfaces using `@RegisterBeanMapper` for auto-mapping:

- `TaskDAO` — findById, findAll, findByProjectId, findByAssigneeId, findByCreatorId, create, update, delete
- `UserDAO` — findById, findByUsername, findByEmail, findAll, create, update, delete
- `ProjectDAO` — findById, findByOwnerId, findAll, create, update, delete

**Key annotations used:**
| Annotation | Purpose |
|---|---|
| `@SqlQuery` | SELECT statements |
| `@SqlUpdate` | INSERT/UPDATE/DELETE |
| `@Bind("name")` | Binds a single param to `:name` in SQL |
| `@BindBean` | Binds all getters of an object |
| `@GetGeneratedKeys` | Returns auto-generated ID after INSERT |
| `@RegisterBeanMapper` | Auto-maps columns to bean setters (snake_case → camelCase) |

**Note:** Use `java.util.Optional` (NOT Guava's shaded Optional) for return types. JDBI only registers a mapper for `java.util.Optional`.

## 8. REST Resource Implementations (`resources/`)

Implemented the generated interfaces:

- `TasksResource implements TasksApi`
- `UsersResource implements UsersApi`
- `ProjectsResource implements ProjectsApi`

Each resource:
1. Takes a DAO via constructor injection
2. Maps request objects → domain objects
3. Calls DAO methods
4. Returns 404 (`NotFoundException`) when entity not found

## 9. Application Wiring (`TaskManagerApplication.java`)

**`initialize()` method:**
- Registered `MigrationsBundle` to enable `db migrate` CLI command

**`run()` method:**
- Creates DAOs via `jdbi.onDemand(XxxDAO.class)`
- Registers resource classes with `environment.jersey().register(...)`

## 10. Database Migration Setup

- Created `migrations.xml` (Liquibase XML changelog) that references `migrations.sql`
- Dropwizard's migrations bundle requires Liquibase XML format, not plain SQL

**Commands to run:**
```bash
mvn clean package -DskipTests
java -jar target/task-manager-1.0-SNAPSHOT.jar db migrate config.yml
java -jar target/task-manager-1.0-SNAPSHOT.jar server config.yml
```

## Lessons Learned / Gotchas

1. **JDBC URL format:** Use `jdbc:postgresql://host/db?params` — do NOT put `user:password@` in the URL. Credentials go in separate `user`/`password` fields in `config.yml`
2. **Guava Optional vs java.util.Optional:** JDBI only supports `java.util.Optional`. Never use `io.dropwizard.logback.shaded.guava.base.Optional`
3. **OpenAPI generator needs the YAML file to exist** before `mvn compile` — otherwise you get a `NullPointerException` (openAPI is null)
4. **Generated sources need `build-helper-maven-plugin`** to be added to the compiler source path
5. **Swagger annotations dependency** (`io.swagger:swagger-annotations`) is required by the generated code
