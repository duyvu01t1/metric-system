# Hướng Dẫn Khởi Tạo Database & Chạy Ứng Dụng

## 1. Chuẩn Bị Tiên Quyết

### Yêu cầu:
- PostgreSQL 12+ (đã cài đặt và đang chạy)
- Java 17+
- Maven 3.8+

### Kiểm tra cài đặt:
```bash
# Kiểm tra PostgreSQL
psql --version

# Kiểm tra Java
java -version

# Kiểm tra Maven
mvn --version
```

---

## 2. Khởi Tạo Database (Với Flyway)

### **Cách 1: Sử dụng Script (Dễ nhất)**

#### Windows (PowerShell):
```powershell
# Cho phép chạy script
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Chạy setup script
.\setup-database.ps1
```

#### Windows (CMD):
```cmd
setup-database.bat
```

#### Linux/Mac:
```bash
bash setup-database.sh
```

---

### **Cách 2: Chạy thủ công**

#### Bước 1: Tạo database PostgreSQL
```bash
psql -U postgres -c "CREATE DATABASE metric_system;"
```

#### Bước 2: Build project với Maven
```bash
mvn clean install -DskipTests
```

Flyway sẽ tự động:
- Quét folder `src/main/resources/db/migration/`
- Tìm tất cả các migration files (V1__*, V2__*, etc.)
- Thực thi chúng theo thứ tự version
- Tạo ra tất cả các tables, indexes, sequences

---

## 3. Chạy Ứng Dụng

### Bước 1: Khởi động ứng dụng
```bash
mvn spring-boot:run
```

Hoặc nếu đã build:
```bash
java -jar target/metric-system-1.0.0.jar
```

### Bước 2: Truy cập ứng dụng
- **API Base**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Dashboard**: http://localhost:8080/dashboard
- **Login**: http://localhost:8080/login

---

## 4. Cấu Hình Flyway

Flyway đã được enable và cấu hình trong `application.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration          # Folder chứa migration scripts
    baseline-on-migrate: true                  # Tạo baseline nếu không tồn tại
    baseline-version: 0                        # Version baseline
    enabled: true                              # Kích hoạt Flyway
    out-of-order: false                        # Không cho phép migration không theo thứ tự
    validate-on-migrate: true                  # Validate lệnh SQL trước khi execute
```

---

## 5. Cấu Trúc Migration Scripts

Các migration scripts nằm trong: `src/main/resources/db/migration/`

**Format tên file:**
```
V{version}__{description}.sql
```

**Ví dụ:**
- `V1__create_initial_schema.sql` (v1.0) - Tạo schema ban đầu
- `V2__add_audit_tables.sql` (v1.1) - Thêm bảng audit
- `V3__add_new_columns.sql` (v1.2) - Thêm cột mới

**Quy tắc:**
- Version phải tăng dần (1, 2, 3, ...)
- Tên phải bắt đầu với `V` (viết hoa)
- Phải có `__` (hai dấu gạch dưới) trước mô tả
- Mô tả có thể chứa chữ, số, dấu gạch dưới

---

## 6. Các Migration Hiện Có

### V1__create_initial_schema.sql
Khởi tạo schema hoàn chỉnh bao gồm:

**User Management:**
- `user_roles` - Định nghĩa roles (ADMIN, USER, MANAGER)
- `users` - Thông tin người dùng
- `user_role_mappings` - Liên kết user-role

**Customer Management:**
- `customers` - Thông tin khách hàng

**Order Management:**
- `tailoring_orders` - Đơn may đo

**Measurements:**
- `measurement_templates` - Mẫu đo lường
- `measurement_fields` - Các trường đo lường
- `measurements` - Dữ liệu đo lường thực tế

**Payments:**
- `payments` - Lịch sử thanh toán

**Auditing:**
- `audit_logs` - Ghi lại các thay đổi
- `api_access_logs` - Ghi lại API access

**Sequences:**
- `customer_code_seq` - Tạo mã khách hàng
- `order_code_seq` - Tạo mã đơn hàng

---

## 7. Kiểm Tra Flyway Hoạt Động

### Xem Flyway Migration History

Sau khi chạy ứng dụng, kiểm tra trong PostgreSQL:

```bash
psql -U postgres -d metric_system -c "SELECT * FROM flyway_schema_history;"
```

**Output mong đợi:**
```
 version |                 description                 | type |    installed_by     | installed_on        | execution_time | success
---------+--------------------------------------------+------+---------------------+---------------------+----------------+---------
       1 | create initial schema                       | S... | postgres            | 2026-04-04 10:30:00 | 1500           | t
```

---

## 8. Kiểm Tra Database Schema

### Xem tất cả tables:
```bash
psql -U postgres -d metric_system -c "\dt"
```

### Xem cấu trúc một table:
```bash
psql -U postgres -d metric_system -c "\d customers"
```

### Xem tất cả sequences:
```bash
psql -U postgres -d metric_system -c "SELECT * FROM information_schema.sequences;"
```

---

## 9. Troubleshooting

### Lỗi 1: "Database does not exist"
```bash
psql -U postgres -c "CREATE DATABASE metric_system;"
```

### Lỗi 2: "Flyway migration failed"
- Kiểm tra các lỗi SQL trong folder `db/migration/`
- Đảm bảo PostgreSQL đang chạy
- Xem logs trong console

### Lỗi 3: "Port 5432 is already in use"
- Đổi port trong `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/metric_system
```

### Lỗi 4: Reset database (Development only)
```bash
# Xóa database
psql -U postgres -c "DROP DATABASE metric_system;"

# Tạo lại
psql -U postgres -c "CREATE DATABASE metric_system;"

# Chạy lại migrations
mvn clean install
```

---

## 10. Các Lệnh Hữu Ích

```bash
# Build project
mvn clean install -DskipTests

# Chạy ứng dụng
mvn spring-boot:run

# Chỉ chạy Flyway (không build)
mvn flyway:migrate

# Xem Flyway info
mvn flyway:info

# Reset Flyway (cẩn thận!)
mvn flyway:clean

# Build JAR
mvn clean package

# Chạy test
mvn test
```

---

## 11. Kết Quả Mong Đợi

Sau khi hoàn tất:

✅ Database `metric_system` được tạo  
✅ Tất cả tables được tạo với proper constraints  
✅ Flyway schema_history được tạo để track migrations  
✅ Ứng dụng Spring Boot khởi động thành công  
✅ API endpoints có sẵn tại http://localhost:8080/api

---

## 12. Tiếp Theo

Sau khi Flyway khởi tạo database:
- Tạo các service classes để handle business logic
- Implement các REST controllers
- Thêm authentication endpoints
- Develop frontend components

---

**Cần giúp?** Xem [README.md](./README.md) hoặc [.github/copilot-instructions.md](./.github/copilot-instructions.md)
