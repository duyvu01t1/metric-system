@echo off
REM Metric System - Database Setup Script
REM This script initializes the PostgreSQL database using Flyway

echo ========================================
echo Metric System - Database Setup
echo ========================================
echo.

REM Check if PostgreSQL is running
echo Checking PostgreSQL connection...
psql -U postgres -c "SELECT version();" > nul 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL is not running or not installed
    echo Please ensure PostgreSQL is installed and running
    pause
    exit /b 1
)

echo [OK] PostgreSQL is running

REM Create database if not exists
echo.
echo Creating database 'metric_system' if it doesn't exist...
psql -U postgres -c "CREATE DATABASE metric_system;" 2>nul

if %errorlevel% neq 0 (
    echo [WARNING] Database might already exist or error occurred
) else (
    echo [OK] Database created successfully
)

REM Run Maven to apply Flyway migrations
echo.
echo Running Maven to apply Flyway migrations...
echo ========================================

cd /d "%~dp0"

if not exist pom.xml (
    echo [ERROR] pom.xml not found. Please run this script from the project root directory
    pause
    exit /b 1
)

echo Building project and running migrations...
call mvn clean install -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed. Please check the error messages above
    pause
    exit /b 1
)

echo.
echo ========================================
echo [SUCCESS] Database setup completed!
echo ========================================
echo.
echo To start the application, run:
echo   mvn spring-boot:run
echo.
echo Or access:
echo   - Application: http://localhost:8080/api
echo   - Swagger UI: http://localhost:8080/api/swagger-ui.html
echo.
pause
