# Google OAuth2 Setup Guide

Để kích hoạt Google login cho ứng dụng Metric System, bạn cần set up Google OAuth2 credentials.

## Bước 1: Tạo Google Cloud Project

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Tạo một dự án mới hoặc chọn dự án hiện có
3. Enable Google+ API:
   - Đi tới `APIs & Services` > `Library`
   - Tìm "Google+ API"
   - Click vào và chọn `Enable`

## Bước 2: Tạo OAuth2 Credentials

1. Đi tới `APIs & Services` > `Credentials`
2. Click `Create Credentials` > `OAuth client ID`
3. Chọn `Web application`
4. Điền thông tin:
   - **Name**: `Metric System (Local)` hoặc tên khác
   - **Authorized JavaScript origins**: `http://localhost:8080`
   - **Authorized redirect URIs**: 
     - `http://localhost:8080/api/login/oauth2/code/google`
     - `http://localhost:8080/login/oauth2/code/google`

5. Click `Create` để tạo credentials
6. Saoliển copy **Client ID** và **Client Secret**

## Bước 3: Cấu hình ứng dụng

### Cách 1: Sử dụng Environment Variables (Được khuyến cáo)

Set environment variables trước khi chạy ứng dụng:

**Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID = "your-client-id-here.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret-here"

# Rồi chạy
mvn spring-boot:run
```

**Windows (CMD):**
```cmd
set GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
set GOOGLE_CLIENT_SECRET=your-client-secret-here

mvn spring-boot:run
```

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="your-client-id-here.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret-here"

mvn spring-boot:run
```

### Cách 2: Sử dụng application.yml (Development only)

Tạo file `application-dev.yml` với nội dung:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id-here.apps.googleusercontent.com
            client-secret: your-client-secret-here
```

Chạy với profile dev:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Cách 3: Chỉnh sửa application.yml trực tiếp (⚠️ KHÔNG dùng cho Production)

Edit `src/main/resources/application.yml`:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id-here.apps.googleusercontent.com
            client-secret: your-client-secret-here
```

## Bước 4: Test Google Login

1. Khởi động ứng dụng:
   ```bash
   mvn spring-boot:run
   ```

2. Mở browser: `http://localhost:8080/api/login`

3. Click nút "Google" để test login

## Cấu hình cho Production

Khi triển khai lên production:

1. **Tạo lại credentials trong Google Cloud Console:**
   - Thêm production domain vào `Authorized JavaScript origins`
   - Thêm production redirect URI vào `Authorized redirect URIs`

2. **Set environment variables trên production server:**
   ```bash
   export GOOGLE_CLIENT_ID="your-production-client-id"
   export GOOGLE_CLIENT_SECRET="your-production-client-secret"
   ```

3. **Hoặc cấu hình trong application-prod.yml:**
   ```yaml
   spring:
     profiles:
       active: prod
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID}
               client-secret: ${GOOGLE_CLIENT_SECRET}
   ```

4. **Chạy với production profile:**
   ```bash
   java -jar target/metric-system-1.0.0.jar --spring.profiles.active=prod
   ```

## Troubleshooting

### Lỗi: "redirect_uri_mismatch"
- **Nguyên nhân**: Redirect URI trong code không match với Google Cloud Console
- **Giải pháp**: Kiểm tra lại `Authorized redirect URIs` trong Google Cloud Console

### Lỗi: "invalid_client"
- **Nguyên nhân**: Client ID hoặc Client Secret sai
- **Giải pháp**: Copy lại từ Google Cloud Console, đảm bảo không có space

### Login button không hoạt động
- **Nguyên nhân**: Client ID chưa được cấu hình (còn là placeholder)
- **Giải pháp**: Follow các step trên để set up credentials

## Cấu hình OAuth2 hiện tại

Ứng dụng đã được cấu hình hỗ trợ:
- **Scope**: `openid,profile,email`
- **Redirect URI format**: `{baseUrl}/login/oauth2/code/{registrationId}`
- **Session Management**: `IF_REQUIRED` (Chi khi cần)

## API Endpoints

Sau khi đăng nhập thành công:
- Login endpoint: `POST /api/auth/login` (form-based)
- OAuth2 callback: `/login/oauth2/code/google` (tự động xử lý)
- Logout: `POST /api/auth/logout`
