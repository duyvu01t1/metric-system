# Metric System - Tailoring Management Application

A comprehensive web-based system for managing tailoring shop operations, customer information, measurements, and orders. Built with Spring Boot, PostgreSQL, and Bootstrap.

## Project Overview

The Metric System is designed to help tailoring shops efficiently manage:
- **Customer Management**: Store and manage customer information
- **Order Management**: Track tailoring orders from creation to completion
- **Measurement Tracking**: Record and manage customer measurements
- **Payment Processing**: Track payments and outstanding balances
- **Role-Based Access Control**: Admin and User roles with different permissions
- **Reporting & Analytics**: Dashboard with key statistics and insights

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence
- **Spring Cache** - Caching support
- **Spring Batch** - Batch processing
- **Flyway** - Database migrations
- **JWT** - Token-based authentication
- **Swagger/OpenAPI** - API documentation

### Frontend
- **Bootstrap 5** - Responsive UI framework
- **jQuery** - DOM manipulation and AJAX
- **Chart.js** - Data visualization
- **HTML5/CSS3** - Modern web standards

### Database
- **PostgreSQL** - Primary database
- **Flyway** - Database schema versioning

### Build Tool
- **Maven 3.8+** - Project build and dependency management

## Project Structure

```
metric-system/
├── src/
│   ├── main/
│   │   ├── java/com/tailorshop/metric/
│   │   │   ├── config/                 # Configuration classes
│   │   │   ├── controller/             # REST API controllers
│   │   │   ├── service/                # Business logic
│   │   │   ├── repository/             # Data access layer
│   │   │   ├── entity/                 # JPA entities
│   │   │   ├── dto/                    # Data transfer objects
│   │   │   ├── security/               # Security configuration
│   │   │   ├── exception/              # Custom exceptions
│   │   │   └── MetricSystemApplication.java
│   │   ├── resources/
│   │   │   ├── application.yml         # Application configuration
│   │   │   ├── db/migration/           # Flyway SQL migrations
│   │   │   ├── static/
│   │   │   │   ├── css/                # Stylesheets
│   │   │   │   ├── js/                 # JavaScript files
│   │   │   │   └── img/                # Images
│   │   │   └── templates/              # HTML templates
│   └── test/
│       └── java/com/tailorshop/metric/ # Unit tests
├── pom.xml                             # Maven configuration
└── README.md
```

## Database Schema

The application includes the following main entities:

### User Management
- `users` - User accounts
- `user_roles` - Role definitions
- `user_role_mappings` - User-role associations

### Business Data
- `customers` - Customer information
- `tailoring_orders` - Orders
- `measurements` - Measurement records
- `measurement_templates` - Measurement template definitions
- `measurement_fields` - Fields for each template

### Financial Data
- `payments` - Payment records

### Auditing
- `audit_logs` - Change tracking
- `api_access_logs` - API usage logs

## Setup & Installation

### Prerequisites
- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.8 or higher
- Git

### Quick Start (Automated Setup)

**Windows (PowerShell):**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\setup-database.ps1
```

**Windows (CMD):**
```cmd
setup-database.bat
```

**Linux/Mac:**
```bash
chmod +x setup-database.sh
./setup-database.sh
```

The setup script will:
- Create PostgreSQL database automatically
- Build the project with Maven
- Run Flyway migrations to initialize database schema
- Start the application

---

### Manual Setup Steps

#### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd metric-system
```

#### Step 2: Create PostgreSQL Database
```bash
psql -U postgres -c "CREATE DATABASE metric_system;"
```

Or with user:
```sql
CREATE DATABASE metric_system;
CREATE USER metric_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE metric_system TO metric_user;
```

#### Step 3: Configure Application Properties (Optional)

Edit `src/main/resources/application.yml` if you need custom settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/metric_system
    username: postgres
    password: postgres
```

#### Step 4: Build the Project
```bash
mvn clean install -DskipTests
```

This will automatically run Flyway migrations to create the database schema.

#### Step 5: Run the Application
```bash
mvn spring-boot:run
```

Or using Java:
```bash
java -jar target/metric-system-1.0.0.jar
```

The application will start at `http://localhost:8080/api`

---

### Database Initialization with Flyway

Flyway automatically initializes the database schema on first run:

**Location:** `src/main/resources/db/migration/`

**Configuration:** `src/main/resources/application.yml`

```yaml
spring:
  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true
    enabled: true
```

**Current Migrations:**
- `V1__create_initial_schema.sql` - Creates all tables, indexes, sequences, and initial data

**To view migration history:**
```bash
psql -U postgres -d metric_system -c "SELECT * FROM flyway_schema_history;"
```

For detailed setup information, see [SETUP_GUIDE.md](./SETUP_GUIDE.md)

## Configuration

### OAuth2 Setup (Optional)

#### Google OAuth2
1. Create a project in [Google Cloud Console](https://console.cloud.google.com)
2. Create OAuth 2.0 credentials (Web application)
3. Add to `application.yml`:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
```

#### Azure AD Setup
1. Register an application in [Azure Portal](https://portal.azure.com)
2. Create a client secret
3. Add to `application.yml`:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          azure:
            client-id: YOUR_AZURE_CLIENT_ID
            client-secret: YOUR_AZURE_CLIENT_SECRET
```

### JWT Configuration
Update JWT secret in `application.yml`:
```yaml
app:
  jwt:
    secret: your-very-secure-secret-key-min-32-chars
    expiration: 86400000  # 24 hours
```

## Features

### Authentication & Authorization
- Local login with username/password
- OAuth2 integration (Google, Azure)
- JWT token-based authentication
- Role-based access control (RBAC)
- Session management

### Dashboard
- Real-time statistics
- Recent orders display
- Order status distribution
- Payment overview

### Customer Management
- Add/Edit/Delete customers
- Search and filter
- Customer history
- Contact information

### Order Management
- Create tailoring orders
- Track order status
- Define order types (Suit, Shirt, Pants, Dress, Custom)
- Associate measurements
- Set promised dates

### Measurements
- Pre-defined measurement templates
- Custom measurement fields
- Record customer measurements
- Track measurement history

### Reporting
- Order statistics
- Payment reports
- Customer analytics
- Audit logs

## API Documentation

API documentation is available at:
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with credentials
- `POST /api/auth/logout` - Logout
- `POST /api/auth/refresh` - Refresh JWT token

### Users
- `GET /api/users` - List users
- `GET /api/users/{id}` - Get user details
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Customers
- `GET /api/customers` - List customers
- `GET /api/customers/{id}` - Get customer details
- `POST /api/customers` - Create customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

### Orders
- `GET /api/orders` - List orders
- `GET /api/orders/{id}` - Get order details
- `POST /api/orders` - Create order
- `PUT /api/orders/{id}` - Update order
- `DELETE /api/orders/{id}` - Delete order

### Measurements
- `GET /api/measurements` - List measurements
- `GET /api/measurements/{id}` - Get measurement details
- `POST /api/measurements` - Record measurement
- `PUT /api/measurements/{id}` - Update measurement

### Dashboard
- `GET /api/dashboard/statistics` - Get dashboard statistics
- `GET /api/dashboard/recent-orders` - Get recent orders
- `GET /api/dashboard/payment-summary` - Get payment summary

## Security

### Authentication Methods
1. **Local Authentication**: Username/Password
2. **OAuth2**: Google and Azure integration
3. **JWT**: Token-based stateless authentication

### Authorization
- Role-based access control
- Method-level security with @PreAuthorize
- Entity-level filtering based on user permissions

### Password Security
- BCrypt hashing for stored passwords
- Password validation rules
- Account lockout after failed attempts

## Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t metric-system:1.0.0 .

# Run container
docker run -d -p 8080:8080 \
  -e DB_HOST=db-host \
  -e DB_NAME=metric_system \
  -e DB_USER=metric_user \
  -e DB_PASSWORD=secure_password \
  metric-system:1.0.0
```

### Production Checklist
- [ ] Change default JWT secret
- [ ] Configure OAuth2 credentials
- [ ] Update CORS allowed origins
- [ ] Enable HTTPS
- [ ] Set up proper logging
- [ ] Configure database backups
- [ ] Set up monitoring and alerts
- [ ] Review security settings

## Troubleshooting

### Database Connection Issues
```
Check PostgreSQL is running and credentials are correct
Verify database exists: SELECT datname FROM pg_database;
```

### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8081
```

### Migration Issues
```bash
# Reset database schema (development only!)
DROP DATABASE metric_system;
CREATE DATABASE metric_system;
# Run application to re-create schema with migrations
```

## Development

### Running Tests
```bash
mvn test
```

### Running with Hot Reload
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.devtools.restart.enabled=true"
```

### Building JAR
```bash
mvn clean package
```

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add new feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit pull request

## License

This project is licensed under the Apache License 2.0 - see LICENSE file for details.

## Support

For support, please contact: support@tailorsystem.local

## Changelog

### Version 1.0.0 (Initial Release)
- Initial project structure
- Basic authentication and authorization
- CRUD operations for all entities
- Dashboard with statistics
- OAuth2 integration setup
- API documentation with Swagger

---

**Last Updated**: April 2026
**Version**: 1.0.0
