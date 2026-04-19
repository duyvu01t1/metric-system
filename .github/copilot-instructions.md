# Metric System - Development Guidelines

## Project Description

Metric System is a comprehensive web-based tailoring shop management application built with Spring Boot and PostgreSQL. The system manages customer information, tailoring orders, measurements, payments, and provides role-based access control with OAuth2 authentication.

## Architecture Overview

### Backend Architecture
- **Layered Architecture**: Controller → Service → Repository → Entity
- **Security**: Spring Security with JWT tokens and OAuth2 support
- **Database**: PostgreSQL with Flyway migrations
- **API**: RESTful API with Swagger/OpenAPI documentation
- **Caching**: Spring Cache for performance optimization

### Frontend Architecture
- **Template Engine**: Thymeleaf (optional, uses HTML with Bootstrap)
- **Framework**: Bootstrap 5 for responsive design
- **JavaScript**: jQuery for AJAX calls and DOM manipulation
- **Charts**: Chart.js for data visualization

## Project Structure

```
metric-system/
├── src/main/java/com/tailorshop/metric/
│   ├── config/              # Spring configurations
│   ├── controller/          # REST API endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Database access
│   ├── entity/              # JPA entities
│   ├── dto/                 # Data transfer objects
│   ├── security/            # Security config
│   └── exception/           # Custom exceptions
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL migrations
│   ├── application.yml      # Configuration
│   ├── static/              # CSS, JS, images
│   └── templates/           # HTML templates
└── pom.xml                  # Maven configuration
```

## Key Technologies

| Component | Technology |
|-----------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.0 |
| **Security** | Spring Security + JWT |
| **Database** | PostgreSQL |
| **Build** | Maven |
| **Frontend** | Bootstrap 5 + jQuery |
| **Migration** | Flyway |
| **API Docs** | Swagger/OpenAPI |

## Development Workflow

### 1. Setting Up Development Environment

```bash
# Clone repository
git clone <repo-url>
cd metric-system

# Create database
createdb -U postgres metric_system

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

### 2. Database Migrations

All database changes must be made via Flyway migration scripts:
- Location: `src/main/resources/db/migration/`
- Naming: `V{number}__{description}.sql`
- Example: `V2__add_users_table.sql`

### 3. Adding New Features

#### Step 1: Create Entity
```java
@Entity
@Table(name = "table_name")
public class EntityName {
    // Properties and annotations
}
```

#### Step 2: Create Repository
```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {
    // Custom query methods
}
```

#### Step 3: Create DTO
```java
@Data
@Builder
public class EntityDTO {
    // DTO fields
}
```

#### Step 4: Create Service
```java
@Service
public class EntityService {
    // Business logic
}
```

#### Step 5: Create Controller
```java
@RestController
@RequestMapping("/api/entities")
public class EntityController {
    // REST endpoints
}
```

## Coding Standards

### Java Code Style
- Use meaningful variable and method names
- Follow Java naming conventions (camelCase)
- Add JavaDoc comments for public methods
- Use Lombok annotations to reduce boilerplate
- Max line length: 120 characters

### Example:
```java
/**
 * Create a new customer
 * @param customerDTO Customer data
 * @return Created customer
 */
@PostMapping
public ApiResponse<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO) {
    CustomerDTO created = customerService.create(customerDTO);
    return ApiResponse.success("Customer created successfully", created);
}
```

### SQL Naming Conventions
- Table names: `snake_case`
- Column names: `snake_case`
- Primary keys: `id`
- Foreign keys: `{entity}_id`
- Timestamps: `created_at`, `updated_at`

## API Design Guidelines

### Response Format
All API responses should follow the standard format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* actual data */ },
  "timestamp": "2026-04-04T10:30:00"
}
```

### Error Response Format
```json
{
  "success": false,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Customer not found",
  "timestamp": "2026-04-04T10:30:00"
}
```

### HTTP Status Codes
- `200` - OK
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error

## Authentication & Authorization

### User Roles
1. **ADMIN** - Full system access
2. **USER** - Limited access to assigned customers and orders

### Implementing Role-Based Authorization
```java
@GetMapping("/admin-only")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnlyEndpoint() {
    return ResponseEntity.ok("Admin access");
}
```

## Testing Guidelines

### Unit Testing
```bash
mvn test
```

### Test Structure
```java
@SpringBootTest
public class EntityServiceTest {
    @MockBean
    private EntityRepository repository;
    
    @InjectMocks
    private EntityService service;
    
    @Test
    public void testCreateEntity() {
        // Arrange, Act, Assert
    }
}
```

## Deployment

### Building for Production
```bash
mvn clean package
```

### Environment Configuration
Create `application-prod.yml` for production settings:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

## Common Tasks

### Add a New Endpoint
1. Create/Update Controller method
2. Add `@PostMapping`, `@GetMapping`, etc.
3. Handle request validation and error cases
4. Document with Swagger annotations
5. Write unit tests

### Add a New Database Field
1. Create migration: `V{n}__add_{field}_to_{table}.sql`
2. Update Entity class
3. Update DTO if needed
4. Update Service layer
5. Update Controller if needed

### Fix a Bug
1. Create a test that reproduces the bug
2. Fix the issue
3. Ensure test passes
4. Run full test suite
5. Commit with descriptive message

## Code Review Checklist

- [ ] Code follows naming conventions
- [ ] Business logic is in Service layer
- [ ] API responses follow standard format
- [ ] Tests are added/updated
- [ ] Database migrations are provided
- [ ] Documentation is updated
- [ ] No hardcoded values
- [ ] Security considerations addressed
- [ ] Error handling implemented
- [ ] Deprecations are handled

## Useful Commands

```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=EntityServiceTest

# Run application
mvn spring-boot:run

# Package application
mvn clean package

# Check code quality
mvn checkstyle:check

# Generate API documentation
mvn javadoc:javadoc

# Show dependency tree
mvn dependency:tree
```

## Configuration Files

### application.yml locations
- `application.yml` - Default configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

### Running with specific profile
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## Troubleshooting

### Common Issues

**Issue**: Port 8080 already in use
```bash
# Change port in application.yml or run with:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Issue**: Database connection error
```bash
# Verify PostgreSQL is running and credentials are correct
psql -U postgres -d metric_system
```

**Issue**: Flyway migration errors
```bash
# Reset database (development only)
mvn flyway:clean
mvn flyway:migrate
```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Bootstrap 5 Documentation](https://getbootstrap.com/docs/5.0/)
- [Swagger/OpenAPI](https://swagger.io/tools/swagger-ui/)

## Contact & Support

For questions or issues, please reach out to the development team at: support@tailorsystem.local

## Version History

- **v1.0.0** (2026-04-04) - Initial release with core functionality
  - User authentication and authorization
  - Customer and order management
  - Measurement tracking
  - Dashboard and reporting
  - OAuth2 integration (Google, Azure)
